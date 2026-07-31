package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import com.salgosipo.property.mapper.PropertyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {

    private final PropertyMapper propertyMapper;

    @Override
    public List<PropertyListDTO> getPropertyList(PropertySearchCondDTO cond, Long userId) {
        return propertyMapper.selectPropertyList(cond, userId);
    }

    @Override
    public PropertyDetailDTO getPropertyDetail(Long propertyId, Long userId) {
        PropertyDetailDTO detail = propertyMapper.selectPropertyDetail(propertyId, userId);
        if (detail != null) {
            List<String> imageUrls = propertyMapper.selectPropertyImageUrls(propertyId);
            detail.setImageUrls(imageUrls);

            List<String> tags = propertyMapper.selectPropertyTags(propertyId);
            detail.setTags(tags);
        }
        return detail;
    }
}
