package com.salgosipo.routevote.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteVoteDTO {
    // route_vote 테이블 컬럼
    private Long voteId;
    private Long userId;
    private Long propertyId;
    private Integer destinationId;
    private String voteType;
    private String delYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 투표 결과 조회용 필드
    private boolean hasVoted;
    private String myVote;
    private int safeCount;
    private int unsafeCount;
    private int totalVotes;
    private boolean reliable;
    private Integer safePercent;
}
