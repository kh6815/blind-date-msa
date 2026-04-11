package com.project.blinddate.user.controller;

import com.project.blinddate.common.dto.ResponseDto;
import com.project.blinddate.user.dto.UserIdRequest;
import com.project.blinddate.user.dto.UserLikeRequest;
import com.project.blinddate.user.dto.UserLikeResponse;
import com.project.blinddate.user.service.UserLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Like API", description = "좋아요 도메인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/likes")
public class UserLikeController {

    private final UserLikeService userLikeService;

    @Operation(summary = "좋아요")
    @PostMapping
    public ResponseEntity<ResponseDto<Void>> like(
            @RequestBody UserLikeRequest userLikeRequest,
            UserIdRequest userIdRequest
    ) {
        userLikeService.like(userIdRequest.getId(), userLikeRequest.getTargetUserId());
        return ResponseEntity.ok(ResponseDto.ok(null));
    }

    @Operation(summary = "좋아요 취소")
    @DeleteMapping
    public ResponseEntity<ResponseDto<Void>> unlike(
            @RequestBody UserLikeRequest userLikeRequest,
            UserIdRequest userIdRequest
    ) {
        userLikeService.unlike(userIdRequest.getId(), userLikeRequest.getTargetUserId());
        return ResponseEntity.ok(ResponseDto.ok(null));
    }

    @Operation(summary = "좋아요 여부 확인")
    @PostMapping("/check")
    public ResponseEntity<ResponseDto<UserLikeResponse>> checkLike(
            @RequestBody UserLikeRequest userLikeRequest,
            UserIdRequest userIdRequest
    ) {
        boolean liked = userLikeService.isLiked(userIdRequest.getId(), userLikeRequest.getTargetUserId());
        long count = userLikeService.getLikeCount(userLikeRequest.getTargetUserId());
        return ResponseEntity.ok(ResponseDto.ok(UserLikeResponse.builder()
                .isLiked(liked)
                .likeCount(count)
                .build()));
    }

    @Operation(summary = "미읽은 좋아요 알림 여부")
    @GetMapping("/has-unread")
    public ResponseEntity<ResponseDto<Map<String, Boolean>>> hasUnread(UserIdRequest userIdRequest) {
        boolean hasUnread = userLikeService.hasUnreadLikes(userIdRequest.getId());
        return ResponseEntity.ok(ResponseDto.ok(Map.of("has_unread", hasUnread)));
    }

    @Operation(summary = "좋아요 알림 읽음 처리")
    @PatchMapping("/read")
    public ResponseEntity<ResponseDto<Void>> markAsRead(UserIdRequest userIdRequest) {
        userLikeService.markLikesAsRead(userIdRequest.getId());
        return ResponseEntity.ok(ResponseDto.ok(null));
    }
}
