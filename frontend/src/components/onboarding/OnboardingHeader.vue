<script setup>
defineProps({
  currentStep: {
    type: Number,
    required: true,
  },
  totalSteps: {
    type: Number,
    default: 5,
  },
});

defineEmits(['back']);
</script>

<template>
  <header class="onboarding-header">
    <div class="header-row">
      <button type="button" class="back-button" aria-label="이전 단계" @click="$emit('back')">
        ‹
      </button>

      <h1>나에게 맞는 매물 찾기</h1>

      <span class="step-count">{{ currentStep }} / {{ totalSteps }}</span>
    </div>

    <div class="progress-track" aria-hidden="true">
      <div class="progress-value" :style="{ width: `${(currentStep / totalSteps) * 100}%` }" />
    </div>
  </header>
</template>

<style scoped>
.onboarding-header {
  box-sizing: border-box;
  height: max(100px, calc(env(safe-area-inset-top) + 62px));
  padding: max(50px, calc(env(safe-area-inset-top) + 12px)) 20px 0;
  background: #f8fafc;
}

.header-row {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 36px;
}

.back-button {
  z-index: 1;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1d1d1f;
  font-size: 26px;
  font-weight: 300;
  line-height: 30px;
  cursor: pointer;
}

h1 {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: max-content;
  margin: 0;
  color: #17191d;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
}

.step-count {
  z-index: 1;
  color: #6d7480;
  font-size: 9px;
  font-weight: 500;
}

.progress-track {
  width: 100%;
  height: 4px;
  margin-top: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf0f5;
}

.progress-value {
  height: 100%;
  border-radius: inherit;
  background: #2a60f7;
  transition: width 0.2s ease;
}
</style>
