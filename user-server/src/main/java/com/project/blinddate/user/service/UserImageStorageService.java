package com.project.blinddate.user.service;

import com.project.blinddate.user.domain.UserImage;
import com.project.blinddate.user.repository.UserImageRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImageStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @Value("${minio.endpoint}")
    private String endpoint;

    private final UserImageRepository userImageRepository;

    @PostConstruct
    public void init() {
        try {
            ensureBucket();
        } catch (Exception e) {
            // 서버 시작 시 Minio 연결 실패는 로그만 남기고 서비스를 시작할 수 있도록 합니다.
            log.error("Failed to initialize Minio bucket", e);
        }
    }

    @Transactional
    public void deleteUserImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        Set<String> targetUrls = new HashSet<>(imageUrls);
        for (String url : imageUrls) {
            // Handle localhost vs minio mismatch
            if (url.contains("localhost:9000")) {
                targetUrls.add(url.replace("localhost:9000", "minio:9000"));
            } else if (url.contains("minio:9000")) {
                targetUrls.add(url.replace("minio:9000", "localhost:9000"));
            }

            // Handle endpoint vs publicEndpoint mismatch if they are different and not covered above
            if (publicEndpoint != null && endpoint != null && !publicEndpoint.equals(endpoint)) {
                 if (url.startsWith(publicEndpoint)) {
                     targetUrls.add(url.replace(publicEndpoint, endpoint));
                 } else if (url.startsWith(endpoint)) {
                     targetUrls.add(url.replace(endpoint, publicEndpoint));
                 }
            }
        }

        // Soft delete from DB
        userImageRepository.softDeleteByImageUrlIn(new ArrayList<>(targetUrls));

        // Delete from Minio (Best effort)
        for (String url : imageUrls) {
            try {
                String objectName = extractObjectName(url);
                if (objectName != null) {
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(objectName)
                                    .build()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to delete image from Minio: {}", url, e);
            }
        }
    }

    private String extractObjectName(String url) {
        // Assume url format: endpoint/bucket/objectName
        // e.g. http://localhost:9000/bucket/profiles/123.jpg
        // or if endpoint doesn't have path: http://minio:9000/bucket/profiles/123.jpg

        // Simple heuristic: find bucket name and take everything after
        int bucketIdx = url.indexOf("/" + bucket + "/");
        if (bucketIdx != -1) {
            return url.substring(bucketIdx + bucket.length() + 2);
        }
        return null;
    }

public String uploadProfileImage(Long userId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
        return null;
    }
    try {
        ensureBucket(); // Re-enabled for robustness against bucket loss

        String ext = getExtension(file.getOriginalFilename());
            String objectName = "profiles/" + userId + "/" + UUID.randomUUID() + (ext != null ? "." + ext : "");

            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(is, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            return publicEndpoint + "/" + bucket + "/" + objectName;
        } catch (Exception e) {
            log.error("Failed to upload profile image", e);
            throw new IllegalStateException("프로필 이미지를 업로드할 수 없습니다.");
        }
    }

    public void saveUserImage(UserImage userImage) {
        userImageRepository.save(userImage);
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

