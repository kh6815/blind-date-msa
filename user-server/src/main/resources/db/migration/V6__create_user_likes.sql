CREATE TABLE user_likes (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    actor_user_id  BIGINT       NOT NULL COMMENT '좋아요를 누른 유저 ID',
    target_user_id BIGINT       NOT NULL COMMENT '좋아요를 받은 유저 ID',
    created_at     DATETIME(6)  NOT NULL COMMENT '좋아요 시각',
    updated_at     DATETIME(6)  NOT NULL COMMENT '수정 시각',
    del_yn         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부 (0: 정상, 1: 삭제)',
    deleted_at     DATETIME(6)  NULL               COMMENT '삭제 시각',

    PRIMARY KEY (id),

    -- 같은 대상에게 중복 좋아요 방지
    CONSTRAINT uk_user_likes_actor_target UNIQUE (actor_user_id, target_user_id),

    -- "나를 좋아요한 사람 목록" 조회용 (target 기준)
    INDEX idx_user_likes_target_user_id (target_user_id),

    -- "내가 좋아요한 사람 목록" 조회용 (actor 기준)
    INDEX idx_user_likes_actor_user_id  (actor_user_id),

    CONSTRAINT fk_user_likes_actor  FOREIGN KEY (actor_user_id)  REFERENCES users (id),
    CONSTRAINT fk_user_likes_target FOREIGN KEY (target_user_id) REFERENCES users (id)
) COMMENT = '유저 좋아요 관계 테이블';
