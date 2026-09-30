package org.example.giic_asset.dto.loss;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetLossRequestCreate {

    @NotNull(message = "자산 ID는 필수입니다.")
    private Long assetId;

    @NotNull(message = "손망 일자는 필수입니다.")
    @PastOrPresent(message = "손망 일자는 오늘 이후일 수 없습니다.")
    private LocalDate lossDate;

    @NotBlank(message = "비고는 필수입니다.")
    @Size(max = 1000, message = "비고는 1000자 이하로 입력해주세요.")
    private String remarks;
}
