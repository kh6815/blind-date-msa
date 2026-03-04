package com.project.blinddate.user.controller;

import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Health", description = "User 서버 상태 확인 API")
@RestController
@RequestMapping(ApiPathConst.USER_API_PREFIX)
public class UserHealthController {

    @Operation(summary = "헬스 체크", description = "User 서버의 기본 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<ResponseDto<String>> health() {
        return ResponseEntity.ok(ResponseDto.ok("user-server-ok"));
    }
}


