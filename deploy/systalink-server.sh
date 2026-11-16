#!/usr/bin/env bash
# Deploys KingaAPI on a Systalink Cloud Server (Ubuntu or Debian). Run it as root on the server:
#
#   curl -fsSL https://raw.githubusercontent.com/Ideborah-tech/kinga-api/main/deploy/systalink-server.sh | bash
#
# The app is then at http://<server IP>. For HTTPS on your own domain, point the domain's DNS A record
# at the server first, then run:
#
#   curl -fsSL https://raw.githubusercontent.com/Ideborah-tech/kinga-api/main/deploy/systalink-server.sh | DOMAIN=kinga.example.com bash
#
# Running it again pulls the latest code from main and redeploys.
set -euo pipefail

REPO=https://github.com/Ideborah-tech/kinga-api.git
DIR=/opt/kinga-api

if ! command -v docker >/dev/null 2>&1 || ! command -v git >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y docker.io git curl
  systemctl enable --now docker
fi

if [ -d "$DIR/.git" ]; then
  git -C "$DIR" pull --ff-only
else
  git clone --depth 1 "$REPO" "$DIR"
fi

docker build -t kinga-api "$DIR"
docker network create kinga >/dev/null 2>&1 || true
docker rm -f kinga-api kinga-caddy >/dev/null 2>&1 || true

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q active; then
  ufw allow 80/tcp
  ufw allow 443/tcp
fi

if [ -n "${DOMAIN:-}" ]; then
  # Caddy gets a free HTTPS certificate for the domain and forwards to the app.
  docker run -d --name kinga-api --restart unless-stopped --network kinga kinga-api
  docker run -d --name kinga-caddy --restart unless-stopped --network kinga -p 80:80 -p 443:443 \
    -v kinga-caddy:/data caddy:2 caddy reverse-proxy --from "$DOMAIN" --to kinga-api:8085
  URL="https://$DOMAIN"
else
  docker run -d --name kinga-api --restart unless-stopped -p 80:8085 kinga-api
  URL="http://$(curl -fsS https://api.ipify.org 2>/dev/null || hostname -I | awk '{print $1}')"
fi

echo "Waiting for KingaAPI to start..."
for _ in $(seq 1 60); do
  if docker exec kinga-api wget -qO- http://localhost:8085/api/providers >/dev/null 2>&1; then
    echo "KingaAPI is running: $URL"
    echo "Judges sign in with 'Try the demo'."
    exit 0
  fi
  sleep 3
done
echo "KingaAPI did not answer after 3 minutes. Logs: docker logs kinga-api" >&2
exit 1
