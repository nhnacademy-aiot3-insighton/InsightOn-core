package com.insighton.core.weather.parser;

import org.springframework.stereotype.Component;

@Component
public class SidoNameParser {

    public String parse(String sidoName) {
        if (sidoName == null || sidoName.isBlank()) {
            return "서울";
        }

        return switch (sidoName) {
            case "서울특별시" -> "서울";
            case "전남광주통합특별시" -> "전남광주";
            case "부산광역시" -> "부산";
            case "대구광역시" -> "대구";
            case "인천광역시" -> "인천";
            case "대전광역시" -> "대전";
            case "울산광역시" -> "울산";
            case "세종특별자치시" -> "세종";
            case "경기도" -> "경기";
            case "강원특별자치도" -> "강원";
            case "충청북도" -> "충북";
            case "충청남도" -> "충남";
            case "전라북도", "전북특별자치도" -> "전북";
            case "경상북도" -> "경북";
            case "경상남도" -> "경남";
            case "제주특별자치도", "제주도" -> "제주";

            default -> sidoName;
        };
    }
}
