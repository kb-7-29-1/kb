package com.salgosipo.routevote.service;

import com.salgosipo.routevote.dto.RouteVoteDTO;

public interface RouteVoteService {
    // 투표 결과 조회
    RouteVoteDTO getVoteSummary(Long userId, Long propertyId, Integer destinationId);

    // 투표 저장
    RouteVoteDTO saveVote(Long userId, Long propertyId, RouteVoteDTO routeVoteDTO);
}
