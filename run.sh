#!/usr/bin/env bash
# Runs the whole demo: starts the facilitator and the server, shows what is free and what is paid,
# shows the facilitator's customizations, pays both paid endpoints with the Java client, stops.
# Needs ./.env (copy .env.example) and the module jars (mvn -q package -DskipTests).
set -euo pipefail
cd "$(dirname "$0")"

# Load .env line by line (a mnemonic contains spaces, so the file is not sourced).
[ -f .env ] || { echo "Missing .env: cp .env.example .env and fill it in"; exit 1; }
while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in ''|\#*) continue ;; esac
  key="${line%%=*}"; value="${line#*=}"
  value="${value%\"}"; value="${value#\"}"
  export "$key=$value"
done < .env
API_KEY="${FACILITATOR_API_KEY:-demo-secret}"

for jar in facilitator server client; do
  [ -f "$jar/target/$jar-1.0.0-SNAPSHOT.jar" ] || { echo "Build first: mvn -q package -DskipTests"; exit 1; }
done

wait_for() {  # url, name
  for _ in $(seq 1 60); do
    curl -s -o /dev/null "$1" && return 0
    sleep 1
  done
  echo "$2 did not start; see logs/$2.log"; exit 1
}
step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }

mkdir -p logs
java -jar facilitator/target/facilitator-1.0.0-SNAPSHOT.jar > logs/facilitator.log 2>&1 &
FACILITATOR=$!
trap 'kill $FACILITATOR ${SERVER:-} 2>/dev/null || true' EXIT
wait_for http://127.0.0.1:8402/x402/supported facilitator
java -jar server/target/server-1.0.0-SNAPSHOT.jar > logs/server.log 2>&1 &
SERVER=$!
wait_for http://127.0.0.1:8080/api/status server
echo "facilitator http://127.0.0.1:8402/x402   server http://127.0.0.1:8080   logs in ./logs"

step "1. The server: what is free, what is paid (curl, no payment)"
for path in /api/status /api/weather/lisbon /api/reports/ada-usd; do
  curl -s -o /dev/null -w "GET $path -> HTTP %{http_code}\n" "http://127.0.0.1:8080$path"
done
echo "   /api/weather/* is paid through @X402Payment(\"weather\") on WeatherController."
echo "   /api/reports/* is paid through path: in application.yml; ReportController has no x402 code."
echo "   The 402's PAYMENT-REQUIRED header, decoded:"
curl -s -D - -o /dev/null http://127.0.0.1:8080/api/reports/ada-usd \
  | grep -i '^payment-required:' | cut -d' ' -f2 | tr -d '\r' | base64 -d 2>/dev/null \
  | python3 -m json.tool 2>/dev/null | sed 's/^/     /' || true

step "2. The facilitator's customizations (curl)"
# A well-formed verify request paying someone not in demo.allowed-payees.
REQ='{"scheme":"exact","network":"cardano:preprod","asset":"lovelace","amount":"1000000","payTo":"addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket","maxTimeoutSeconds":300,"extra":{}}'
NONCE="$(printf '0%.0s' $(seq 1 64))#0"
BODY="{\"x402Version\":2,\"paymentPayload\":{\"x402Version\":2,\"accepted\":$REQ,\"payload\":{\"transaction\":\"AA==\",\"nonce\":\"$NONCE\"}},\"paymentRequirements\":$REQ}"
curl -s -o /dev/null -w "ApiKeyFilter:  POST /x402/verify without X-Api-Key -> HTTP %{http_code}\n" \
  -X POST -H 'Content-Type: application/json' -d "$BODY" http://127.0.0.1:8402/x402/verify
printf "PayeePolicy:   POST /x402/verify paying an unknown address -> "
curl -s -X POST -H 'Content-Type: application/json' -H "X-Api-Key: $API_KEY" -d "$BODY" \
  http://127.0.0.1:8402/x402/verify
echo

step "3. Paying with the Java client (each payment waits for its block, ~20-90 s)"
java -cp "client/target/client-1.0.0-SNAPSHOT.jar:$(cat client/target/classpath.txt)" demo.client.PayingClient

step "4. What the facilitator saw"
echo "AuditHooks (log):"
grep -E "VERIFIED|SETTLED|REFUSED" logs/facilitator.log | sed -E 's/^.* : /   /' || true
printf "StatsController (GET /stats), filled by the hooks and MeteredChainBackend:\n   "
curl -s http://127.0.0.1:8402/stats
echo
