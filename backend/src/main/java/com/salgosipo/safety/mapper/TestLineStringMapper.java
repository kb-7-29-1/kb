package com.salgosipo.safety.mapper;

import com.salgosipo.safety.domain.TestLineStringVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * [임시/테스트 전용] TestLineStringSeedService 검증용 매퍼입니다.
 */
public interface TestLineStringMapper {

    List<Long> selectExistingPropertyIds(@Param("destinationId") Integer destinationId);

    int insertTestLineString(TestLineStringVO vo);

    int countTestLineString(@Param("destinationId") Integer destinationId);

    List<TestLineStringVO> selectTestLineStringPage(
            @Param("destinationId") Integer destinationId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}
