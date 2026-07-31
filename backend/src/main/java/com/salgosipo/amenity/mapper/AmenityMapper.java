package com.salgosipo.amenity.mapper;

import com.salgosipo.amenity.dto.AmenityRequestDTO;
import com.salgosipo.amenity.dto.AmenityResponseDTO;

import java.util.List;

public interface AmenityMapper {

    List<AmenityResponseDTO> getAmenitiesByFilter(AmenityRequestDTO request);
}
