// Squash & Go: send-notification Edge Function
//
// Internal-only entry point for delivering push notifications to a player.
// Callers are server-side: Postgres triggers (via pg_net) and other Edge
// Functions (confirm-match, dispute-match, etc.). NEVER called from the
// mobile client — there is no JWT auth, only a shared secret.
//
// Pipeline:
//   1. Validate Bearer <INTERNAL_FN_SHARED_SECRET>            (constant-time)
//   2. Parse + validate body                                   (structured codes)
//   3. Look up push_tokens for recipient_player_id             (service role)
//   4. Mint OAuth2 access token from FCM service account       (cached in-process)
//   5. POST one FCM HTTP v1 message per token                  (data-only, HIGH prio)
//   6. Prune tokens that FCM reports as unregistered/invalid   (best-effort)
//
// Errors are returned as a closed enum (`ErrorCode`) — never substring-matched
// by callers. If a new failure mode is needed, add it to the union and to the
// caller mapping intentionally.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { SignJWT, importPKCS8 } from "npm:jose@5.9.6";

// ---------------------------------------------------------------------------
// Public contract — keep in sync with Android NotificationType enum.
// ---------------------------------------------------------------------------

type NotificationType =
  | "CHALLENGE_CREATED"
  | "CHALLENGE_ACCEPTED"
  | "CHALLENGE_DECLINED"
  | "CHALLENGE_CANCELLED"
  | "MATCH_RESULT_SUBMITTED"
  | "MATCH_CONFIRMED"
  | "MATCH_DISPUTED"
  | "MATCH_AUTO_CONFIRMED";

type ErrorCode =
  | "method_not_allowed"
  | "unauthenticated"
  | "invalid_body"
  | "missing_recipient"
  | "missing_type"
  | "fcm_failed"
  | "server_misconfigured";

interface SendNotificationRequest {
  recipient_player_id: string;
  type: NotificationType;
  data?: Record<string, string>;
}

interface SendNotificationResponse {
  success: boolean;
  delivered?: number;     // # of FCM tokens that accepted the message
  pruned?: number;        // # of stale rows deleted from push_tokens
  error?: ErrorCode;      // closed enum — callers match exactly
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const jsonResponse = (body: SendNotificationResponse, status = 200): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

// Constant-time string compare. Important: a regular `===` would short-circuit
// on the first mismatched byte and leak length + prefix via timing.
const timingSafeEqual = (a: string, b: string): boolean => {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
};

// ---------------------------------------------------------------------------
// FCM OAuth: mint a JWT with the service-account private key, exchange it
// for an access token, cache for ~50 minutes (tokens are valid for 1 hour).
// ---------------------------------------------------------------------------

interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
}

let cachedAccessToken: { token: string; expiresAt: number } | null = null;

const getFcmAccessToken = async (sa: ServiceAccount): Promise<string> => {
  const now = Math.floor(Date.now() / 1000);
  if (cachedAccessToken && cachedAccessToken.expiresAt > now + 60) {
    return cachedAccessToken.token;
  }

  const privateKey = await importPKCS8(sa.private_key, "RS256");
  const jwt = await new SignJWT({
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(sa.client_email)
    .setSubject(sa.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(privateKey);

  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  if (!tokenRes.ok) {
    const errText = await tokenRes.text();
    throw new Error(`oauth_exchange_failed status=${tokenRes.status} body=${errText}`);
  }

  const json = await tokenRes.json() as { access_token: string; expires_in: number };
  cachedAccessToken = {
    token: json.access_token,
    expiresAt: now + json.expires_in,
  };
  return json.access_token;
};

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return jsonResponse({ success: false, error: "method_not_allowed" }, 405);
  }

  // --- Step 1: env wiring ---
  const expectedSecret = Deno.env.get("INTERNAL_FN_SHARED_SECRET");
  const supabaseUrl    = Deno.env.get("SUPABASE_URL");
  const serviceKey     = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const fcmProjectId   = Deno.env.get("FCM_PROJECT_ID");
  const saJson         = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");

  if (!expectedSecret || !supabaseUrl || !serviceKey || !fcmProjectId || !saJson) {
    console.error("missing env vars: one of INTERNAL_FN_SHARED_SECRET, SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, FCM_PROJECT_ID, FCM_SERVICE_ACCOUNT_JSON");
    return jsonResponse({ success: false, error: "server_misconfigured" }, 500);
  }

  // --- Step 2: shared-secret auth ---
  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.startsWith("Bearer ")) {
    return jsonResponse({ success: false, error: "unauthenticated" }, 401);
  }
  const presented = authHeader.substring("Bearer ".length);
  if (!timingSafeEqual(presented, expectedSecret)) {
    return jsonResponse({ success: false, error: "unauthenticated" }, 401);
  }

  // --- Step 3: body parse + validation ---
  let body: SendNotificationRequest;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ success: false, error: "invalid_body" }, 400);
  }
  if (!body.recipient_player_id) {
    return jsonResponse({ success: false, error: "missing_recipient" }, 400);
  }
  if (!body.type) {
    return jsonResponse({ success: false, error: "missing_type" }, 400);
  }

  // --- Step 4: token lookup (service role; bypasses RLS) ---
  const adminClient = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: tokenRows, error: lookupError } = await adminClient
    .from("push_tokens")
    .select("id, token")
    .eq("player_id", body.recipient_player_id);

  if (lookupError) {
    console.error("push_tokens lookup failed", lookupError);
    return jsonResponse({ success: false, error: "fcm_failed" }, 500);
  }

  // No tokens registered for this player — not an error. The player simply
  // hasn't installed the app or hasn't granted notification permission.
  if (!tokenRows || tokenRows.length === 0) {
    return jsonResponse({ success: true, delivered: 0, pruned: 0 });
  }

  // --- Step 5: FCM access token ---
  let serviceAccount: ServiceAccount;
  try {
    serviceAccount = JSON.parse(saJson);
  } catch {
    console.error("FCM_SERVICE_ACCOUNT_JSON is not valid JSON");
    return jsonResponse({ success: false, error: "server_misconfigured" }, 500);
  }

  let accessToken: string;
  try {
    accessToken = await getFcmAccessToken(serviceAccount);
  } catch (e) {
    console.error("oauth token mint failed", e);
    return jsonResponse({ success: false, error: "fcm_failed" }, 500);
  }

  // --- Step 6: send each message in parallel; collect prunable rows ---
  const fcmEndpoint = `https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`;
  const idsToPrune: string[] = [];
  let delivered = 0;

  await Promise.all(tokenRows.map(async (row) => {
    const payload = {
      message: {
        token: row.token,
        // Data-only payload. Title/body are rendered client-side from
        // strings.xml so localization works regardless of server locale.
        data: {
          type: body.type,
          ...(body.data ?? {}),
        },
        android: { priority: "HIGH" },
      },
    };

    const res = await fetch(fcmEndpoint, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      delivered += 1;
      return;
    }

    const errBody = await res.json().catch(() => ({})) as {
      error?: { status?: string; message?: string };
    };
    const fcmStatus = errBody?.error?.status;

    // Tokens FCM tells us are dead. We delete them so we don't keep paying
    // the round-trip cost on every notification.
    //   NOT_FOUND      → uninstalled / data cleared
    //   UNREGISTERED   → legacy spelling of the same thing
    //   INVALID_ARGUMENT on token field → malformed token
    if (fcmStatus === "NOT_FOUND" || fcmStatus === "UNREGISTERED" ||
        (res.status === 400 && fcmStatus === "INVALID_ARGUMENT")) {
      idsToPrune.push(row.id);
    } else {
      // Server-side issue (rate limiting, FCM outage, our auth wrong, etc.).
      // Log and move on — don't prune, the token is probably fine.
      console.error("fcm send failed (non-prune)", {
        token_id: row.id,
        http_status: res.status,
        fcm_status: fcmStatus,
        message: errBody?.error?.message,
      });
    }
  }));

  // --- Step 7: prune stale tokens (best-effort; never blocks the response) ---
  let pruned = 0;
  if (idsToPrune.length > 0) {
    const { error: pruneError, count } = await adminClient
      .from("push_tokens")
      .delete({ count: "exact" })
      .in("id", idsToPrune);
    if (pruneError) {
      console.error("prune failed", pruneError);
    } else {
      pruned = count ?? idsToPrune.length;
    }
  }

  return jsonResponse({ success: true, delivered, pruned });
});
