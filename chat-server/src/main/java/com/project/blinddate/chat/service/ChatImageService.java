package com.project.blinddate.chat.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatImageService {
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @PostConstruct
    public void init() {
        try {
            ensureBucket();
        } catch (Exception e) {
            log.error("Failed to initialize Minio bucket for chat", e);
        }
    }

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        // Ensure bucket exists before upload to prevent 500 errors if init failed or bucket missing
        try {
            ensureBucket();
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists", e);
        }

        String ext = getExtension(file.getOriginalFilename());
        String objectName = "chat/" + UUID.randomUUID() + (ext != null ? "." + ext : "");

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to upload chat image", e);
            throw new IllegalStateException("이미지를 업로드할 수 없습니다.");
        }

        return publicEndpoint + "/" + bucket + "/" + objectName;
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int idx = filename.lastIndexOf('.');
        if (idx == -1) {
            return null;
        }
        return filename.substring(idx + 1);
    }
}
