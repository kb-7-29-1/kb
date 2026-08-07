package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;

import java.util.List;

public interface PropertyService {
    List<PropertyListDTO> getPropertyList(PropertySearchCondDTO cond, Long userId);

    PropertyDetailDTO getPropertyDetail(
            Long propertyId,
            Integer destinationId,
            Long userId
    );
}
