package com.salgosipo.property.service;

import com.salgosipo.property.dto.AiVoiceSearchResponseDTO;

public interface AiVoiceSearchService {
    AiVoiceSearchResponseDTO interpretText(
            String transcript,
            String currentFiltersJson,
            String currentAmenitiesJson);
}
