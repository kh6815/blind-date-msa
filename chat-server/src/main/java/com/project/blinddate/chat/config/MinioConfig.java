package com.project.blinddate.chat.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * MinIO 설정 클래스
 *
 * MinIO 오브젝트 스토리지와의 연결을 설정하는 클래스입니다.
 * MinIO는 AWS S3 호환 스토리지로, 이미지나 파일 등을 저장하는 데 사용됩니다.
 * application.yml 파일에 정의된 설정을 읽어와 MinioClient 빈을 생성합니다.
 */
public class MinioConfig {

    /**
     * MinioClient 빈 등록
     *
     * MinIO 서버와 통신하기 위한 클라이언트 객체를 생성합니다.
     *
     * @param endpoint MinIO 서버 접속 URL (예: http://localhost:9000)
     * @param accessKey MinIO 접속 액세스 키
     * @param secretKey MinIO 접속 시크릿 키
     * @return MinioClient
     */
    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
