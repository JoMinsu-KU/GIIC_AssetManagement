package org.example.giic_asset.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.giic_asset.dto.asset.AssetSearchCriteria;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetExcelExportServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetExcelExportService assetExcelExportService;

    @Test
    void exportsRequestedColumnsInRequestedOrder() throws Exception {
        Asset asset = Asset.builder()
                .assetNo("2411309")
                .name("모니터")
                .model("32UN650K")
                .value(480_000L)
                .purchaseDate(LocalDate.of(2025, 2, 25))
                .managingDept("경남지능화혁신사업단")
                .location("제조DX혁신연구실")
                .status(AssetStatus.NORMAL)
                .build();

        when(assetRepository.findAll(
                anySpecification(),
                any(Sort.class)))
                .thenReturn(List.of(asset));

        byte[] result = assetExcelExportService.export(
                AssetSearchCriteria.empty(),
                List.of("name", "assetNo", "value", "status"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            var firstAsset = sheet.getRow(1);

            assertThat(sheet.getSheetName()).isEqualTo("자산 목록");
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("물품명");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("자산번호");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("취득금액");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("상태");

            assertThat(firstAsset.getCell(0).getStringCellValue()).isEqualTo("모니터");
            assertThat(firstAsset.getCell(1).getStringCellValue()).isEqualTo("2411309");
            assertThat(firstAsset.getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(firstAsset.getCell(2).getNumericCellValue()).isEqualTo(480_000D);
            assertThat(firstAsset.getCell(3).getStringCellValue()).isEqualTo("정상");
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
        }
    }

    @Test
    void previewsAssetsWithoutImageQueriesOrPayloads() {
        Asset asset = Asset.builder()
                .assetNo("2411309")
                .name("모니터")
                .status(AssetStatus.NORMAL)
                .build();
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("assetNo"));

        when(assetRepository.findAll(
                anySpecification(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asset), pageable, 1));

        var result = assetExcelExportService.preview(AssetSearchCriteria.empty(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssetNo()).isEqualTo("2411309");
        assertThat(result.getContent().get(0).getImages()).isEmpty();
    }

    @Test
    void exportsLostStatusLabel() throws Exception {
        Asset asset = Asset.builder()
                .assetNo("2500001")
                .name("테스트 자산")
                .status(AssetStatus.LOST)
                .build();

        when(assetRepository.findAll(
                anySpecification(),
                any(Sort.class)))
                .thenReturn(List.of(asset));

        byte[] result = assetExcelExportService.export(
                AssetSearchCriteria.empty(),
                List.of("status"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("손망");
        }
    }

    @SuppressWarnings("unchecked")
    private Specification<Asset> anySpecification() {
        return any(Specification.class);
    }
}
