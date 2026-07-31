package com.salgosipo.destination.controller;

import com.salgosipo.destination.dto.DestinationDTO;
import com.salgosipo.destination.service.DestinationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
public class DestinationController {
<<<<<<< HEAD
    private final DestinationService destinationService;

    @GetMapping("/search")
    public ResponseEntity<List<DestinationDTO>> searchDestinations(@RequestParam String keyword) {
=======

    private final DestinationService destinationService;

    /**
     * 목적지 검색
     * GET /api/destinations/search?keyword=세종대학교
     */
    @GetMapping("/search")
    public ResponseEntity<List<DestinationDTO>> searchDestinations(
            @RequestParam String keyword
    ) {
>>>>>>> 36e37d2 (feat: 네이버 검색 API 구현)
        return ResponseEntity.ok(destinationService.searchDestinations(keyword));
    }
}
