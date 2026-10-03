#!/bin/bash

# Let's Encrypt 인증서 초기 발급 스크립트
# 참고: https://github.com/wmnnd/nginx-certbot

set -e

# 도메인 설정 (실제 도메인으로 변경 필요)
domains=(user.blind-date.site chat.blind-date.site minio.blind-date.site)
email="test@naver.com" # 실제 이메일로 변경 필요
staging=0 # 테스트용 0, 실제 운영용 1

# Let's Encrypt 설정
rsa_key_size=4096
data_path="./letsencrypt-data"
compose_file="docker-compose.yml"  # 서버에서는 docker-compose.yml 사용

echo "### Let's Encrypt 인증서 초기 발급 시작 ###"

# 기존 인증서 데이터 확인
if [ -d "$data_path" ]; then
  read -p "기존 인증서 데이터를 발견했습니다. 삭제하고 새로 발급하시겠습니까? (y/N) " decision
  if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
    echo "작업을 취소합니다."
    exit 0
  fi
  echo "기존 데이터를 삭제합니다..."
  docker compose -f "$compose_file" run --rm --entrypoint "\
    rm -rf /etc/letsencrypt/live /etc/letsencrypt/archive /etc/letsencrypt/renewal" certbot
fi

echo "### TLS 파라미터 다운로드 중..."
mkdir -p "$data_path/conf"
curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf > "$data_path/conf/options-ssl-nginx.conf"
curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem > "$data_path/conf/ssl-dhparams.pem"
echo

echo "### 임시 인증서 생성 중 (nginx 시작을 위해)..."
path="/etc/letsencrypt/live/${domains[0]}"
docker compose -f "$compose_file" run --rm --entrypoint "\
  sh -c 'mkdir -p $path && \
  openssl req -x509 -nodes -newkey rsa:$rsa_key_size -days 1 \
    -keyout $path/privkey.pem \
    -out $path/fullchain.pem \
    -subj /CN=localhost'" certbot
echo

echo "### nginx 시작 중..."
docker compose -f "$compose_file" up --force-recreate -d nginx
echo

echo "### 임시 인증서 삭제 중..."
docker compose -f "$compose_file" run --rm --entrypoint "\
  rm -rf /etc/letsencrypt/live/${domains[0]} && \
  rm -rf /etc/letsencrypt/archive/${domains[0]} && \
  rm -rf /etc/letsencrypt/renewal/${domains[0]}.conf" certbot
echo

echo "### Let's Encrypt 인증서 발급 요청 중..."

# staging 모드 선택
staging_arg=""
if [ $staging != "0" ]; then staging_arg="--staging"; fi

# 각 도메인에 대해 인증서 발급
for domain in "${domains[@]}"; do
  echo ">>> 도메인: $domain"
  docker compose -f "$compose_file" run --rm --entrypoint "\
    certbot certonly --webroot -w /var/www/certbot \
      $staging_arg \
      --email $email \
      --rsa-key-size $rsa_key_size \
      --agree-tos \
      --no-eff-email \
      --force-renewal \
      -d $domain" certbot
  echo
done

echo "### nginx 재시작 중..."
docker compose -f "$compose_file" restart nginx

echo "### 완료! ###"
echo "인증서가 성공적으로 발급되었습니다."
echo ""
echo "※ 주의사항:"
echo "  - staging 모드로 테스트한 경우, 실제 인증서 발급을 위해 staging=0으로 설정 후 다시 실행하세요."
echo "  - certbot 컨테이너가 12시간마다 자동으로 인증서를 갱신합니다."
echo "  - 인증서 갱신 로그 확인: docker logs certbot"