<script setup>
import { computed } from 'vue';

const props = defineProps({
  destination: {
    type: Object,
    default: () => ({
      name: '세종대학교',
      lat: 37.5502,
      lng: 127.0731,
    }),
  },
});

const displayName = computed(() => {
  if (!props.destination || !props.destination.name) return '주 목적지';
  return props.destination.name;
});
</script>

<template>
  <div
    class="inline-flex w-max flex-col items-center pointer-events-auto cursor-pointer transform -translate-x-1/2 -translate-y-full select-none"
    :title="displayName"
  >
    <div
      class="flex w-max items-center gap-1.5 whitespace-nowrap rounded-full border border-blue-400 bg-blue-600 px-2.5 py-2 text-xs font-bold text-white shadow-xl transition-all hover:bg-blue-700"
    >
      <span class="inline-block shrink-0 animate-bounce">🚩</span>
      <span class="shrink-0">{{ displayName }}</span>
    </div>
    <div class="-mt-1.5 h-2.5 w-2.5 rotate-45 bg-blue-600"></div>
    <div class="mt-1 h-2 w-6 rounded-full bg-black/20 blur-sm"></div>
  </div>
</template>
