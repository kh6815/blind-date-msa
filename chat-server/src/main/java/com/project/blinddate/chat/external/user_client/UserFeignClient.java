package com.project.blinddate.chat.external.user_client;

import com.project.blinddate.chat.external.user_client.dto.UserFeignResponse;
import com.project.blinddate.common.dto.ResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-server", url = "${external.user-server.url:http://localhost:8081}")
public interface UserFeignClient {

    @GetMapping("/api/v1/users/{userId}")
    ResponseDto<UserFeignResponse> getUserInfo(@PathVariable("userId") Long userId);
}
