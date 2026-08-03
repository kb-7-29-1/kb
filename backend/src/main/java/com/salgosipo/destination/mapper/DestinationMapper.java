package com.salgosipo.destination.mapper;

import com.salgosipo.destination.domain.DestinationVO;

public interface DestinationMapper {
    // 목적지 저장
    int insertDestination(DestinationVO destination);
}
