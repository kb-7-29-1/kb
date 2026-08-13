<template>
  <Transition name="toast">
    <div
      v-if="toastMessage"
      class="district-toast fixed left-1/2 transform -translate-x-1/2 z-50 bg-slate-900/95 text-amber-100 px-5 py-3 rounded-2xl text-xs font-bold shadow-2xl backdrop-blur-md flex items-center gap-3 border border-amber-500/40 pointer-events-auto"
    >
      <span class="text-base">⚠️</span>
      <span class="toast-message">{{ toastMessage }}</span>
      <button
        type="button"
        class="ml-2 text-slate-400 hover:text-white text-xs font-bold"
        @click="hideToast"
      >
        ✕
      </button>
    </div>
  </Transition>
</template>

<script setup>
import { useDistrictToast } from '@/composables/useDistrictToast.js';

const { toastMessage, hideToast } = useDistrictToast();
</script>

<style scoped>
.district-toast {
  /* 모바일 헤더와 상단 필터 버튼 아래에 표시 */
  top: calc(var(--app-header-height, 56px) + 80px);
}

@media (max-width: 767px) {
  .district-toast {
    width: calc(100vw - 24px);
    max-width: 380px;
  }

  .toast-message {
    min-width: 0;
    line-height: 1.5;
  }
}

@media (min-width: 1280px) {
  .district-toast {
    /* PC 퀵 필터 행을 가리지 않도록 아래에 표시 */
    top: calc(var(--app-header-height, 56px) + 80px);
  }
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}
</style>
