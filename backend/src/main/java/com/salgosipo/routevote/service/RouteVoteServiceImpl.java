package com.salgosipo.routevote.service;

import com.salgosipo.routevote.domain.RouteVoteVO;
import com.salgosipo.routevote.dto.RouteVoteDTO;
import com.salgosipo.routevote.mapper.RouteVoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Log4j2
public class RouteVoteServiceImpl implements RouteVoteService {
    // 20명 이상 참여 시 결과 나타냄
    private static final int MIN_RELIABLE_VOTE_COUNT = 20;
    private static final String SAFE = "SAFE";
    private static final String UNSAFE = "UNSAFE";

    private final RouteVoteMapper routeVoteMapper;

    // 투표 현황 조회
    @Override
    @Transactional(readOnly = true)
    public RouteVoteDTO getVoteSummary(Long userId, Long propertyId, Integer destinationId) {
        validateIds(userId, propertyId, destinationId);
        return toSummary(userId, propertyId, destinationId);
    }

    // 투표 저장 및 변경
    @Override
    @Transactional
    public RouteVoteDTO saveVote(Long userId, Long propertyId, RouteVoteDTO routeVoteDTO) {
        if (routeVoteDTO == null) {
            throw new IllegalArgumentException("투표 정보가 필요합니다.");
        }

        validateIds(userId, propertyId, routeVoteDTO.getDestinationId());
        String voteType = normalizeVoteType(routeVoteDTO.getVoteType());

        RouteVoteVO routeVote = RouteVoteVO.builder()
                .userId(userId)
                .propertyId(propertyId)
                .destinationId(routeVoteDTO.getDestinationId())
                .voteType(voteType)
                .build();

        routeVoteMapper.upsertVote(routeVote);
        log.info("[RouteVote] saved: userId={}, propertyId={}, destinationId={}, voteType={}",
                userId, propertyId, routeVoteDTO.getDestinationId(), voteType);

        return toSummary(userId, propertyId, routeVoteDTO.getDestinationId());
    }

    private RouteVoteDTO toSummary(Long userId, Long propertyId, Integer destinationId) {
        RouteVoteDTO stats = routeVoteMapper.findVoteStats(userId, propertyId, destinationId);
        int totalVotes = stats == null ? 0 : stats.getTotalVotes();
        int safeCount = stats == null ? 0 : stats.getSafeCount();
        int unsafeCount = stats == null ? 0 : stats.getUnsafeCount();
        String myVote = stats == null ? null : stats.getMyVote();
        boolean reliable = totalVotes >= MIN_RELIABLE_VOTE_COUNT;

        return RouteVoteDTO.builder()
                .propertyId(propertyId)
                .destinationId(destinationId)
                .hasVoted(StringUtils.hasText(myVote))
                .myVote(myVote)
                .safeCount(safeCount)
                .unsafeCount(unsafeCount)
                .totalVotes(totalVotes)
                .reliable(reliable)
                .safePercent(reliable ? Math.round((safeCount * 100F) / totalVotes) : null)
                .build();
    }

    private String normalizeVoteType(String voteType) {
        if (!StringUtils.hasText(voteType)) {
            throw new IllegalArgumentException("안전 유형을 선택해주세요.");
        }

        String normalized = voteType.trim().toUpperCase();
        if (!SAFE.equals(normalized) && !UNSAFE.equals(normalized)) {
            throw new IllegalArgumentException("SAFE 또는 UNSAFE만 선택 가능합니다.");
        }
        return normalized;
    }

    private void validateIds(Long userId, Long propertyId, Integer destinationId) {
        if (userId == null || userId <= 0 || propertyId == null || propertyId <= 0
                || destinationId == null || destinationId <= 0) {
            throw new IllegalArgumentException("투표에 필요한 사용자·매물·목적지 정보가 올바르지 않습니다.");
        }
    }
}
