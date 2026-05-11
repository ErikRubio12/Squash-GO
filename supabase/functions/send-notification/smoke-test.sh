#!/usr/bin/env bash
# Smoke tests for the send-notification Edge Function (local).
#
# Usage:
#   export SECRET=<INTERNAL_FN_SHARED_SECRET from supabase/functions/.env>
#   ./supabase/functions/send-notification/smoke-test.sh [optional-player-uuid]
#
# Requires `supabase functions serve send-notification` running in another shell.

set -u

URL="http://localhost:54321/functions/v1/send-notification"
PLAYER_UUID="${1:-00000000-0000-0000-0000-000000000001}"

if [ -z "${SECRET:-}" ]; then
  echo "error: export SECRET=<your INTERNAL_FN_SHARED_SECRET> first" >&2
  exit 1
fi

hr() { printf '\n----- %s -----\n' "$1"; }

hr "1. wrong secret -> 401 unauthenticated"
curl -s -i -X POST "$URL" \
  -H "Authorization: Bearer wrong" \
  -H "Content-Type: application/json" \
  -d '{"recipient_player_id":"00000000-0000-0000-0000-000000000000","type":"CHALLENGE_CREATED"}'

hr "2. invalid body -> 400 invalid_body"
curl -s -i -X POST "$URL" \
  -H "Authorization: Bearer $SECRET" \
  -H "Content-Type: application/json" \
  -d 'not json'

hr "3. missing recipient -> 400 missing_recipient"
curl -s -i -X POST "$URL" \
  -H "Authorization: Bearer $SECRET" \
  -H "Content-Type: application/json" \
  -d '{"type":"CHALLENGE_CREATED"}'

hr "4. valid player without tokens -> 200 {success:true, delivered:0, pruned:0}"
curl -s -i -X POST "$URL" \
  -H "Authorization: Bearer $SECRET" \
  -H "Content-Type: application/json" \
  -d "{\"recipient_player_id\":\"$PLAYER_UUID\",\"type\":\"CHALLENGE_CREATED\",\"data\":{\"challenge_id\":\"abc\"}}"

echo
