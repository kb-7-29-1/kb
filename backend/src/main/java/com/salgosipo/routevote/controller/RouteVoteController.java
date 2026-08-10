package com.salgosipo.routevote.controller;

import com.salgosipo.global.security.account.domain.CustomUser;
import com.salgosipo.routevote.dto.RouteVoteDTO;
import com.salgosipo.routevote.service.RouteVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route-votes")
@RequiredArgsConstructor
public class RouteVoteController {
    private final RouteVoteService routeVoteService;

    // 투표 정보 조회
    @GetMapping("/properties/{propertyId}")
    public ResponseEntity<RouteVoteDTO> getVoteSummary(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long propertyId,
            @RequestParam Integer destinationId) {
        return ResponseEntity.ok(
                routeVoteService.getVoteSummary(getUserId(customUser), propertyId, destinationId));
    }

    // 투표 저장
    @PostMapping("/properties/{propertyId}")
    public ResponseEntity<RouteVoteDTO> saveVote(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long propertyId,
            @RequestBody RouteVoteDTO request) {
        return ResponseEntity.ok(
                routeVoteService.saveVote(getUserId(customUser), propertyId, request));
    }

    private Long getUserId(CustomUser customUser) {
        if (customUser == null || customUser.getUser() == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return customUser.getUser().getUserId();
    }
}
