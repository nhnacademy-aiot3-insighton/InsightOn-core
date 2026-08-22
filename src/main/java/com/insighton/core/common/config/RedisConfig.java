package com.insighton.core.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.insighton.core.adapter.mqtt.listener.dto.TelemetryEventMessage;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${telemetry.redis.host:localhost}")
    private String telemetryRedisHost;

    @Value("${telemetry.redis.port:6379}")
    private int telemetryRedisPort;

    @Value("${telemetry.redis.password:}")
    private String telemetryRedisPassword;

    // 락/캐시용 기본 Redis와 완전히 분리된 별도 인스턴스 — pub/sub 트래픽이
    // 게이트웨이 락 갱신(GatewayMqttLockService)에 영향을 주지 않도록 물리적으로 분리함
    @Bean
    public RedisConnectionFactory telemetryRedisConnectionFactory() {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(telemetryRedisHost, telemetryRedisPort);
        config.setPassword(telemetryRedisPassword);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        //Key 직렬화 설정
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        //ObjectMapper 생성 및 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<WeatherDataDto> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, WeatherDataDto.class);

        //Value 직렬화 설정
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, WeatherDataDto> weatherRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, WeatherDataDto> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<WeatherDataDto> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, WeatherDataDto.class);

        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, TelemetryEventMessage> telemetryRedisTemplate(
            @Qualifier("telemetryRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, TelemetryEventMessage> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<TelemetryEventMessage> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, TelemetryEventMessage.class);
        redisTemplate.setValueSerializer(serializer);
        return redisTemplate;
    }
}
