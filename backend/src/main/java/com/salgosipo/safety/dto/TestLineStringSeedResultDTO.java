package com.salgosipo.safety.dto;

import java.util.List;

/**
 * [임시/테스트 전용] TestLineStringSeedService 실행 결과 요약입니다.
 */
public class TestLineStringSeedResultDTO {
    private int fetchedCount;
    private int alreadyExistedCount;
    private int succeededCount;
    private int failedCount;
    private List<Long> failedPropertyIds;

    public int getFetchedCount() {
        return fetchedCount;
    }

    public void setFetchedCount(int fetchedCount) {
        this.fetchedCount = fetchedCount;
    }

    public int getAlreadyExistedCount() {
        return alreadyExistedCount;
    }

    public void setAlreadyExistedCount(int alreadyExistedCount) {
        this.alreadyExistedCount = alreadyExistedCount;
    }

    public int getSucceededCount() {
        return succeededCount;
    }

    public void setSucceededCount(int succeededCount) {
        this.succeededCount = succeededCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<Long> getFailedPropertyIds() {
        return failedPropertyIds;
    }

    public void setFailedPropertyIds(List<Long> failedPropertyIds) {
        this.failedPropertyIds = failedPropertyIds;
    }
}