CREATE TABLE user_views (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    viewer_user_id  BIGINT       NOT NULL COMMENT '조회한 유저 ID (본인 프로필 조회시에는 null 가능)',
    viewed_user_id  BIGINT       NOT NULL COMMENT '조회당한 유저 ID (프로필 주인)',
    created_at      DATETIME(6)  NOT NULL COMMENT '조회 시각',
    updated_at      DATETIME(6)  NOT NULL COMMENT '수정 시각',
    del_yn          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부 (0: 정상, 1: 삭제)',
    deleted_at      DATETIME(6)  NULL               COMMENT '삭제 시각',

    PRIMARY KEY (id),

    -- 같은 대상을 같은 사람이 여러 번 조회해도 1회로 카운트
    CONSTRAINT uk_user_views_viewer_viewed UNIQUE (viewer_user_id, viewed_user_id),

    -- "나의 프로필을 조회한 사람 수" 조회용 (viewed 기준)
    INDEX idx_user_views_viewed_user_id (viewed_user_id),

    -- "내가 조회한 프로필 목록" 조회용 (viewer 기준)
    INDEX idx_user_views_viewer_user_id (viewer_user_id),

    CONSTRAINT fk_user_views_viewer FOREIGN KEY (viewer_user_id) REFERENCES users (id),
    CONSTRAINT fk_user_views_viewed FOREIGN KEY (viewed_user_id) REFERENCES users (id)
) COMMENT = '유저 프로필 조회 기록 테이블';