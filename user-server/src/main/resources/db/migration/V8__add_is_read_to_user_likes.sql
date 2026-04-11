ALTER TABLE user_likes
    ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '읽음 여부 (0: 미읽음, 1: 읽음)';
