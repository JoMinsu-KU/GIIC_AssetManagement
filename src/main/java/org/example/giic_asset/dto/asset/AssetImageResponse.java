package org.example.giic_asset.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.AssetImage;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssetImageResponse {
    private Long id;
    private String imageUrl;
    private boolean isPrimary;

    public static AssetImageResponse from(AssetImage image) {
        return AssetImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.isPrimary())
                .build();
    }
}
