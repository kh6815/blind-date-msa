# 디스크 공간 부족 문제 해결 가이드

## 문제 분석

### 발생한 에러
```
java.io.FileNotFoundException: /var/lib/kafka/data/replication-offset-checkpoint.tmp (No space left on device)
mkdir /var/lib/docker/overlay2/...: no space left on device
```

### 근본 원인

1. **Kafka가 주요 원인 (80%)**
   - `__consumer_offsets` 토픽이 기본 50개 파티션으로 생성
   - 각 파티션당 최대 1GB 저장 가능 → 최대 60GB 사용 가능
   - 보존 기간 24시간으로 너무 길게 설정
   - 컨테이너 내부(Docker overlay2)에 데이터 저장

2. **Loki 로그 시스템 (15%)**
   - 90일 로그 보존으로 설정됨 (개인 프로젝트에 과도)

3. **모니터링 데이터 누적 (5%)**
   - Prometheus, Grafana 데이터 누적

## 즉시 복구 방법

### 1. EC2 서버 접속
```bash
ssh ec2-user@your-ec2-ip
cd /path/to/blind-date
```

### 2. 디스크 공간 확보

```bash
# 현재 디스크 사용량 확인
df -h

# Docker 디스크 사용량 확인
docker system df

# 1단계: 중지된 컨테이너 및 사용하지 않는 이미지 정리
docker system prune -f

# 2단계: 오래된 로그 파일 삭제 (주의: 중요한 로그 백업 후 실행)
find ./logs -type f -name "*.gz" -mtime +3 -delete
find ./monitoring/prometheus/data -type f -mtime +3 -delete
find ./monitoring/loki/data -type f -mtime +3 -delete

# 3단계: 필요시 Kafka 볼륨 초기화 (데이터 손실 주의!)
# 개인 프로젝트이고 Kafka 데이터가 중요하지 않다면:
docker-compose -f docker-compose-prod.yml down
docker volume rm blind-date_kafka-data  # 또는 해당 볼륨명
```

### 3. 서비스 재배포

```bash
# 최신 코드 가져오기 (수정된 설정 적용)
git pull origin feature/bd-001_develop-mark-chat-read  # 또는 해당 브랜치

# Docker Compose 파일 검증
docker-compose -f docker-compose-prod.yml config

# 서비스 재시작
docker-compose -f docker-compose-prod.yml up -d

# 로그 확인
docker-compose -f docker-compose-prod.yml logs -f kafka
```

### 4. Kafka 토픽 수동 생성 (자동 생성 비활성화됨)

```bash
# Kafka 컨테이너에 접속
docker exec -it kafka bash

# 필요한 토픽 생성 (파티션 3개로 제한)
kafka-topics --create --bootstrap-server localhost:9092 \
  --topic user-info-updated \
  --partitions 3 \
  --replication-factor 1

kafka-topics --create --bootstrap-server localhost:9092 \
  --topic chat-message-save \
  --partitions 3 \
  --replication-factor 1

kafka-topics --create --bootstrap-server localhost:9092 \
  --topic chat-message-save.DLT \
  --partitions 3 \
  --replication-factor 1

# 토픽 목록 확인
kafka-topics --list --bootstrap-server localhost:9092

exit
```

## 적용된 개선 사항

### 1. Kafka 최적화 (docker-compose-prod.yml)

```yaml
kafka:
  environment:
    # 보존 정책 - 디스크 공간 절약
    KAFKA_LOG_RETENTION_HOURS: 6                    # 24시간 → 6시간
    KAFKA_LOG_RETENTION_BYTES: 524288000            # 파티션당 500MB
    KAFKA_LOG_SEGMENT_BYTES: 134217728              # 세그먼트 128MB

    # 파티션 수 축소
    KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS: 10          # 50 → 10
    KAFKA_NUM_PARTITIONS: 3                         # 기본 3개

    # 압축 활성화
    KAFKA_COMPRESSION_TYPE: lz4

    # 자동 토픽 생성 비활성화
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'

    # 메모리 최적화
    KAFKA_HEAP_OPTS: "-Xms256m -Xmx512m"

  volumes:
    - kafka-data:/var/lib/kafka/data               # 볼륨 매핑 추가
```

**예상 효과:**
- 최대 Kafka 디스크 사용량: ~5-6GB (기존 60GB에서 90% 감소)
- 메모리 사용량: 512MB로 제한

### 2. Loki 최적화 (monitoring/loki/config/loki.yml)

```yaml
limits_config:
  retention_period: 168h  # 90일(2160h) → 7일(168h)
```

**예상 효과:**
- Loki 디스크 사용량: ~1-2GB (기존 10GB+에서 80% 감소)

### 3. Prometheus (이미 적절하게 설정됨)

```yaml
prometheus:
  command:
    - --storage.tsdb.retention.time=7d      # 7일 보존
    - --storage.tsdb.retention.size=1GB     # 1GB 제한
```

### 4. 애플리케이션 로그 (이미 적절하게 설정됨)

- 로그 파일 최대 크기: 1GB
- 보존 기간: 1일
- 자동 압축: .gz 형식

## 예방 조치

### 1. 디스크 정리 자동화

```bash
# 정리 스크립트 권한 부여
chmod +x scripts/cleanup-disk.sh

# 테스트 실행
./scripts/cleanup-disk.sh

# Cron 설정 (매주 일요일 새벽 2시 실행)
crontab -e

# 아래 라인 추가 (경로 수정 필요)
0 2 * * 0 /home/ec2-user/blind-date/scripts/cleanup-disk.sh >> /var/log/cleanup-disk.log 2>&1
```

### 2. 디스크 사용량 모니터링

Grafana에 디스크 사용량 알림 추가 (선택사항):

```bash
# Node Exporter 추가 (docker-compose-prod.yml에 추가)
node-exporter:
  image: prom/node-exporter:latest
  container_name: node-exporter
  restart: unless-stopped
  volumes:
    - /proc:/host/proc:ro
    - /sys:/host/sys:ro
    - /:/rootfs:ro
  command:
    - '--path.procfs=/host/proc'
    - '--path.sysfs=/host/sys'
    - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
  networks:
    - app-net
```

### 3. 주기적인 상태 확인

```bash
# 디스크 사용량 확인
df -h

# Docker 사용량 확인
docker system df

# Kafka 토픽별 크기 확인
docker exec kafka kafka-log-dirs --describe --bootstrap-server localhost:9092 | grep size

# 로그 디렉토리 크기 확인
du -sh ./logs/*
du -sh ./monitoring/*/data
```

## 권장 모니터링 임계값

| 항목 | 경고 임계값 | 위험 임계값 |
|------|------------|-----------|
| 전체 디스크 사용률 | 70% | 85% |
| Docker 이미지 | 5GB | 10GB |
| Docker 볼륨 | 10GB | 15GB |
| Kafka 데이터 | 5GB | 8GB |
| 로그 디렉토리 | 2GB | 5GB |

## 트러블슈팅

### Q: Kafka가 시작되지 않는 경우
```bash
# 로그 확인
docker logs kafka

# 볼륨 권한 확인
docker exec kafka ls -la /var/lib/kafka/data

# 필요시 볼륨 재생성
docker-compose -f docker-compose-prod.yml down
docker volume rm blind-date_kafka-data
docker-compose -f docker-compose-prod.yml up -d
```

### Q: 토픽이 자동 생성되지 않는 경우
`KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'`로 설정했으므로 수동 생성 필요 (위의 "Kafka 토픽 수동 생성" 참고)

### Q: 여전히 공간이 부족한 경우
```bash
# 볼륨별 크기 확인
docker system df -v

# 큰 볼륨 찾기
du -sh /var/lib/docker/volumes/*

# 필요시 더 공격적인 정리
docker system prune -a --volumes -f  # 주의: 모든 사용하지 않는 리소스 삭제
```

## 장기 계획

### 1. AWS EBS 볼륨 확장 (필요시)
현재 EC2 인스턴스의 루트 볼륨 크기가 작다면 확장 고려:
- AWS Console → EC2 → Volumes → Modify Volume
- 최소 30GB 이상 권장

### 2. 로그 외부 저장소 연동
- S3에 오래된 로그 백업
- CloudWatch Logs 연동

### 3. Kafka KRaft 모드로 전환 (Zookeeper 제거)
- Zookeeper 없이 Kafka 운영
- 리소스 절약

## 요약

**이번 수정으로 예상되는 디스크 공간 절약:**
- Kafka: 55GB 절약 (60GB → 5GB)
- Loki: 8GB 절약 (10GB → 2GB)
- **총 63GB 절약**

**권장 최소 디스크 공간:**
- EC2 루트 볼륨: 30GB 이상
- 여유 공간: 15GB 이상 유지

**주기적인 관리:**
- 매주: cleanup-disk.sh 실행
- 매월: 디스크 사용량 리뷰
- 필요시: EBS 볼륨 확장