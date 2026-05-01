// Squash & Go: dispute-match Edge Function
// Authenticates the caller via JWT, then invokes the dispute_match Postgres
// RPC with service-role privileges. The RPC enforces all business rules and
// performs the dispute insert + match status transition atomically.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

interface DisputeMatchRequest {
  match_id: string;
  reason: string;
}

interface DisputeMatchResponse {
  success: boolean;
  dispute_id?: string;
  error?: string;
}

const jsonResponse = (body: DisputeMatchResponse, status = 200): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return jsonResponse({ success: false, error: "method_not_allowed" }, 405);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return jsonResponse({ success: false, error: "unauthenticated" }, 401);
  }
  const jwt = authHeader.substring("Bearer ".length);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    console.error("missing supabase env vars");
    return jsonResponse({ success: false, error: "server_misconfigured" }, 500);
  }

  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: `Bearer ${jwt}` } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) {
    return jsonResponse({ success: false, error: "unauthenticated" }, 401);
  }
  const callerId = userData.user.id;

  let body: DisputeMatchRequest;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ success: false, error: "invalid_body" }, 400);
  }

  if (!body.match_id) {
    return jsonResponse({ success: false, error: "missing_match_id" }, 400);
  }
  if (!body.reason?.trim()) {
    return jsonResponse({ success: false, error: "missing_reason" }, 400);
  }

  const adminClient = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await adminClient.rpc("dispute_match", {
    p_match_id: body.match_id,
    p_caller_id: callerId,
    p_reason: body.reason,
  });

  if (error) {
    console.error("dispute_match rpc failed", error);
    const status = error.message?.includes("not_a_participant") ||
                   error.message?.includes("cannot_dispute_own_submission")
      ? 403
      : 400;
    return jsonResponse({ success: false, error: error.message }, status);
  }

  return jsonResponse(data as DisputeMatchResponse);
});
