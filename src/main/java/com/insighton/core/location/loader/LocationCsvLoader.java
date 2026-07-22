package com.insighton.core.location.loader;

import com.insighton.core.location.dto.LocationGridDto;
import com.insighton.core.location.registry.InMemoryLocationRegistry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationCsvLoader implements ApplicationRunner {

    private final LocationCsvParser locationCsvParser;
    private final InMemoryLocationRegistry inMemoryLocationRegistry;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("locations.csv");

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;

            while ((line = bufferedReader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                LocationGridDto dto = locationCsvParser.parse(line);
                if (dto == null) {
                    throw new IllegalStateException("잘못된 locations.csv 행: " + line);
                }
                inMemoryLocationRegistry.save(dto);
            }
        }
    }
}
