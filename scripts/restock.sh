#!/usr/bin/env bash
# Tops every visible (not archived) cookie up to a stock level, through the admin API.
# Usage: scripts/restock.sh staging|production [level, default 1000]
set -euo pipefail

case "${1:-}" in
  staging) api=https://guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie ;;
  production) api=https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie ;;
  *) echo "Usage: $0 staging|production [level]" >&2; exit 1 ;;
esac
level="${2:-1000}"

read -rp "Admin email: " email
read -rsp "Admin password: " password
echo

token=$(EMAIL="$email" PASSWORD="$password" python3 -I -c '
import json, os
print(json.dumps({"email": os.environ["EMAIL"], "password": os.environ["PASSWORD"]}))' |
  curl -sf -m 60 -H 'Content-Type: application/json' --data-binary @- "$api/admin/login" |
  python3 -I -c 'import json, sys; print(json.load(sys.stdin)["token"])') || { echo "Sign-in failed" >&2; exit 1; }

curl -sf -m 60 -H "Authorization: Bearer $token" "$api/api/v1/admin/cookies" |
  LEVEL="$level" python3 -I -c '
import json, os, sys
level = int(os.environ["LEVEL"])
for view in json.load(sys.stdin):
    c = view["cookie"]
    if not c.get("archived") and (c.get("stockQuantity") or 0) < level:
        print(c["cookieId"], c.get("stockQuantity") or 0, c.get("description") or c["cookieId"], sep="\t")' |
  while IFS=$'\t' read -r id current name; do
    body=$(printf '{"setTo":%d,"reason":"RESTOCK","note":"Restocked to %d by scripts/restock.sh"}' "$level" "$level")
    if curl -sf -m 60 -o /dev/null -X PUT -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
        --data "$body" "$api/api/v1/admin/cookies/$id/stock"; then
      echo "$name: $current -> $level"
    else
      echo "$name: failed" >&2
    fi
  done
echo "Done. Cookies already at $level or more were left alone."
