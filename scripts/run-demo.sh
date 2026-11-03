#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -d frontend/node_modules ]]; then
  printf '%s\n' 'Install frontend dependencies first: cd frontend && npm ci'
  exit 1
fi
./mvnw -q spring-boot:run -Dspring-boot.run.profiles=demo &
api_pid=$!
web_pid=
cleanup() {
  kill "$api_pid" 2>/dev/null || true
  if [[ -n "$web_pid" ]]; then kill "$web_pid" 2>/dev/null || true; fi
}
trap cleanup EXIT INT TERM
printf '%s\n' 'Starting local demo. Store: http://127.0.0.1:5173' 'Admin: owner@example.com / DemoCookies123! (local demo only)'
(cd frontend && npm run dev -- --host 127.0.0.1) &
web_pid=$!
wait "$api_pid"
