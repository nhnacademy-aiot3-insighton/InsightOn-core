package com.insighton.core.location.registry;

import com.insighton.core.location.dto.LocationGridDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class InMemoryLocationRegistry implements LocationRegistry {

    private final Map<String, List<String>> stateToCitiesMap = new HashMap<>(); // 1:N 구조관리
    private final Map<String, Map<String, LocationGridDto>> gridCoordinateMap = new HashMap<>(); // 한 번에 격자 데이터까지 도달하게 해줌

    @Override
    public void save(LocationGridDto dto) {
        String state = dto.step1();
        String city = dto.step2();

        stateToCitiesMap.computeIfAbsent(state, k -> new ArrayList<>()); // state가 없으면 빈 리스트를 생성
        if (city != null && !city.isBlank() && !stateToCitiesMap.get(state).contains(city)) {
            stateToCitiesMap.get(state).add(city);
        }

        gridCoordinateMap.computeIfAbsent(state, k -> new HashMap<>());
        gridCoordinateMap.get(state).put(city, dto);
    }

    @Override
    public List<String> findAllStates() {
        return new ArrayList<>(stateToCitiesMap.keySet());
    }

    @Override
    public List<String> findCitiesByState(String state) {
        List<String> cities = stateToCitiesMap.get(state);
        if (cities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(cities);
    }

    @Override
    public Optional<LocationGridDto> findGridCoordinate(String state, String city) {
        Map<String, LocationGridDto> cityMap = gridCoordinateMap.get(state);
        if (cityMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cityMap.get(city));
    }

    @Override
    public void clear() {
        stateToCitiesMap.clear();
        gridCoordinateMap.clear();
    }
}
