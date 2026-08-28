package com.insighton.core.adapter.client.internal;

import com.insighton.core.domain.groupmember.dto.response.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "insighton-auth", url = "${service-url.auth}")
public interface AuthClient {

    @GetMapping("/internal/v1/users/{user-id}")
    AuthUserResponse getUserResponse(@PathVariable("user-id") Long userId);

    @GetMapping("/internal/v1/users/invite/{user-email}")
    AuthUserResponse getUserResponseEmail(@PathVariable("user-email") String userEmail);
}
