package org.example.giic_asset.dto.asset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DisposalRequest {
    private String disposalType;
    private String reason;
}
