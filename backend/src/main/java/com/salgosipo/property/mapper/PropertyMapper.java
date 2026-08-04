package com.salgosipo.property.mapper;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PropertyMapper {
    // 매물 목록 조회 (필터 & 영역/중심점 & 정렬)
    List<PropertyListDTO> selectPropertyList(@Param("cond") PropertySearchCondDTO cond, @Param("userId") Long userId);

    // 매물 상세 정보 조회
    PropertyDetailDTO selectPropertyDetail(@Param("propertyId") Long propertyId, @Param("userId") Long userId);

    // 매물 이미지 목록 조회
    List<String> selectPropertyImageUrls(@Param("propertyId") Long propertyId);

    // 매물 댓글 태그 목록 조회
    List<String> selectPropertyTags(@Param("propertyId") Long propertyId);

    // 공공데이터 API 수집 매물 DB 배치 저장
    int insertBatchPublicProperties(@Param("list") List<PropertyListDTO> list);
}
