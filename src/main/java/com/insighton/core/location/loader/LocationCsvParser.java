package com.insighton.core.location.loader;

import com.insighton.core.location.dto.LocationGridDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationCsvParser {

    public LocationGridDto parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] tokens = line.split(",");
        if (tokens.length < 4) {
            return null;
        }

        String step1 = tokens[0].trim();
        String step2 = tokens[1].trim();

        try {
            int gridX = Integer.parseInt(tokens[2].trim());
            int gridY = Integer.parseInt(tokens[3].trim());
            return new LocationGridDto(step1, step2, gridX, gridY);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
