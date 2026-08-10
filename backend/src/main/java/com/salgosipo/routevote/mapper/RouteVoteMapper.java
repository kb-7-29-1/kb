package com.salgosipo.routevote.mapper;

import com.salgosipo.routevote.domain.RouteVoteVO;
import com.salgosipo.routevote.dto.RouteVoteDTO;
import org.apache.ibatis.annotations.Param;

public interface RouteVoteMapper {
    // 경로 투표 및 수정
    int upsertVote(RouteVoteVO routeVote);

    // 투표 조회
    RouteVoteDTO findVoteStats(
            @Param("userId") Long userId,
            @Param("propertyId") Long propertyId,
            @Param("destinationId") Integer destinationId);
}
