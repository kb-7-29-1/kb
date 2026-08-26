package com.salgosipo.destination.mapper;

import com.salgosipo.destination.domain.DestinationVO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DestinationMapper {
    // 목적지 이름 + 주소 검색
    DestinationVO findByNameAndAddress(
            @Param("destName") String destName,
            @Param("destAddress") String destAddress);

    // 같은 목적지 명 + 반경 30m 좌표 검색
    List<DestinationVO> findByNameInCoordinateRange(
            @Param("destName") String destName,
            @Param("minLatitude") BigDecimal minLatitude,
            @Param("maxLatitude") BigDecimal maxLatitude,
            @Param("minLongitude") BigDecimal minLongitude,
            @Param("maxLongitude") BigDecimal maxLongitude);

    // 목적지 명 또는 주소 부분 검색 (DB 0순위 검색)
    List<DestinationVO> searchByNameOrAddress(@Param("keyword") String keyword);

    // 목적지 명 검색
    DestinationVO findByName(@Param("destName") String destName);

    // ID 목록으로 목적지 목록 검색
    List<DestinationVO> findByIds(@Param("ids") List<Integer> ids);

    // 목적지 저장
    int insertDestination(DestinationVO destination);
}
