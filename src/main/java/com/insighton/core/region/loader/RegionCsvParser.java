package com.insighton.core.region.loader;

import com.insighton.core.exception.LocationNotFoundException;
import com.insighton.core.region.dto.RegionGridDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegionCsvParser {

    public RegionGridDto parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] tokens = line.split(",");
        if (tokens.length < 4) {
            return null;
        }

        if (tokens[0].isBlank() || tokens[1].isBlank()) {
            throw new LocationNotFoundException("행정구역이 비어있습니다.");
        }

        String step1 = tokens[0].trim();
        String step2 = tokens[1].trim();

        try {
            int gridX = Integer.parseInt(tokens[2].trim());
            int gridY = Integer.parseInt(tokens[3].trim());
            return new RegionGridDto(step1, step2, gridX, gridY);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
