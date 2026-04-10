-- users 테이블에 BaseEntity 컬럼 추가
ALTER TABLE users
    ADD COLUMN created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    ADD COLUMN updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    ADD COLUMN del_yn      TINYINT(1)  NOT NULL DEFAULT 0                    COMMENT '삭제 여부 (0: 정상, 1: 삭제)',
    ADD COLUMN deleted_at  DATETIME(6) NULL                                  COMMENT '삭제 시각';

-- user_images 테이블에 BaseEntity 컬럼 추가
ALTER TABLE user_images
    ADD COLUMN created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    ADD COLUMN updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    ADD COLUMN del_yn      TINYINT(1)  NOT NULL DEFAULT 0                    COMMENT '삭제 여부 (0: 정상, 1: 삭제)',
    ADD COLUMN deleted_at  DATETIME(6) NULL                                  COMMENT '삭제 시각';
