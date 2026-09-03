package com.insighton.core.domain.weather.util;

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

    public static Duration getDurationUntilNextMidFcst() {
        /**
         * 현재 시각부터 다음 중기예보 발표시각(06시10분 또는 18시10분)까지 남은 시간을 계산합니다.
         * 중기예보는 하루 2회만 갱신되므로 시간 단위 캐시보다 훨씬 긴 TTL을 사용합니다.
         */
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime next6 = now.withHour(6).withMinute(10).withSecond(0).withNano(0);
        OffsetDateTime next18 = now.withHour(18).withMinute(10).withSecond(0).withNano(0);

        OffsetDateTime target;
        if (now.isBefore(next6)) {
            target = next6;
        } else if (now.isBefore(next18)) {
            target = next18;
        } else {
            target = next6.plusDays(1);
        }

        Duration duration = Duration.between(now, target);

        if (duration.isNegative() || duration.isZero()) {
            return Duration.ofSeconds(1);
        }

        return duration;
    }
}
