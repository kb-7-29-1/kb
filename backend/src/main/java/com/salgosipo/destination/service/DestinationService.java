package com.salgosipo.destination.service;

import com.salgosipo.destination.dto.DestinationDTO;
import java.util.List;

public interface DestinationService {
    // 목적지 검색
    List<DestinationDTO> searchDestinations(String keyword);

    // 목적지 등록: 동일 좌표 존재 시 기존 목적지 반환
    DestinationDTO saveDestination(DestinationDTO destination);
}
