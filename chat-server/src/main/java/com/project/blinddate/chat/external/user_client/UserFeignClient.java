package com.project.blinddate.chat.external.user_client;

import com.project.blinddate.chat.external.user_client.dto.UserFeignResponse;
import com.project.blinddate.common.dto.ResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-server", url = "${external.user-server.url}")
public interface UserFeignClient {

    @PostMapping("/api/v1/users/token/validate")
    ResponseDto<Long> validateToken(@RequestHeader("Authorization") String authorization);

    @GetMapping("/api/v1/users/{userId}")
    ResponseDto<UserFeignResponse> getUserInfo(@PathVariable("userId") Long userId);
}
