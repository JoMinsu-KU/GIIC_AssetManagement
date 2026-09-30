package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.example.giic_asset.dto.asset.AssetExportColumn;
import org.example.giic_asset.dto.asset.AssetResponse;
import org.example.giic_asset.dto.asset.AssetSearchCriteria;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.repository.AssetRepository;
import org.example.giic_asset.repository.AssetSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetExcelExportService {

    private static final int EXCEL_COLUMN_WIDTH_UNIT = 256;

    private final AssetRepository assetRepository;

    @Transactional(readOnly = true)
    public Page<AssetResponse> preview(AssetSearchCriteria criteria, Pageable pageable) {
        Pageable previewPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("assetNo").ascending());

        return assetRepository.findAll(AssetSpecifications.withCriteria(criteria), previewPageable)
                .map(asset -> AssetResponse.from(asset, List.of()));
    }

    @Transactional(readOnly = true)
    public byte[] export(AssetSearchCriteria criteria, List<String> requestedColumns) {
        List<AssetExportColumn> columns = AssetExportColumn.resolve(requestedColumns);
        List<Asset> assets = assetRepository.findAll(
                AssetSpecifications.withCriteria(criteria),
                Sort.by(Sort.Direction.ASC, "assetNo"));

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try (workbook; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("자산 목록");
            ExcelStyles styles = createStyles(workbook);

            createHeader(sheet, columns, styles.header());
            createRows(sheet, assets, columns, styles);
            configureSheet(sheet, columns, assets.size());

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("자산 목록 엑셀 파일을 생성하지 못했습니다.", exception);
        } finally {
            workbook.dispose();
        }
    }

    private void createHeader(Sheet sheet, List<AssetExportColumn> columns, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(28);
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(columns.get(columnIndex).getHeader());
            cell.setCellStyle(headerStyle);
        }
    }

    private void createRows(
            Sheet sheet,
            List<Asset> assets,
            List<AssetExportColumn> columns,
            ExcelStyles styles) {
        for (int rowIndex = 0; rowIndex < assets.size(); rowIndex++) {
            Asset asset = assets.get(rowIndex);
            Row row = sheet.createRow(rowIndex + 1);
            row.setHeightInPoints(22);

            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                Cell cell = row.createCell(columnIndex);
                writeCell(cell, asset, columns.get(columnIndex), styles);
            }
        }
    }

    private void writeCell(Cell cell, Asset asset, AssetExportColumn column, ExcelStyles styles) {
        switch (column) {
            case ASSET_NO -> setText(cell, asset.getAssetNo(), styles.text());
            case NAME -> setText(cell, asset.getName(), styles.text());
            case MODEL -> setText(cell, asset.getModel(), styles.text());
            case SPEC -> setText(cell, asset.getSpec(), styles.text());
            case VALUE -> {
                if (asset.getValue() != null) {
                    cell.setCellValue(asset.getValue());
                }
                cell.setCellStyle(styles.currency());
            }
            case PURCHASE_DATE -> {
                if (asset.getPurchaseDate() != null) {
                    cell.setCellValue(Date.valueOf(asset.getPurchaseDate()));
                }
                cell.setCellStyle(styles.date());
            }
            case MANAGING_DEPT -> setText(cell, asset.getManagingDept(), styles.text());
            case LOCATION -> setText(cell, asset.getLocation(), styles.text());
            case STATUS -> setText(cell, getStatusLabel(asset.getStatus()), styles.center());
            case ACTUAL_USER_NAME -> setText(cell,
                    asset.isSharedUse() ? "공용" : asset.getActualUserName(), styles.text());
            case ACTUAL_USER_DEPARTMENT -> setText(cell, asset.getActualUserDepartment(), styles.text());
            case ACTUAL_USER_CONTACT -> setText(cell, asset.getActualUserContact(), styles.text());
            case REGISTERED_BY_NAME -> setText(
                    cell,
                    asset.getRegisteredBy() == null ? null : asset.getRegisteredBy().getName(),
                    styles.text());
            case CREATED_AT -> {
                if (asset.getCreatedAt() != null) {
                    cell.setCellValue(Timestamp.valueOf(asset.getCreatedAt()));
                }
                cell.setCellStyle(styles.dateTime());
            }
        }
    }

    private void setText(Cell cell, String value, CellStyle style) {
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void configureSheet(Sheet sheet, List<AssetExportColumn> columns, int assetCount) {
        sheet.createFreezePane(0, 1);
        if (!columns.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(assetCount, 1), 0, columns.size() - 1));
        }
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            sheet.setColumnWidth(
                    columnIndex,
                    Math.min(columns.get(columnIndex).getWidth() * EXCEL_COLUMN_WIDTH_UNIT, 255 * EXCEL_COLUMN_WIDTH_UNIT));
        }
    }

    private ExcelStyles createStyles(SXSSFWorkbook workbook) {
        CellStyle header = workbook.createCellStyle();
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(header);

        Font headerFont = workbook.createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        header.setFont(headerFont);

        CellStyle text = workbook.createCellStyle();
        text.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(text);

        CellStyle center = workbook.createCellStyle();
        center.cloneStyleFrom(text);
        center.setAlignment(HorizontalAlignment.CENTER);

        CellStyle currency = workbook.createCellStyle();
        currency.cloneStyleFrom(text);
        currency.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

        CellStyle date = workbook.createCellStyle();
        date.cloneStyleFrom(center);
        date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

        CellStyle dateTime = workbook.createCellStyle();
        dateTime.cloneStyleFrom(center);
        dateTime.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

        return new ExcelStyles(header, text, center, currency, date, dateTime);
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private String getStatusLabel(AssetStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case NORMAL -> "정상";
            case ABNORMAL -> "고장";
            case UNUSED -> "미사용";
            case DISPOSED -> "폐기";
            case REPAIRING -> "수리중";
            case TRANSFER_PENDING -> "이관 대기";
            case DISPOSAL_PENDING -> "폐기 대기";
            case DISPOSAL_REJECTED -> "폐기 반려";
            case LOSS_PENDING -> "손망 승인 대기";
            case LOST -> "손망";
        };
    }

    private record ExcelStyles(
            CellStyle header,
            CellStyle text,
            CellStyle center,
            CellStyle currency,
            CellStyle date,
            CellStyle dateTime) {
    }
}
