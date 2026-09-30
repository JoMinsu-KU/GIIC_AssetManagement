package org.example.giic_asset.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class AssetPageSort {

    private AssetPageSort() {
    }

    static Pageable stable(Pageable pageable, Sort defaultSort) {
        Sort sort = pageable.getSort().isUnsorted() ? defaultSort : pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
