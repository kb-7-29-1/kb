package com.salgosipo.property.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVoiceSearchResponseDTO {
    private boolean success;
    private String transcript;
    private String action;
    private String destinationQuery;
    private FilterPatchDTO filters;
    @Builder.Default
    private List<String> resetFields = new ArrayList<>();
    private String amenityMode;
    @Builder.Default
    private List<AmenityPatchDTO> amenities = new ArrayList<>();
    private String message;

    public static AiVoiceSearchResponseDTO error(String message) {
        return AiVoiceSearchResponseDTO.builder()
                .success(false)
                .action("NO_CHANGE")
                .amenityMode("UNCHANGED")
                .filters(new FilterPatchDTO())
                .resetFields(new ArrayList<>())
                .amenities(new ArrayList<>())
                .message(message)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterPatchDTO {
        private String tradeType;
        private Integer minDeposit;
        private Integer maxDeposit;
        private Integer minRent;
        private Integer maxRent;
        private Integer minSafetyScore;
        private String transportMode;
        private Integer minTravelTime;
        private Integer travelTime;
        private String walkPace;
        private Integer flexTime;
        private String selectedLoanId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmenityPatchDTO {
        private Integer amenityType;
        private Integer walkTimeMinutes;
    }
}
