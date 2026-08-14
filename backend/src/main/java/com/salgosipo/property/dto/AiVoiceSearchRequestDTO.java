package com.salgosipo.property.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiVoiceSearchRequestDTO {
    private String transcript;
    private String currentFilters;
    private String currentAmenities;
}
