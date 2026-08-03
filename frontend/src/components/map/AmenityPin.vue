<script setup>
import { computed } from 'vue';

const props = defineProps({
  amenity: {
    type: Object,
    required: true,
  },
});

const amenityIcons = {
  1: '🏪',
  2: '☕',
  3: '🧺',
  4: '🍔',
  5: '🛍️',
  6: '💄',
  7: '🛒',
};

const icon = computed(() => amenityIcons[props.amenity.amenityType] ?? '📍');
const walkingInfo = computed(() => {
  const minutes = props.amenity.walkTimeMinutes;
  const distance = props.amenity.distanceMeters;

  if (minutes == null && distance == null) return '';
  if (distance == null) return `도보 ${minutes}분`;
  if (minutes == null) return `${distance}m`;
  return `도보 ${minutes}분 · ${distance}m`;
});
</script>

<template>
  <div class="flex flex-col items-center cursor-default" :title="amenity.amenityName">
    <div class="min-w-10 px-2.5 py-1.5 rounded-full bg-white border border-emerald-500 shadow-lg text-xs font-bold text-slate-800 whitespace-nowrap">
      <span class="mr-1">{{ icon }}</span>
      <span>{{ amenity.amenityName }}</span>
      <span v-if="walkingInfo" class="ml-1 text-emerald-600">{{ walkingInfo }}</span>
    </div>
    <div class="w-2.5 h-2.5 bg-emerald-500 rotate-45 -mt-1.5"></div>
    <div class="w-6 h-2 bg-black/20 rounded-full blur-sm mt-1"></div>
  </div>
</template>
