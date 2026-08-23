<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['click']);
</script>

<template>
  <button
    type="button"
    class="current-location-btn flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-white text-[#5b8df9] shadow-sm transition-all hover:border-sky-200 hover:bg-sky-50/60 hover:text-sky-600 hover:shadow-md active:scale-90 cursor-pointer"
    :class="{
      'border-sky-300 bg-sky-50 text-sky-500 shadow-inner': loading,
    }"
    title="현재 내 위치로 이동"
    aria-label="현재 내 위치로 이동"
    @click="$emit('click')"
  >
    <i
      class="fa-solid fa-location-arrow text-[14px] text-[#5b8df9] transition-transform duration-300 -rotate-45"
      :class="{ 'animate-spin': loading, 'hover:scale-110': !loading }"
      aria-hidden="true"
    ></i>
  </button>
</template>

<style scoped>
.current-location-btn {
  box-sizing: border-box;
  user-select: none;
  touch-action: manipulation;
}
</style>
