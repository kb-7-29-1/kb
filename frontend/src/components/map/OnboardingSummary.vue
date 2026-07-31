<script setup>
defineEmits(['close']);
</script>

<template>
  <section class="summary">
    <div class="summary-top">
      <div>
        <p class="summary-label">목적지</p>
        <div class="summary-title">
          <span class="location-icon">⌖</span>
          <strong>세종대학교</strong>
        </div>
      </div>

      <button class="close-button" type="button" aria-label="필터 닫기" @click="$emit('close')">
        ×
      </button>
    </div>

    <div class="condition-chips">
      <span>도보 15분</span>
      <span>보증 3,000만 이하</span>
      <span>위반건축물 제외</span>
    </div>
  </section>
</template>

<style scoped>
.summary {
  position: relative;
  padding-right: 42px;
  margin-bottom: 20px;
}

.summary-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.summary-label {
  margin: 0 0 5px;
  color: #7d8797;
  font-size: 12px;
  font-weight: 600;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1e293b;
  font-size: 18px;
}

.location-icon {
  color: #3d55f6;
}

.close-button {
  position: absolute;
  top: 0;
  right: 0;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #8b95a7;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
}

.condition-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.condition-chips span {
  padding: 5px 9px;
  border: 1px solid #dce4ff;
  border-radius: 999px;
  background: #f3f6ff;
  color: #5470ed;
  font-size: 11px;
  font-weight: 600;
}
</style>
