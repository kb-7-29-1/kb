package com.salgosipo.destination.controller;

import com.salgosipo.destination.dto.DestinationDTO;
import com.salgosipo.destination.service.DestinationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
@Log4j2
public class DestinationController {
    private final DestinationService destinationService;

    // 목적지 검색
    @GetMapping("/search")
    public ResponseEntity<List<DestinationDTO>> searchDestinations(@RequestParam String keyword) {
        List<DestinationDTO> destinations = destinationService.searchDestinations(keyword);
        return ResponseEntity.ok(destinations);
    }

    // 목적지 저장
    @PostMapping
    public ResponseEntity<DestinationDTO> saveDestination(@RequestBody DestinationDTO destination) {
        DestinationDTO savedDestination = destinationService.saveDestination(destination);
        return ResponseEntity.ok(savedDestination);
    }
}
