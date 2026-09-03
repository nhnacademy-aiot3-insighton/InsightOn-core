package com.insighton.core.domain.weather.parser;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 기상청 중기기온조회(getMidTa) 전용 예보구역코드(regId) 매핑.
 * SidoNameParser가 뽑아낸 시/도 축약명을 기준으로 각 권역의 대표 관측지점 코드로 변환함.
 * data.go.kr 공식 "중기기온예보구역 코드 정보 표" 기준으로 검증됨. 단, 충남은 표에 도 단위
 * 대표 코드가 따로 없어 가장 가까운 도시인 서산(11C20101)을 대표값으로 사용함.
 */
@Component
public class MidTermRegionCodeMapper {

    private static final Map<String, String> REG_ID_BY_SIDO = Map.ofEntries(
            Map.entry("서울", "11B10101"),
            Map.entry("인천", "11B20201"),
            Map.entry("경기", "11B20601"),
            Map.entry("강원", "11D10301"),
            Map.entry("충북", "11C10301"),
            Map.entry("충남", "11C20101"), // 서산 - 도 단위 대표 코드가 없어 근사치로 사용
            Map.entry("대전", "11C20401"),
            Map.entry("세종", "11C20404"),
            Map.entry("전북", "11F10201"),
            Map.entry("광주", "11F20501"),
            Map.entry("대구", "11H10701"),
            Map.entry("경북", "11H10501"),
            Map.entry("부산", "11H20201"),
            Map.entry("울산", "11H20101"),
            Map.entry("경남", "11H20301"),
            Map.entry("제주", "11G00201")
    );

    public String toRegId(String parsedSidoName) {
        return REG_ID_BY_SIDO.getOrDefault(parsedSidoName, REG_ID_BY_SIDO.get("서울"));
    }
}
