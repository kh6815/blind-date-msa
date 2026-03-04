package com.project.blinddate.chat.controller;

import com.project.blinddate.common.ApiPathConst;
import com.project.blinddate.common.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat Health", description = "Chat 서버 상태 확인 API")
@RestController
@RequestMapping(ApiPathConst.CHAT_API_PREFIX)
public class ChatHealthController {

    @Operation(summary = "헬스 체크", description = "Chat 서버의 기본 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<ResponseDto<String>> health() {
        return ResponseEntity.ok(ResponseDto.ok("chat-server-ok"));
    }
}


