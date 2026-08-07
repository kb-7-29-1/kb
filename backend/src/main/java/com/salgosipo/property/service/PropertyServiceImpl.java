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
    private final PublicDataApiService publicDataApiService;

    @Override
    @Cacheable(value = "propertyList", key = "(#cond != null ? #cond.toCacheKey() : '') + '_' + (#userId != null ? #userId : 0)", unless = "#result == null || #result.isEmpty()")
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
        return Collections.emptyList();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        /*
         * 목적지별 안전점수는 property_safety의 실제 property_id가 필요합니다.
         * destinationId가 있는 메인 화면 요청에서는 공공 API 임시 매물로
         * 대체하지 않고 DB 조회 결과를 그대로 반환합니다.
         */
        if (cond != null && cond.getDestinationId() != null) {
            return List.of();
        }

        Double centerLat = cond != null ? cond.getLat() : null;
        Double centerLng = cond != null ? cond.getLng() : null;
        return publicDataApiService.getGwangjinMonthlyProperties(
                centerLat,
                centerLng
        );
    }

    @Override
    @Cacheable(
            value = "propertyDetail",
            key = "#propertyId + '_' + "
                    + "(#destinationId != null ? #destinationId : 0) + '_' + "
                    + "(#userId != null ? #userId : 0)"
    )
    public PropertyDetailDTO getPropertyDetail(
            Long propertyId,
            Integer destinationId,
            Long userId
    ) {
        PropertyDetailDTO detail = propertyMapper.selectPropertyDetail(
                propertyId,
                destinationId,
                userId
        );
        if (detail != null) {
            detail.setImageUrls(
                    propertyMapper.selectPropertyImageUrls(propertyId)
            );
            detail.setTags(
                    propertyMapper.selectPropertyTags(propertyId)
            );
        }
        return detail;
    }
}
