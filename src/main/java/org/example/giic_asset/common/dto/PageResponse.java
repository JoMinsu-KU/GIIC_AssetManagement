package org.example.giic_asset.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private PageInfo page_info;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page_info(PageInfo.builder()
                        .page(page.getNumber() + 1) // 1-based index as per spec
                        .size(page.getSize())
                        .total_elements(page.getTotalElements())
                        .total_pages(page.getTotalPages())
                        .first(page.isFirst())
                        .last(page.isLast())
                        .build())
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PageInfo {
        private int page;
        private int size;
        private long total_elements;
        private int total_pages;
        private boolean first;
        private boolean last;
    }
}
