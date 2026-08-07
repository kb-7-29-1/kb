package com.salgosipo.property.mapper;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PropertyMapper {
    List<PropertyListDTO> selectPropertyList(
            @Param("cond") PropertySearchCondDTO cond,
            @Param("userId") Long userId
    );

    PropertyDetailDTO selectPropertyDetail(
            @Param("propertyId") Long propertyId,
            @Param("destinationId") Integer destinationId,
            @Param("userId") Long userId
    );

    List<String> selectPropertyImageUrls(
            @Param("propertyId") Long propertyId
    );

    List<String> selectPropertyTags(
            @Param("propertyId") Long propertyId
    );

    int insertBatchPublicProperties(
            @Param("list") List<PropertyListDTO> list
    );

    List<PropertyListDTO> selectAllPropertiesToGeocode();

    int updatePropertyCoordinates(
            @Param("propertyId") Long propertyId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );

    int updateBatchPropertyCoordinates(
            @Param("list") List<PropertyListDTO> list
    );

    int updateBuildingRegisterInfo(
            @Param("propertyId") Long propertyId,
            @Param("useAprDay") String useAprDay,
            @Param("structureName") String structureName,
            @Param("mainPurposeName") String mainPurposeName,
            @Param("earthquakeProofYn") String earthquakeProofYn,
            @Param("illegalReason") String illegalReason,
            @Param("isIllegalBuilding") Boolean isIllegalBuilding
    );
}
