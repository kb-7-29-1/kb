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
    <!-- Row 1: 🛡️ 안전 점수 -->
    <div class="flex items-center gap-1">
      <span
        class="text-emerald-400 font-extrabold flex items-center gap-0.5 text-[12px]"
      >
        <i class="fa-solid fa-shield-halved text-[9px]"></i>
        {{ safetyScore }}점
      </span>
    </div>
    <!-- Row 2: 건물 이모지 + 전월세 가격 -->
    <div class="text-[11px] font-black text-white whitespace-nowrap mt-0.5">
      {{ buildingIcon }} {{ priceText }}
    </div>
    <!-- Row 3: 맨 아래 🏢 동일 건물 실거래 건수 배지 (dealCount > 1 일 때만 표출) -->
    <div
      v-if="property.dealCount && property.dealCount > 1"
      class="mt-0.5 text-[9px] px-1.5 py-0.2 rounded-full bg-blue-500/30 text-blue-300 border border-blue-400/40 font-black tracking-tight"
    >
      + 최근 3개월 {{ property.dealCount }}건 더 거래됐어요
    </div>
  </div>
</template>
