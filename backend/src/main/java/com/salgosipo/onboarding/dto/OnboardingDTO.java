package com.salgosipo.onboarding.dto;

import com.salgosipo.destination.dto.DestinationDTO;
import com.salgosipo.onboarding.domain.OnboardingVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDTO {
    private Integer userId;
    private Integer destinationId;
    private String destinationType;
    private String transportMode;
    private Integer maxTravelTime;
    private Integer budgetDeposit;
    private Integer budgetRent;
    private Integer minSafetyScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DestinationDTO destination;

    public OnboardingVO toVO() {
        return OnboardingVO.builder()
                .userId(userId)
                .destinationId(destinationId)
                .destinationType(destinationType)
                .transportMode(transportMode)
                .maxTravelTime(maxTravelTime)
                .budgetDeposit(budgetDeposit)
                .budgetRent(budgetRent)
                .minSafetyScore(minSafetyScore)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
