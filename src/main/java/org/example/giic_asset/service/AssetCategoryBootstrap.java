package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.AssetCategory;
import org.example.giic_asset.repository.AssetCategoryRepository;
import org.example.giic_asset.repository.AssetRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetCategoryBootstrap implements ApplicationRunner {
    private final AssetCategoryRepository categoryRepository;
    private final AssetRepository assetRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Map<String, AssetCategory> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(AssetCategory::getCode, category -> category));
        List<String> catalog = readLines("asset-category-catalog.tsv");
        int order = 0;
        for (String line : catalog.subList(1, catalog.size())) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 3 || fields[0].isBlank() || fields[2].isBlank()) {
                throw new IllegalStateException("Invalid category catalog row: " + line);
            }
            AssetCategory parent = fields[1].isBlank() ? null : categories.get(fields[1]);
            if (!fields[1].isBlank() && parent == null) {
                throw new IllegalStateException("Unknown parent category: " + fields[1]);
            }
            int sortOrder = ++order;
            categories.computeIfAbsent(fields[0], code -> categoryRepository.save(
                    new AssetCategory(code, fields[2], parent, sortOrder)));
        }

        ClassPathResource assignmentResource = new ClassPathResource("asset-category-assignments.tsv");
        if (!assignmentResource.exists()) {
            log.info("Asset categories ready: {} categories; private asset assignments are not packaged",
                    categories.size());
            return;
        }
        Map<String, Asset> assets = assetRepository.findAll().stream()
                .collect(Collectors.toMap(Asset::getAssetNo, asset -> asset));
        List<String> assignments = readLines("asset-category-assignments.tsv");
        Set<String> seen = new HashSet<>();
        int applied = 0;
        int missing = 0;
        for (String line : assignments.subList(1, assignments.size())) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 2 || !seen.add(fields[0])) {
                throw new IllegalStateException("Invalid or duplicated category assignment: " + line);
            }
            AssetCategory category = categories.get(fields[1]);
            if (category == null || category.getParent() == null) {
                throw new IllegalStateException("Unknown subcategory in assignment: " + line);
            }
            Asset asset = assets.get(fields[0]);
            if (asset == null) {
                missing++;
            } else if (asset.getCategory() == null) {
                asset.setCategory(category);
                applied++;
            }
        }
        log.info("Asset categories ready: {} categories, {} classifications applied, {} source assets absent",
                categories.size(), applied, missing);
    }

    private List<String> readLines(String resourceName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(resourceName).getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().filter(line -> !line.isBlank()).toList();
            if (lines.size() < 2) {
                throw new IllegalStateException("Category resource is empty: " + resourceName);
            }
            return lines;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read category resource: " + resourceName, exception);
        }
    }
}
