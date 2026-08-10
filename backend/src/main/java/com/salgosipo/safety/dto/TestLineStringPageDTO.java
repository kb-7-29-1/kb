package com.salgosipo.safety.dto;

import java.util.List;

/**
 * [임시/테스트 전용] test_line_string 페이징 조회 응답입니다.
 */
public class TestLineStringPageDTO {
    private int totalCount;
    private int offset;
    private int limit;
    private List<TestLineStringDetailDTO> items;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<TestLineStringDetailDTO> getItems() {
        return items;
    }

    public void setItems(List<TestLineStringDetailDTO> items) {
        this.items = items;
    }
}
