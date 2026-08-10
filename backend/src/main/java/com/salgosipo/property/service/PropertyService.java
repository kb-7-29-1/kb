package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertyPageResponseDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;

public interface PropertyService {
    PropertyPageResponseDTO getPropertyList(PropertySearchCondDTO cond, Long userId);

    PropertyDetailDTO getPropertyDetail(
            Long propertyId,
            Integer destinationId,
            Long userId
    );
}
