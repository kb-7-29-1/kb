<template>
  <Transition name="toast">
    <div
      v-if="toastMessage"
      class="fixed top-20 left-1/2 transform -translate-x-1/2 z-50 bg-slate-900/95 text-amber-100 px-5 py-3 rounded-2xl text-xs font-bold shadow-2xl backdrop-blur-md flex items-center gap-3 border border-amber-500/40 pointer-events-auto"
    >
      <span class="text-base">⚠️</span>
      <span>{{ toastMessage }}</span>
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
