<template>
  <Transition name="app-toast">
    <div
      v-if="toastState.visible"
      class="app-toast-box fixed top-6 left-1/2 -translate-x-1/2 z-[9999] pointer-events-auto flex items-center gap-2.5 px-4 py-2.5 sm:px-5 sm:py-3 rounded-2xl text-xs sm:text-sm font-extrabold shadow-2xl backdrop-blur-md border transition-all duration-300 max-w-[90vw] sm:max-w-md w-auto"
      :class="[
        toastState.type === 'success'
          ? 'bg-slate-900/95 text-emerald-300 border-emerald-500/40 shadow-emerald-950/40'
          : toastState.type === 'error'
            ? 'bg-slate-900/95 text-rose-300 border-rose-500/40 shadow-rose-950/40'
            : toastState.type === 'warning'
              ? 'bg-slate-900/95 text-amber-200 border-amber-500/40 shadow-amber-950/40'
              : 'bg-slate-900/95 text-blue-200 border-blue-500/40 shadow-blue-950/40'
      ]"
    >
      <span class="text-base shrink-0 select-none">{{ toastState.icon }}</span>
      <span class="leading-snug whitespace-pre-line text-slate-100 font-bold flex-1 break-keep">{{ toastState.message }}</span>
      <button
        type="button"
        class="ml-2 text-slate-400 hover:text-white text-xs font-black p-1 transition-colors shrink-0"
        aria-label="닫기"
        @click="hideToast"
      >
        ✕
      </button>
    </div>
  </Transition>
</template>

<script setup>
import { useAppToast } from '@/composables/useAppToast.js';

const { toastState, hideToast } = useAppToast();
</script>

<style scoped>
.app-toast-enter-active,
.app-toast-leave-active {
  transition: all 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}

.app-toast-enter-from,
.app-toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -16px) scale(0.95);
}

.app-toast-enter-to,
.app-toast-leave-from {
  opacity: 1;
  transform: translate(-50%, 0) scale(1);
}
</style>
