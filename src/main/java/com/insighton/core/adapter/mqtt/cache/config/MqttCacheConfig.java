package com.insighton.core.adapter.mqtt.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * devEui 3계층 캐싱에 필요한 Caffeine/Redis 빈을 등록하는 설정 클래스.
 * 이 클래스가 만드는 빈들은 {@link SensorLookupCacheService} 전용이며, mqtt 모듈 전역 설정이 아님.
 */
@Configuration
public class MqttCacheConfig {

    /**
     * devEui 조회 결과를 담는 로컬 1차 캐시(Caffeine)를 생성함.
     * 최대 5만 건을 유지하며 마지막 기록 후 10분이 지나면 만료됨.
     *
     * @return devEui 문자열을 키로 하는 로컬 캐시
     */
    @Bean
    public Cache<String, SensorCacheEntry> sensorEuiLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    /**
     * devEui 조회 결과를 담는 2차 캐시(Redis)용 템플릿을 생성함.
     * 키는 문자열로, 값은 JSON으로 직렬화함.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return {@link SensorCacheEntry} 전용 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, SensorCacheEntry> sensorRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, SensorCacheEntry> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(SensorCacheEntry.class));
        return template;
    }
}