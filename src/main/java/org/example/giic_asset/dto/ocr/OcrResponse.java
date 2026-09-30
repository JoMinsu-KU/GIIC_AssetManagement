package org.example.giic_asset.dto.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {
    private String assetNo; // 자산번호
    private String name; // 자산명/제품명
    private String managingDept; // 관리부서 (NEW - was missing)
    private String purchaseDate; // 취득일자 (NEW - was missing)
    private String spec; // 규격/사양
    private Double confidence; // 인식 신뢰도
    private String rawText; // 원본 OCR 텍스트
}
