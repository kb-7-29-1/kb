package com.salgosipo.property.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImageVO {
    private Long imageId;
    private Long propertyId;
    private String imageUrl;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
