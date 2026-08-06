<script setup>
import { computed } from 'vue';

const props = defineProps({
  amenity: {
    type: Object,
    required: true,
  },
  isExpanded: {
    type: Boolean,
    default: false,
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
  <div
    class="group inline-flex w-max flex-col items-center cursor-pointer transform -translate-x-1/2 -translate-y-full transition-transform duration-200 ease-out hover:-translate-y-[calc(100%+4px)]"
    :title="amenity.amenityName"
  >
    <div
      class="relative z-10 flex w-max min-w-10 items-center whitespace-nowrap rounded-full border border-violet-400 bg-white px-2.5 py-1.5 text-xs font-bold text-slate-800 shadow-lg transition-all duration-200 group-hover:bg-violet-50 group-hover:shadow-xl"
      :class="isExpanded ? 'gap-1.5 px-3.5' : 'gap-1.5'"
    >
      <span class="shrink-0 text-violet-600">{{ icon }}</span>
      <span class="shrink-0">{{ amenity.amenityName }}</span>
      <span
        v-if="isExpanded && walkingInfo"
        class="amenity-detail shrink-0 rounded-md bg-violet-100 px-1.5 py-0.5 text-[10px] font-bold text-violet-700"
      >
        {{ walkingInfo }}
      </span>
    </div>
    <div class="relative z-0 -mt-1.5 h-2.5 w-2.5 rotate-45 bg-violet-500"></div>
    <div class="w-6 h-2 bg-black/20 rounded-full blur-sm mt-1"></div>
  </div>
</template>

<style scoped>
.amenity-detail {
  animation: amenity-detail-in 180ms ease-out;
}

@keyframes amenity-detail-in {
  from {
    opacity: 0;
    transform: translateX(-4px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
