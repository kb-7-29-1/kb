<script setup>
import { computed } from 'vue';
import { formatPropertyPrice } from '@/utils/priceFormatter';

const props = defineProps({
  property: {
    type: Object,
    required: true,
  },
  isSelected: {
    type: Boolean,
    default: false,
  },
});

const buildingIcon = computed(() => {
  if (props.isSelected) return '📍';
  const bType = props.property.buildingType;
  if (bType === 3) return '🏢';
  if (bType === 1) return '🏡';
  return '🏠';
});

const roomText = computed(() => {
  return props.property.roomType === 2 ? '투룸' : '원룸';
});

const priceText = computed(() => {
  return formatPropertyPrice(
    props.property.deposit,
    props.property.monthlyRent,
  );
});
</script>

<template>
  <div
    class="px-2.5 py-1.5 rounded-2xl text-xs font-black shadow-lg border transition-all cursor-pointer flex items-center gap-1.5 transform -translate-x-1/2 -translate-y-full select-none"
    :class="[
      isSelected
        ? 'bg-blue-600 text-white ring-4 ring-blue-500/30 border-blue-700 scale-110 z-30'
        : 'bg-slate-900 text-white hover:bg-blue-600 border-slate-700 z-10',
    ]"
  >
    <span>{{ buildingIcon }}</span>
    <span>{{ priceText }}</span>
  </div>
</template>
