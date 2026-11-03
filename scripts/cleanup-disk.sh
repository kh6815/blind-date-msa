#!/bin/bash

################################################################################
# Docker 디스크 공간 정리 스크립트
#
# 개인 프로젝트용으로 디스크 공간이 부족하지 않도록 주기적으로 정리합니다.
#
# 사용법:
#   chmod +x scripts/cleanup-disk.sh
#   ./scripts/cleanup-disk.sh
#
# Cron 설정 (매주 일요일 새벽 2시):
#   0 2 * * 0 /path/to/blind-date/scripts/cleanup-disk.sh >> /var/log/cleanup-disk.log 2>&1
################################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 디스크 사용량 출력
print_disk_usage() {
    log_info "===== 디스크 사용량 ====="
    df -h / | awk 'NR==1 || /\/$/'
    echo ""
}

# Docker 디스크 사용량 출력
print_docker_usage() {
    log_info "===== Docker 디스크 사용량 ====="
    docker system df
    echo ""
}

# 메인 로직
main() {
    log_info "========================================="
    log_info "Docker 디스크 공간 정리 시작"
    log_info "========================================="
    echo ""

    # 정리 전 상태
    log_info "===== 정리 전 상태 ====="
    print_disk_usage
    print_docker_usage

    # 1. 사용하지 않는 Docker 이미지 정리 (dangling images)
    log_info "1. 사용하지 않는 Docker 이미지 정리 중..."
    if docker image prune -f > /dev/null 2>&1; then
        log_success "사용하지 않는 이미지 정리 완료"
    else
        log_warning "이미지 정리 실패 또는 정리할 항목 없음"
    fi
    echo ""

    # 2. 중지된 컨테이너 정리
    log_info "2. 중지된 컨테이너 정리 중..."
    if docker container prune -f > /dev/null 2>&1; then
        log_success "중지된 컨테이너 정리 완료"
    else
        log_warning "컨테이너 정리 실패 또는 정리할 항목 없음"
    fi
    echo ""

    # 3. 사용하지 않는 네트워크 정리
    log_info "3. 사용하지 않는 네트워크 정리 중..."
    if docker network prune -f > /dev/null 2>&1; then
        log_success "네트워크 정리 완료"
    else
        log_warning "네트워크 정리 실패 또는 정리할 항목 없음"
    fi
    echo ""

    # 4. Build cache 정리 (오래된 것만)
    log_info "4. Docker build cache 정리 중..."
    if docker builder prune -f --filter "until=168h" > /dev/null 2>&1; then
        log_success "Build cache 정리 완료 (7일 이상된 캐시)"
    else
        log_warning "Build cache 정리 실패 또는 정리할 항목 없음"
    fi
    echo ""

    # 5. 오래된 로그 파일 압축 (선택사항)
    log_info "5. 오래된 로그 파일 정리 중..."
    LOG_DIR="./logs"
    if [ -d "$LOG_DIR" ]; then
        # 7일 이상된 .gz 파일 삭제
        find "$LOG_DIR" -type f -name "*.gz" -mtime +7 -delete 2>/dev/null || true
        # 3일 이상된 .log 파일 압축
        find "$LOG_DIR" -type f -name "*.log.*" -mtime +3 ! -name "*.gz" -exec gzip {} \; 2>/dev/null || true
        log_success "로그 파일 정리 완료"
    else
        log_warning "로그 디렉토리를 찾을 수 없습니다: $LOG_DIR"
    fi
    echo ""

    # 6. 시스템 전체 정리 (선택사항 - 주의 필요)
    # log_warning "6. 시스템 전체 정리는 생략합니다 (필요시 주석 해제)"
    # log_warning "   docker system prune -a --volumes -f"
    # echo ""

    # 정리 후 상태
    log_info "===== 정리 후 상태 ====="
    print_disk_usage
    print_docker_usage

    log_success "========================================="
    log_success "디스크 공간 정리 완료!"
    log_success "========================================="
}

# 스크립트 실행
main "$@"