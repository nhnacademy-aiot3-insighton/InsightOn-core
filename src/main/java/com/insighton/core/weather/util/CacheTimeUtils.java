package com.insighton.core.weather.util;

import java.time.Duration;
import java.time.OffsetDateTime;

public class CacheTimeUtils {

    public static Duration getDurationUtilNextHour() {
        /**
         * 현재 시각부터 다음 정각(00분 00초)까지 남은 초/분 시간을 계산합니다.
         * 예: 14:15:00 -> 45분(2700초) 남음
         */
        OffsetDateTime now = OffsetDateTime.now();

        OffsetDateTime nextHour = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);

        Duration duration = Duration.between(now, nextHour);

        if (duration.isNegative() || duration.isZero()) {
            return Duration.ofSeconds(1);
        }

        return duration;
    }
}
