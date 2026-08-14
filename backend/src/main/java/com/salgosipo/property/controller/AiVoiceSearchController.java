package com.salgosipo.property.controller;

import com.salgosipo.property.dto.AiVoiceSearchRequestDTO;
import com.salgosipo.property.dto.AiVoiceSearchResponseDTO;
import com.salgosipo.property.service.AiVoiceSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Log4j2
public class AiVoiceSearchController {
    private final AiVoiceSearchService aiVoiceSearchService;

    @PostMapping("/voice-search")
    public ResponseEntity<AiVoiceSearchResponseDTO> voiceSearch(
            @RequestBody(required = false) AiVoiceSearchRequestDTO request) {

        if (request == null || !StringUtils.hasText(request.getTranscript())) {
            return ResponseEntity.badRequest()
                    .body(AiVoiceSearchResponseDTO.error("인식된 음성 문장이 비어 있습니다."));
        }

        try {
            return ResponseEntity.ok(
                    aiVoiceSearchService.interpretText(
                            request.getTranscript(),
                            request.getCurrentFilters(),
                            request.getCurrentAmenities()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(AiVoiceSearchResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("AI voice search failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(AiVoiceSearchResponseDTO.error(
                            "AI 음성검색 처리에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }
}
