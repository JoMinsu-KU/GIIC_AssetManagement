package org.example.giic_asset.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AssetCategoryDataTest {
    @Test
    void classificationCatalogIsComplete() throws Exception {
        List<String> catalog = readLines("asset-category-catalog.tsv");
        Map<String, String> names = new HashMap<>();
        Map<String, String> parents = new HashMap<>();
        Set<String> majorCodes = new HashSet<>();
        Set<String> minorCodes = new HashSet<>();
        for (String line : catalog.subList(1, catalog.size())) {
            String[] fields = line.split("\t", -1);
            assertThat(fields).hasSize(3);
            assertThat(names.putIfAbsent(fields[0], fields[2])).isNull();
            parents.put(fields[0], fields[1]);
            if (fields[1].isEmpty()) majorCodes.add(fields[0]);
            else minorCodes.add(fields[0]);
        }
        assertThat(majorCodes).hasSize(8);
        assertThat(minorCodes).hasSize(28);
        assertThat(names).containsValue("디스플레이/회의");
        assertThat(names).containsValue("기타 비품");
        for (String code : minorCodes) {
            assertThat(majorCodes).contains(parents.get(code));
        }

    }

    private List<String> readLines(String resourceName) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(resourceName).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().filter(line -> !line.isBlank()).toList();
        }
    }
}
