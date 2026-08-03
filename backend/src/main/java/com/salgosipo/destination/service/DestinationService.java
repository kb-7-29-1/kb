package com.salgosipo.destination.service;

import com.salgosipo.destination.dto.DestinationDTO;
import java.util.List;

public interface DestinationService {
    // 목적지 검색
    List<DestinationDTO> searchDestinations(String keyword);
}
