package org.example.giic_asset.dto.asset;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum AssetExportColumn {
    ASSET_NO("assetNo", "자산번호", 14),
    NAME("name", "물품명", 24),
    MAJOR_CATEGORY("majorCategory", "대분류", 22),
    SUBCATEGORY("subcategory", "중분류", 24),
    MODEL("model", "모델명", 24),
    SPEC("spec", "규격", 30),
    VALUE("value", "취득금액", 16),
    PURCHASE_DATE("purchaseDate", "취득일자", 14),
    MANAGING_DEPT("managingDept", "관리부서", 24),
    LOCATION("location", "비치장소", 42),
    STATUS("status", "상태", 14),
    ACTUAL_USER_NAME("actualUserName", "실사용자", 16),
    ACTUAL_USER_DEPARTMENT("actualUserDepartment", "실사용자 부서", 22),
    ACTUAL_USER_CONTACT("actualUserContact", "실사용자 연락처", 22),
    REGISTERED_BY_NAME("registeredByName", "등록자", 16),
    CREATED_AT("createdAt", "등록일", 18);

    private static final List<AssetExportColumn> DEFAULT_COLUMNS = List.of(
            ASSET_NO,
            NAME,
            MAJOR_CATEGORY,
            SUBCATEGORY,
            MODEL,
            SPEC,
            VALUE,
            PURCHASE_DATE,
            MANAGING_DEPT,
            LOCATION,
            STATUS);

    private final String key;
    private final String header;
    private final int width;

    public static List<AssetExportColumn> resolve(List<String> requestedColumns) {
        if (requestedColumns == null || requestedColumns.isEmpty()) {
            return DEFAULT_COLUMNS;
        }

        Set<String> requested = new LinkedHashSet<>(requestedColumns);
        List<AssetExportColumn> resolved = new ArrayList<>();
        for (String key : requested) {
            Arrays.stream(values())
                    .filter(column -> column.key.equals(key))
                    .findFirst()
                    .ifPresent(resolved::add);
        }

        return resolved.isEmpty() ? DEFAULT_COLUMNS : List.copyOf(resolved);
    }
}
