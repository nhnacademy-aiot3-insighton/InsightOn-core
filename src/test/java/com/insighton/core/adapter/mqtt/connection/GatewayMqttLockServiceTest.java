package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class GatewayMqttLockServiceTest {

    private static final Long GATEWAY_ID = 100L;
    private static final String OWNER = "core-0";
    private static final String OTHER_OWNER = "core-1";
    private static final String LOCK_KEY = "gateway-mqtt-lock100";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private GatewayMqttLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new GatewayMqttLockService(redisTemplate);
    }

    @Test
    void tryAcquire_아무도_없으면_성공한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(LOCK_KEY), eq(OWNER), any(Duration.class))).willReturn(true);

        assertThat(lockService.tryAcquire(GATEWAY_ID, OWNER)).isTrue();
    }

    @Test
    void tryAcquire_이미_다른_인스턴스가_쥐고_있으면_실패한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(LOCK_KEY), eq(OWNER), any(Duration.class))).willReturn(false);

        assertThat(lockService.tryAcquire(GATEWAY_ID, OWNER)).isFalse();
    }

    @Test
    void renew_소유자가_나면_TTL을_연장하고_true를_반환한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(LOCK_KEY)).willReturn(OWNER);

        boolean renewed = lockService.renew(GATEWAY_ID, OWNER);

        assertThat(renewed).isTrue();
        verify(redisTemplate).expire(eq(LOCK_KEY), any(Duration.class));
    }

    @Test
    void renew_소유자가_다르면_연장하지_않고_false를_반환한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(LOCK_KEY)).willReturn(OTHER_OWNER);

        boolean renewed = lockService.renew(GATEWAY_ID, OWNER);

        assertThat(renewed).isFalse();
        verify(redisTemplate, never()).expire(eq(LOCK_KEY), any(Duration.class));
    }

    @Test
    void renew_키가_이미_만료됐으면_소유자_불일치로_취급해_false를_반환한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(LOCK_KEY)).willReturn(null);

        assertThat(lockService.renew(GATEWAY_ID, OWNER)).isFalse();
    }

    @Test
    void release_소유자가_나면_키를_삭제한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(LOCK_KEY)).willReturn(OWNER);

        lockService.release(GATEWAY_ID, OWNER);

        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    void release_소유자가_다르면_삭제하지_않는다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(LOCK_KEY)).willReturn(OTHER_OWNER);

        lockService.release(GATEWAY_ID, OWNER);

        verify(redisTemplate, never()).delete(LOCK_KEY);
    }

    @Test
    void markAlive_생존_신호를_TTL과_함께_기록한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        lockService.markAlive(OWNER);

        verify(valueOperations).set(eq("instance-alive:" + OWNER), eq("1"), any(Duration.class));
    }

    @Test
    void isAlive_최근_생존_신호가_있으면_true를_반환한다() {
        given(redisTemplate.hasKey("instance-alive:" + OWNER)).willReturn(true);

        assertThat(lockService.isAlive(OWNER)).isTrue();
    }

    @Test
    void isAlive_생존_신호가_없으면_false를_반환한다() {
        given(redisTemplate.hasKey("instance-alive:" + OWNER)).willReturn(false);

        assertThat(lockService.isAlive(OWNER)).isFalse();
    }
}
