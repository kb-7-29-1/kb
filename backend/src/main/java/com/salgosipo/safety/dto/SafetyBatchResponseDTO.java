package com.salgosipo.safety.dto;

import java.util.List;

/**
 * 여러 매물의 안전점수 준비 결과입니다.
 */
public class SafetyBatchResponseDTO {
    private Integer destinationId;
    private Integer requestedCount;
    private Integer successCount;
    private Integer cacheHitCount;
    private Integer calculatedCount;
    private Integer failedCount;
    private List<SafetyBatchItemDTO> items;

    public SafetyBatchResponseDTO() {
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }

    public Integer getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(Integer requestedCount) {
        this.requestedCount = requestedCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getCacheHitCount() {
        return cacheHitCount;
    }

    public void setCacheHitCount(Integer cacheHitCount) {
        this.cacheHitCount = cacheHitCount;
    }

    public Integer getCalculatedCount() {
        return calculatedCount;
    }

    public void setCalculatedCount(Integer calculatedCount) {
        this.calculatedCount = calculatedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<SafetyBatchItemDTO> getItems() {
        return items;
    }

    public void setItems(List<SafetyBatchItemDTO> items) {
        this.items = items;
    }
}
