package com.insighton.core.domain.weather.util;

public class WeatherCodeMapper {

    // 하늘상태 변환
    public static String parseSky(String code) {
        if (code == null) {
            return "알수없음";
        }
        return switch (code) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "알수없음(" + code + ")";
        };
    }

    // 강수형태 변환
    public static String parsePty(String code) {
        if (code == null) {
            return "없음";
        }
        return switch (code) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "없음";
        };
    }

    // 미세먼지 등급 변환
    public static String parseAirGrade(String grade) {
        if (grade == null) {
            return "정보없음";
        }
        return switch (grade) {
            case "1" -> "좋음";
            case "2" -> "보통";
            case "3" -> "나쁨";
            case "4" -> "매우나쁨";
            default -> "정보없음";
        };
    }
}
