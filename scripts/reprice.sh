#!/usr/bin/env bash
# Sets every visible (not archived) cookie to the standard packs, through the admin API:
# Single R5, Box of 6 R27 (save 10%), Box of 12 R48 (save 20%), Box of 24 R84 (save 30%).
# Adds a missing box, leaves other pack sizes alone. Same prices as DemoData.
# Usage: scripts/reprice.sh staging|production|local
set -euo pipefail

case "${1:-}" in
  staging) api=https://guiltfreecookie-api-staging.azurewebsites.net/guiltFreeCookie ;;
  production) api=https://guiltfreecookie-api.azurewebsites.net/guiltFreeCookie ;;
  local) api=http://127.0.0.1:8080/guiltFreeCookie ;;
  *) echo "Usage: $0 staging|production|local" >&2; exit 1 ;;
esac

read -rp "Admin email: " email
read -rsp "Admin password: " password
echo

token=$(EMAIL="$email" PASSWORD="$password" python3 -I -c '
import json, os
print(json.dumps({"email": os.environ["EMAIL"], "password": os.environ["PASSWORD"]}))' |
  curl -sf -m 60 -H 'Content-Type: application/json' --data-binary @- "$api/admin/login" |
  python3 -I -c 'import json, sys; print(json.load(sys.stdin)["token"])') || { echo "Sign-in failed" >&2; exit 1; }

curl -sf -m 60 -H "Authorization: Bearer $token" "$api/api/v1/admin/cookies" |
  python3 -I -c '
import json, sys
for view in json.load(sys.stdin):
    c = view["cookie"]
    if not c.get("archived"):
        print(c["cookieId"], c.get("description") or c["cookieId"], sep="\t")' |
  while IFS=$'\t' read -r id name; do
    variants=$(curl -sf -m 60 -H "Authorization: Bearer $token" "$api/api/v1/admin/cookies/$id/variants")
    # One line per change: METHOD, path suffix, JSON body.
    VARIANTS="$variants" python3 -I -c '
import json, os
packs = [(1, "Single", "5.00", 0), (6, "Box of 6", "27.00", 1), (12, "Box of 12", "48.00", 2), (24, "Box of 24", "84.00", 3)]
existing = json.loads(os.environ["VARIANTS"])
for units, label, price, order in packs:
    match = next((v for v in existing if v["unitsPerPack"] == units and v.get("active")), None) \
        or next((v for v in existing if v["unitsPerPack"] == units), None)
    body = {"label": label if match is None else match["label"], "unitsPerPack": units, "price": price,
            "active": True, "sortOrder": order}
    if match is None:
        print("POST", "-", json.dumps(body), sep="\t")
    else:
        print("PUT", "/" + match["variantId"], json.dumps(body), sep="\t")' |
      while IFS=$'\t' read -r method suffix body; do
        if curl -sf -m 60 -o /dev/null -X "$method" -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
            --data "$body" "$api/api/v1/admin/cookies/$id/variants${suffix#-}"; then
          echo "$name: $(python3 -I -c 'import json,sys; b=json.loads(sys.argv[1]); print(b["label"], "R" + b["price"])' "$body")"
        else
          echo "$name: $method $body failed" >&2
        fi
      done
  done
echo "Done."
