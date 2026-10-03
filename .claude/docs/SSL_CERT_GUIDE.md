# SSL 인증서 발급 및 자동 갱신 가이드

## 개요

이 프로젝트는 Let's Encrypt를 사용하여 무료 SSL 인증서를 발급하고, certbot 컨테이너를 통해 자동으로 갱신합니다.

## 아키텍처

- **nginx**: HTTPS 요청 처리 및 ACME challenge 응답
- **certbot**: 12시간마다 인증서 갱신 확인 및 자동 갱신
- **Docker volumes**:
  - `letsencrypt-data`: 인증서 파일 저장
  - `certbot-webroot`: ACME challenge 파일 저장

## 초기 인증서 발급

### 1. 준비 사항

서버에서 다음 사항을 확인하세요:

1. 도메인이 서버 IP로 올바르게 설정되어 있는지 확인
   ```bash
   nslookup user.blind-date.site
   nslookup chat.blind-date.site
   nslookup minio.blind-date.site
   ```

2. 80번 포트가 열려있는지 확인 (Let's Encrypt 검증에 필요)
   ```bash
   sudo netstat -tuln | grep :80
   ```

### 2. 방법 A: 스크립트 사용 (권장)

```bash
cd /path/to/blind-date

# 스크립트 실행 권한 부여
chmod +x scripts/init-letsencrypt.sh

# 스크립트 편집 (이메일 및 도메인 설정)
vi scripts/init-letsencrypt.sh
# - email 변경: email="your-email@example.com"
# - staging 설정: staging=1 (테스트), staging=0 (실제 발급)

# 스크립트 실행
./scripts/init-letsencrypt.sh
```

**주의**:
- 먼저 `staging=1`로 테스트한 후, 성공하면 `staging=0`으로 실제 인증서를 발급하세요
- Let's Encrypt는 발급 횟수 제한이 있습니다 (주당 5회)

### 3. 방법 B: 수동 발급

```bash
# 1. 서비스 시작
docker-compose -f docker-compose-prod.yml up -d

# 2. certbot으로 인증서 발급 (각 도메인마다 실행)
docker-compose -f docker-compose-prod.yml run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d user.blind-date.site

docker-compose -f docker-compose-prod.yml run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d chat.blind-date.site

docker-compose -f docker-compose-prod.yml run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d minio.blind-date.site

# 3. nginx 재시작
docker-compose -f docker-compose-prod.yml restart nginx
```

## 인증서 자동 갱신

certbot 컨테이너가 12시간마다 자동으로 인증서 만료 여부를 확인하고 갱신합니다.

### 갱신 로그 확인

```bash
# certbot 컨테이너 로그 확인
docker logs -f certbot

# 갱신 기록 확인
docker exec certbot certbot certificates
```

### 수동 갱신 (필요한 경우)

```bash
# 강제로 갱신 시도
docker-compose -f docker-compose-prod.yml run --rm certbot renew --force-renewal

# nginx 재시작 (갱신 후)
docker-compose -f docker-compose-prod.yml restart nginx
```

## 문제 해결

### 1. 인증서 발급 실패

**증상**: `Failed to verify` 또는 `Connection refused` 에러

**해결**:
```bash
# 1. 도메인 DNS 확인
nslookup user.blind-date.site

# 2. nginx 80번 포트 리스닝 확인
docker logs nginx

# 3. ACME challenge 경로 확인
curl http://user.blind-date.site/.well-known/acme-challenge/test
# 404가 아닌 403 or 500이 나와야 정상 (파일이 없어서 404는 정상)

# 4. 방화벽 확인
sudo ufw status
```

### 2. 인증서가 만료되었을 때

현재 상황처럼 인증서가 이미 만료된 경우:

```bash
# 1. nginx 중지 (인증서 검증 실패 방지)
docker-compose -f docker-compose-prod.yml stop nginx

# 2. 기존 인증서 삭제 (선택)
docker volume rm blind-date_letsencrypt-data

# 3. 인증서 재발급
./scripts/init-letsencrypt.sh

# 또는 수동 발급
docker-compose -f docker-compose-prod.yml up -d nginx
docker-compose -f docker-compose-prod.yml run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  --force-renewal \
  -d user.blind-date.site \
  -d chat.blind-date.site \
  -d minio.blind-date.site

# 4. nginx 재시작
docker-compose -f docker-compose-prod.yml restart nginx
```

### 3. Rate Limit 에러

**증상**: `too many certificates already issued`

**해결**:
- Let's Encrypt는 주당 5개 인증서 발급 제한이 있습니다
- staging 모드로 먼저 테스트: `--staging` 플래그 사용
- 1주일 후 다시 시도하거나, 다른 인증 기관 사용 고려

### 4. certbot 컨테이너가 계속 재시작됨

**증상**: `docker ps`에서 certbot이 Restarting 상태

**해결**:
```bash
# certbot 로그 확인
docker logs certbot

# entrypoint 명령어 확인 (docker-compose-prod.yml)
# 무한 루프가 제대로 동작하는지 확인
```

## 인증서 정보 확인

```bash
# 인증서 만료일 확인
docker exec certbot certbot certificates

# 도메인별 인증서 확인
openssl s_client -connect user.blind-date.site:443 -servername user.blind-date.site < /dev/null 2>/dev/null | openssl x509 -noout -dates
```

## 참고 자료

- [Let's Encrypt 공식 문서](https://letsencrypt.org/docs/)
- [Certbot 공식 문서](https://eff-certbot.readthedocs.io/)
- [nginx-certbot GitHub](https://github.com/wmnnd/nginx-certbot)