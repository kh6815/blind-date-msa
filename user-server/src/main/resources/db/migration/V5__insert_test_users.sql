DELIMITER $$

DROP PROCEDURE IF EXISTS InsertTestUsers$$

CREATE PROCEDURE InsertTestUsers()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE first_names VARCHAR(1000) DEFAULT '민준,서준,도윤,예준,시우,하준,지호,주원,지우,준우,서연,서윤,지우,서현,하은,지안,수아,하율,지유,서우';
    DECLARE last_names VARCHAR(1000) DEFAULT '김,이,박,최,정,강,조,윤,장,임,한,오,서,신,권,황,안,송,류,전';
    DECLARE mbtis VARCHAR(1000) DEFAULT 'ISTJ,ISFJ,INFJ,INTJ,ISTP,ISFP,INFP,INTP,ESTP,ESFP,ENFP,ENTP,ESTJ,ESFJ,ENFJ,ENTJ';
    DECLARE jobs VARCHAR(1000) DEFAULT '개발자,디자이너,기획자,마케터,의사,변호사,학생,공무원,선생님,프리랜서,사업가,트레이너';
    DECLARE locations VARCHAR(1000) DEFAULT '서울 강남구,서울 서초구,서울 마포구,서울 송파구,경기 성남시,경기 수원시,부산 해운대구,인천 연수구,대구 수성구';
    DECLARE interests_list VARCHAR(1000) DEFAULT '운동,여행,음악,영화,독서,게임,요리,맛집,사진,드라이브,캠핑,카페';
    
    DECLARE v_firstName VARCHAR(50);
    DECLARE v_lastName VARCHAR(50);
    DECLARE v_nickname VARCHAR(100);
    DECLARE v_gender VARCHAR(10);
    DECLARE v_mbti VARCHAR(10);
    DECLARE v_job VARCHAR(50);
    DECLARE v_location VARCHAR(50);
    DECLARE v_interest VARCHAR(100);
    DECLARE v_genderPath VARCHAR(10);
    DECLARE v_imgId INT;
    DECLARE v_profileImage VARCHAR(255);
    DECLARE v_lat DOUBLE;
    DECLARE v_lon DOUBLE;
    DECLARE v_email VARCHAR(100);
    
    -- Check if we already have enough users to avoid over-populating on repeated runs if data persists
    -- But since this is a migration, it runs once. 
    -- However, let's just insert 100 users.
    
    WHILE i <= 100 DO
        -- Pick random values
        SET v_firstName = SUBSTRING_INDEX(SUBSTRING_INDEX(first_names, ',', FLOOR(1 + RAND() * 20)), ',', -1);
        SET v_lastName = SUBSTRING_INDEX(SUBSTRING_INDEX(last_names, ',', FLOOR(1 + RAND() * 20)), ',', -1);
        SET v_nickname = CONCAT(v_lastName, v_firstName);
        
        IF RAND() > 0.5 THEN
            SET v_gender = 'MALE';
        ELSE
            SET v_gender = 'FEMALE';
        END IF;
        
        SET v_mbti = SUBSTRING_INDEX(SUBSTRING_INDEX(mbtis, ',', FLOOR(1 + RAND() * 16)), ',', -1);
        SET v_job = SUBSTRING_INDEX(SUBSTRING_INDEX(jobs, ',', FLOOR(1 + RAND() * 12)), ',', -1);
        SET v_location = SUBSTRING_INDEX(SUBSTRING_INDEX(locations, ',', FLOOR(1 + RAND() * 9)), ',', -1);
        
        SET v_interest = CONCAT(
            SUBSTRING_INDEX(SUBSTRING_INDEX(interests_list, ',', FLOOR(1 + RAND() * 12)), ',', -1),
            ',',
            SUBSTRING_INDEX(SUBSTRING_INDEX(interests_list, ',', FLOOR(1 + RAND() * 12)), ',', -1)
        );
        
        -- Profile Image
        IF v_gender = 'FEMALE' THEN
            SET v_genderPath = 'women';
        ELSE
            SET v_genderPath = 'men';
        END IF;
        
        SET v_imgId = FLOOR(RAND() * 99);
        SET v_profileImage = CONCAT('https://randomuser.me/api/portraits/', v_genderPath, '/', v_imgId, '.jpg');
        
        -- Coordinates: Seoul Center 37.5665, 126.9780 +/- 0.1 approx
        SET v_lat = 37.5665 + (RAND() - 0.5) * 0.2;
        SET v_lon = 126.9780 + (RAND() - 0.5) * 0.2;
        
        SET v_email = CONCAT('test_user_', i, '@example.com');
        
        INSERT INTO users (
            email, password_hash, nickname, gender, birth_date, 
            mbti, interests, job, description, location, 
            latitude, longitude, profile_image_url
        )
        VALUES (
            v_email,
            'password',
            v_nickname,
            v_gender,
            DATE_SUB(CURDATE(), INTERVAL FLOOR(20 + RAND() * 15) YEAR),
            v_mbti,
            v_interest,
            v_job,
            CONCAT('안녕하세요! ', v_nickname, '입니다. 반가워요.'),
            v_location,
            v_lat,
            v_lon,
            v_profileImage
        ) ON DUPLICATE KEY UPDATE 
            nickname = VALUES(nickname),
            location = VALUES(location),
            latitude = VALUES(latitude),
            longitude = VALUES(longitude);
            
        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL InsertTestUsers();

DROP PROCEDURE InsertTestUsers;
