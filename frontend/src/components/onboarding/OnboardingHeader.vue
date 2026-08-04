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
  returnLabel: {
    type: String,
    default: '로그인 페이지로',
  },
});

defineEmits(['back', 'go-login']);
</script>

<template>
  <header class="onboarding-header">
    <div class="header-row">
      <button type="button" class="back-button" aria-label="이전 단계" @click="$emit('back')">
        <i class="fa-solid fa-chevron-left" aria-hidden="true"></i>
      </button>

      <h1>나에게 맞는 매물 찾기</h1>

      <span class="step-count">{{ currentStep }} / {{ totalSteps }}</span>
    </div>

    <div class="desktop-header">
      <button type="button" class="desktop-login-link" @click="$emit('go-login')">
        <i class="fa-solid fa-chevron-left" aria-hidden="true"></i>
        {{ returnLabel }}
      </button>

      <p class="desktop-logo">
        <i class="fa-solid fa-shield-halved" aria-hidden="true"></i>
        살고싶오
      </p>

      <div class="desktop-progress-row">
        <span>나에게 맞는 매물 찾기</span>
        <span>{{ currentStep }} / {{ totalSteps }}</span>
      </div>
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
  color: #6d7480;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

h1 {
  position: absolute;
  left: 50%;
  width: max-content;
  margin: 0;
  color: #17191d;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
  transform: translateX(-50%);
}

.step-count {
  z-index: 1;
  color: #6d7480;
  font-size: 9px;
  font-weight: 500;
}

.desktop-header {
  display: none;
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

@media (min-width: 768px) {
  .onboarding-header {
    height: 108px;
    padding: 0;
    background: transparent;
  }

  .header-row {
    display: none;
  }

  .desktop-header {
    display: block;
  }

  .desktop-login-link {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 0;
    border: 0;
    background: transparent;
    color: #94a3b8;
    font-family: inherit;
    font-size: 13px;
    cursor: pointer;
    line-height: 1;
  }

  .desktop-login-link i {
    font-size: 11px;
    line-height: 1;
  }

  .desktop-logo {
    display: flex;
    align-items: center;
    gap: 7px;
    margin: 10px 0 15px;
    color: #2a60f7;
    font-size: 20px;
    font-weight: 700;
  }

  .desktop-logo i {
    font-size: 18px;
  }

  .desktop-progress-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #94a3b8;
    font-size: 13px;
    font-weight: 600;
  }

  .progress-track {
    height: 5px;
    margin-top: 7px;
  }
}
</style>
