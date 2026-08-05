package com.salgosipo.safety.mapper;

import com.salgosipo.safety.domain.PropertySafetyVO;
import com.salgosipo.safety.domain.SafetyDestinationVO;
import com.salgosipo.safety.domain.SafetyPropertyCoordinateVO;
import org.apache.ibatis.annotations.Param;

public interface SafetyMapper {

    PropertySafetyVO selectPropertySafety(
            @Param("propertyId") Long propertyId,
            @Param("destinationId") Integer destinationId
    );

    SafetyPropertyCoordinateVO selectPropertyCoordinate(
            @Param("propertyId") Long propertyId
    );

    SafetyDestinationVO selectDestinationById(
            @Param("destinationId") Integer destinationId
    );

    int upsertDestination(SafetyDestinationVO destination);

    int insertPropertySafetyIfAbsent(PropertySafetyVO propertySafety);
}
