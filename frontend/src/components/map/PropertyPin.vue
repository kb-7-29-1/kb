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

const safetyScore = computed(() => {
  return props.property.safetyScore || 85;
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
    class="px-2.5 py-1 rounded-xl text-xs font-black shadow-2xl border transition-all cursor-pointer flex flex-col items-center justify-center transform -translate-x-1/2 -translate-y-full select-none leading-tight"
    :class="[
      isSelected
        ? 'bg-blue-600 text-white ring-4 ring-blue-500/30 border-blue-400 scale-110 z-30'
        : 'bg-slate-900/95 text-white hover:bg-blue-600 border-slate-700 backdrop-blur-md z-10',
    ]"
  >
    <!-- Row 1: 건물 이모지 + 🛡️ 안전 점수 -->
    <div class="flex items-center gap-1">
      <span class="text-xs">{{ buildingIcon }}</span>
      <span class="text-xs">{{ buildingIcon }}</span>

      <span
        class="text-emerald-400 font-extrabold flex items-center gap-0.5 text-[11px]"
      >
        <i class="fa-solid fa-shield-halved text-[9px]"></i>
        {{ safetyScore }}점
      </span>
    </div>
    <!-- Row 2: 전월세 가격 -->
    <div class="text-[11px] font-black text-white whitespace-nowrap mt-0.5">
      {{ priceText }}
    </div>
  </div>
</template>
