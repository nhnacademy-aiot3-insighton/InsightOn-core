package com.insighton.core.domain.region.loader;

import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.registry.InMemoryRegionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegionCsvLoader implements ApplicationRunner {

    private final RegionCsvParser regionCsvParser;
    private final InMemoryRegionRegistry inMemoryRegionRegistry;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("/data/locations.csv");

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;
            int loadedCount = 0;

            while ((line = bufferedReader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                RegionGridDto dto = regionCsvParser.parse(line);
                if (dto == null) {
                    throw new IllegalStateException("잘못된 locations.csv 행: " + line);
                }
                inMemoryRegionRegistry.save(dto);
                loadedCount++;
            }

            log.info("locations.csv 적재 완료 — {}건", loadedCount);
        }
    }
}
