package com.insighton.core.groupmember.client;

import com.insighton.core.groupmember.dto.response.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("insighton-auth")
public interface AuthClient {

    @GetMapping("/internal/v1/users/{user-id}")
    AuthUserResponse getUserResponse(@PathVariable("user-id") Long userId);
}
