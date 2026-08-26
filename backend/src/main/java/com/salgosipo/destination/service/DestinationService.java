package com.salgosipo.destination.service;

import com.salgosipo.destination.dto.DestinationDTO;
import java.util.List;

public interface DestinationService {
    // 목적지 검색
    List<DestinationDTO> searchDestinations(String keyword);

    // ID 목록으로 목적지 목록 조회
    List<DestinationDTO> getDestinationsByIds(List<Integer> ids);

    // 목적지 등록: 동일 좌표 존재 시 기존 목적지 반환
    DestinationDTO saveDestination(DestinationDTO destination);
}
