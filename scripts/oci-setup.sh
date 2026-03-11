#!/bin/bash
# =============================================================
# OCI 서버 초기 셋업 스크립트
# Oracle Linux / Ubuntu에서 실행
# Usage: bash scripts/oci-setup.sh
# =============================================================
set -euo pipefail

echo "=== OCI Server Setup for Ready Japan ==="

# 1. Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    if command -v dnf &> /dev/null; then
        # Oracle Linux
        sudo dnf install -y docker
        sudo systemctl enable --now docker
    elif command -v apt-get &> /dev/null; then
        # Ubuntu
        curl -fsSL https://get.docker.com | sh
        sudo systemctl enable --now docker
    fi
    sudo usermod -aG docker "$USER"
    echo "Docker installed. Please re-login for group changes to take effect."
fi

# 2. Docker Compose 플러그인 확인
if ! docker compose version &> /dev/null; then
    echo "Installing Docker Compose plugin..."
    sudo mkdir -p /usr/local/lib/docker/cli-plugins
    COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep tag_name | cut -d '"' -f 4)
    ARCH=$(uname -m)
    sudo curl -SL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-${ARCH}" \
        -o /usr/local/lib/docker/cli-plugins/docker-compose
    sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

echo "Docker: $(docker --version)"
echo "Compose: $(docker compose version)"

# 3. 프로젝트 디렉토리 생성
PROJECT_DIR="${HOME}/ready-japan"
mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"

# 4. docker-compose 파일 복사 안내
echo ""
echo "=== 다음 파일을 ${PROJECT_DIR}에 복사하세요 ==="
echo "  - docker-compose.yml"
echo "  - docker-compose.prod.yml"
echo "  - .env (환경변수 파일)"
echo ""

# 5. .env 템플릿 생성
if [ ! -f .env ]; then
    cat > .env << 'ENVEOF'
# Database (Supabase)
DATABASE_URL=jdbc:postgresql://your-supabase-url:6543/postgres
DB_USERNAME=postgres.your-tenant-id
DB_PASSWORD=your-password

# Telegram
TELEGRAM_BOT_TOKEN=your-bot-token
TELEGRAM_CHAT_ID=your-chat-id

# LLM
LLM_PROVIDER=gemini
LLM_API_KEY=your-api-key
LLM_MODEL=gemini-2.5-flash

# Reddit
REDDIT_CLIENT_ID=your-client-id
REDDIT_CLIENT_SECRET=your-client-secret

# Qiita
QIITA_ACCESS_TOKEN=your-access-token
ENVEOF
    echo ".env 템플릿이 생성되었습니다. 실제 값으로 수정하세요:"
    echo "  nano ${PROJECT_DIR}/.env"
fi

# 6. iptables 방화벽 설정 (OCI 인스턴스 내부)
echo ""
echo "=== OCI 방화벽 설정 (필요 시 수동 실행) ==="
echo "# API 포트 (8080) 열기:"
echo "sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT"
echo "sudo netfilter-persistent save  # Ubuntu"
echo "sudo firewall-cmd --permanent --add-port=8080/tcp && sudo firewall-cmd --reload  # Oracle Linux"
echo ""
echo "# OCI Console에서 Security List에도 Ingress Rule 추가 필요:"
echo "#   Source: 0.0.0.0/0, Protocol: TCP, Port: 8080"
echo ""

# 7. GHCR 로그인
echo "=== GHCR 로그인 ==="
echo "GitHub Personal Access Token (read:packages 권한)으로 로그인하세요:"
echo "  echo YOUR_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin"
echo ""

echo "=== Setup complete! ==="
echo ""
echo "배포 시작:"
echo "  cd ${PROJECT_DIR}"
echo "  docker compose -f docker-compose.yml -f docker-compose.prod.yml pull"
echo "  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d"
echo ""
echo "로그 확인:"
echo "  docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f"
echo ""
echo "헬스 체크:"
echo "  curl http://localhost:8080/actuator/health"
echo "  curl http://localhost:8081/actuator/health"
