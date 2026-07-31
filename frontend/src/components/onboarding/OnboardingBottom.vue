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

defineEmits(['previous', 'next', 'complete']);
</script>

<template>
  <nav class="onboarding-bottom" aria-label="온보딩 단계 이동">
    <button
      v-if="currentStep > 1"
      type="button"
      class="secondary-button"
      @click="$emit('previous')"
    >
      <i class="fa-solid fa-chevron-left" aria-hidden="true"></i>
      이전
    </button>

    <button
      v-if="currentStep < totalSteps"
      type="button"
      class="primary-button"
      @click="$emit('next')"
    >
      다음
      <i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
    </button>

    <button v-else type="button" class="primary-button" @click="$emit('complete')">
      지도에서 매물 보기
      <i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
    </button>
  </nav>
</template>

<style scoped>
.onboarding-bottom {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  gap: 10px;
  box-sizing: border-box;
  width: 100%;
  min-height: 110px;
  padding: 16px 20px max(16px, calc(env(safe-area-inset-bottom) + 10px));
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 65px;
  border-radius: 14px;
  font-family: inherit;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

button i {
  font-size: 13px;
}

.primary-button {
  flex: 1;
  border: 1px solid #2a60f7;
  background: #2a60f7;
  color: #fff;
}

.secondary-button {
  flex: 0 0 88px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
}

button:active {
  transform: scale(0.99);
}
</style>
