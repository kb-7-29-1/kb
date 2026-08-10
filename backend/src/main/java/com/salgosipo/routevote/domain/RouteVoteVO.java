package com.salgosipo.routevote.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteVoteVO {
    private Long voteId;
    private Long userId;
    private Long propertyId;
    private Integer destinationId;
    private String voteType;
    private String delYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
