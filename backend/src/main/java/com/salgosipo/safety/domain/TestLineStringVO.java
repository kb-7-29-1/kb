package com.salgosipo.safety.domain;

/**
 * [임시/테스트 전용] 세종대 도보권 매물의 TMAP 경로(linestring)를
 * 눈으로 검증하기 위해 test_line_string 테이블에 저장하는 VO입니다.
 * 검증이 끝나면 관련 코드와 테이블을 삭제해도 됩니다.
 */
public class TestLineStringVO {
    private Long propertyId;
    private Integer destinationId;
    private String lineStringJson;

    public TestLineStringVO() {
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }

    public String getLineStringJson() {
        return lineStringJson;
    }

    public void setLineStringJson(String lineStringJson) {
        this.lineStringJson = lineStringJson;
    }
}
