package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import com.salgosipo.property.mapper.PropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {

    private final PropertyMapper propertyMapper;

    @Override
    @Cacheable(value = "propertyList", key = "#cond != null ? #cond.toCacheKey() : ''", unless = "#result == null || #result.isEmpty()")
    public List<PropertyListDTO> getPropertyList(PropertySearchCondDTO cond, Long userId) {
        try {
            List<PropertyListDTO> list = propertyMapper.selectPropertyList(cond, userId);
            if (list != null && !list.isEmpty()) {
                list.forEach(item -> item.setDataSource("DB"));
                return list;
            }
        } catch (Exception e) {
            log.error("DB property search error: {}", e.getMessage(), e);
        }
        return List.of();
    }

    @Override
    @Cacheable(value = "propertyDetail", key = "#propertyId + '_' + "
            + "(#destinationId != null ? #destinationId : 0) + '_' + "
            + "(#userId != null ? #userId : 0)")
    public PropertyDetailDTO getPropertyDetail(
            Long propertyId,
            Integer destinationId,
            Long userId) {
        PropertyDetailDTO detail = propertyMapper.selectPropertyDetail(
                propertyId,
                destinationId,
                userId);
        if (detail != null) {
            detail.setImageUrls(
                    propertyMapper.selectPropertyImageUrls(propertyId));
            detail.setTags(
                    propertyMapper.selectPropertyTags(propertyId));
        }
        return detail;
    }
}
