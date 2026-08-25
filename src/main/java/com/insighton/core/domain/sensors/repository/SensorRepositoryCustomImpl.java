package com.insighton.core.domain.sensors.repository;

import static com.insighton.core.domain.sensors.entity.QSensor.sensor;

import com.insighton.core.domain.sensors.entity.Sensor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SensorRepositoryCustomImpl implements SensorRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Sensor> search(Long groupId, Long id, String eui, Long locationId, String sensorName) {
        return queryFactory
                .selectFrom(sensor)
                .where(
                        sensor.group.groupId.eq(groupId), // 항상 그룹 스코프로 제한
                        idEq(id),
                        euiEq(eui),
                        locationIdEq(locationId),
                        sensorNameEq(sensorName)
                )
                .fetch();
    }

    private BooleanExpression idEq(Long id) {
        return id != null ? sensor.sensorId.eq(id) : null;
    }

    private BooleanExpression euiEq(String eui) {
        return eui != null && !eui.trim().isEmpty() ? sensor.sensorEui.eq(eui) : null;
    }

    private BooleanExpression locationIdEq(Long locationId) {
        return locationId != null ? sensor.location.locationId.eq(locationId) : null;
    }

    private BooleanExpression sensorNameEq(String sensorName) {
        return sensorName != null && !sensorName.trim().isEmpty() ? sensor.sensorName.eq(sensorName) : null;
    }
}
