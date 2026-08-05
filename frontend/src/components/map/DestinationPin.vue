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
    class="flex flex-col items-center pointer-events-auto cursor-pointer transform -translate-x-1/2 -translate-y-full select-none"
    :title="displayName"
  >
    <div
      class="px-3.5 py-1.5 rounded-full bg-blue-600 text-white font-bold text-xs shadow-xl flex items-center gap-1.5 border border-blue-400 hover:bg-blue-700 transition-all"
    >
      <span class="inline-block animate-bounce">🚩</span>
      <span>{{ displayName }}</span>
    </div>
    <div class="w-3 h-3 bg-blue-600 rotate-45 -mt-1.5"></div>
    <div class="w-8 h-3 bg-black/20 rounded-full blur-xs mt-1"></div>
  </div>
</template>
