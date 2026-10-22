# EC2 인스턴스에 저장된 배포 스크립트

#!/usr/bin/env bash
set -euo pipefail # 어느 명령이든 실패하면 즉시 멈춤. 이미지 pull 실패시 트래픽 전환 막음.

TAG="${1:?사용법: deploy.sh <이미지 태그>}"

APP_DIR="/home/ec2-user/app"
REGION="ap-northeast-2"
REGISTRY="<계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com"
REPOSITORY="blanken-server"
HEALTH_PATH="/blanken/actuator/health"
COMPOSE_FILE="docker-compose.yaml"

cd "$APP_DIR"

# 1. 현재 색과 배포할 색 결정
if [ -f active_color ] && [ "$(cat active_color)" = "blue" ]; then
  CURRENT="blue";  TARGET="green"; TARGET_PORT=8082
else
  CURRENT="green"; TARGET="blue";  TARGET_PORT=8081
fi
echo "[deploy] $CURRENT → $TARGET (port $TARGET_PORT), tag=$TAG"

# 2. 새 버전 띄우기
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

export APP_IMAGE="$REGISTRY/$REPOSITORY:$TAG"
export HOST_PORT="$TARGET_PORT"

docker compose -p "blanken-$TARGET" -f "$COMPOSE_FILE" pull
docker compose -p "blanken-$TARGET" -f "$COMPOSE_FILE" up -d

# 3. 헬스 체크 (최대 5초 x 30회 = 150초)
# 헬스 체크가 실패하면 트래픽 전환없이 종료. nginx는 여전히 기존 버전을 가리킴
# curl -f: DB나 Redis에 연결하지 못해 실패하면 트래픽을 받지 못함.
for i in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:$TARGET_PORT$HEALTH_PATH" > /dev/null; then
    echo "[deploy] 헬스 체크 통과 (${i}회차)"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "[deploy] 헬스 체크 실패 → 새 버전 내리고 종료, 기존 $CURRENT 유지"
    docker compose -p "blanken-$TARGET" -f "$COMPOSE_FILE" logs --tail 100
    docker compose -p "blanken-$TARGET" -f "$COMPOSE_FILE" down
    exit 1
  fi
  sleep 5
done

# 4. 트래픽 전환
echo "set \$service_url http://127.0.0.1:$TARGET_PORT;" \
  | sudo tee /etc/nginx/conf.d/service-url.inc > /dev/null
sudo nginx -t
sudo systemctl reload nginx

# 지금 어느 색이 더떤 SHA로 서비스 중인지 기록.
echo "$TARGET" > active_color
echo "$TAG"    > active_tag
echo "[deploy] 트래픽 전환 완료 → $TARGET"

# 5. 이전 버전 정리 (진행 중 요청이 빠질 시간을 준 뒤)
sleep 10
docker compose -p "blanken-$CURRENT" -f "$COMPOSE_FILE" down || true
docker image prune -f

echo "[deploy] 완료: $TARGET 서비스 중 (tag=$TAG)"