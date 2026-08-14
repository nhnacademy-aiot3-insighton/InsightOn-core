package com.insighton.core.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.Objects;

public class InstanceIndexEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String hostName = System.getenv("HOSTNAME");
        if (Objects.isNull(hostName)) {
            return;
        }
        int lastDash = hostName.lastIndexOf('-');
        if (lastDash < 0 || lastDash == hostName.length() - 1) {
            return;
        }

        String ordinal = hostName.substring(lastDash + 1);
        if (!ordinal.chars().allMatch(Character::isDigit)) {
            return;
        }

        environment.getPropertySources().addFirst(new MapPropertySource("statefulSetPodOrdinal", Map.of("instance.slot-index", ordinal)));
    }
}
