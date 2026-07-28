package com.insighton.core.groupmember.client;

import com.insighton.core.groupmember.dto.response.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("InsightOn-auth")
public interface AuthClient {

    /**
     * @GetMapping("/internal/users/{user-id}") AuthUserResponse getUserName(@PathVariable("user-id") Long userId);
     * 이 구조는 Auth 서버의 API 스펙이 어떻게 되어있는지에 따라 맞는지 틀린지가 갈립니다.
     * <p>
     * CASE A: Auth 서버가 URL 경로로 ID를 받는 경우
     * Auth 서버 controller가 @GetMapping("/api/users/{user-id}") 형태라면 지금 작성하신 코드가 맞습니다!
     * <p>
     * CASE B: Auth 서버가 Query Parameter로 받는 경우
     * 만약 URL 뒤에 ?userId=1 처럼 붙는 형태라면 @RequestParam("userId")을 쓰셔야 합니다.
     * <p>
     * 💡 한 번 더 확인하기!
     * Auth 서버 쪽 Controller가 요청을 어떻게 받도록 선언되어 있는지(@PathVariable, @RequestBody, @RequestParam 중 무엇인지) 꼭 확인해 보세요!
     * <p>
     * 일단 내 맘대로 작성한 거니까 나중에 다시 결정 일단은 구현...하자
     */
    @GetMapping("/internal/users/{user-id}")
    AuthUserResponse getUserResponse(@PathVariable("user-id") Long userId);
}
