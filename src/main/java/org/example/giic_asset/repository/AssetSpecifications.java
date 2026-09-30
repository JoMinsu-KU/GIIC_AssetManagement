package org.example.giic_asset.repository;

import jakarta.persistence.criteria.JoinType;
import org.example.giic_asset.dto.asset.AssetSearchCriteria;
import org.example.giic_asset.entity.Asset;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    public static Specification<Asset> withCriteria(AssetSearchCriteria criteria) {
        AssetSearchCriteria safeCriteria = criteria == null ? AssetSearchCriteria.empty() : criteria;
        Specification<Asset> specification = (root, query, cb) -> cb.conjunction();

        if (safeCriteria.getStatus() != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("status"), safeCriteria.getStatus()));
        }
        if (StringUtils.hasText(safeCriteria.getDept())) {
            specification = specification.and(containsIgnoreCase("managingDept", safeCriteria.getDept()));
        }
        if (StringUtils.hasText(safeCriteria.getActualUserName())) {
            specification = specification.and(containsIgnoreCase("actualUserName", safeCriteria.getActualUserName()));
        }
        if (StringUtils.hasText(safeCriteria.getUserId())) {
            specification = specification.and((root, query, cb) -> cb.equal(
                    root.join("user", JoinType.LEFT).get("employeeNo"), safeCriteria.getUserId().trim()));
        }
        if (StringUtils.hasText(safeCriteria.getKeyword())) {
            String pattern = likePattern(safeCriteria.getKeyword());
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("assetNo")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("model")), pattern),
                    cb.like(cb.lower(root.get("actualUserName")), pattern)));
        }
        if (StringUtils.hasText(safeCriteria.getAssetNo())) {
            specification = specification.and(containsIgnoreCase("assetNo", safeCriteria.getAssetNo()));
        }
        if (StringUtils.hasText(safeCriteria.getName())) {
            specification = specification.and(containsIgnoreCase("name", safeCriteria.getName()));
        }
        if (StringUtils.hasText(safeCriteria.getModel())) {
            specification = specification.and(containsIgnoreCase("model", safeCriteria.getModel()));
        }
        if (StringUtils.hasText(safeCriteria.getLocation())) {
            specification = specification.and(containsIgnoreCase("location", safeCriteria.getLocation()));
        }
        if (safeCriteria.getPurchaseDateFrom() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(
                    root.get("purchaseDate"), safeCriteria.getPurchaseDateFrom()));
        }
        if (safeCriteria.getPurchaseDateTo() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(
                    root.get("purchaseDate"), safeCriteria.getPurchaseDateTo()));
        }
        if (safeCriteria.getValueMin() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(
                    root.get("value"), safeCriteria.getValueMin()));
        }
        if (safeCriteria.getValueMax() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(
                    root.get("value"), safeCriteria.getValueMax()));
        }

        return specification;
    }

    private static Specification<Asset> containsIgnoreCase(String field, String value) {
        String pattern = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get(field)), pattern);
    }

    private static String likePattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
