<script setup>
import { computed } from 'vue';

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

const emit = defineEmits(['click']);

// 가격 표시용 (예: 1천/65)
const priceLabel = computed(() => {
  const deposit = props.property.deposit ? Math.round(props.property.deposit / 1000) : 0;
  if (!props.property.monthlyRent || props.property.monthlyRent === 0) {
    return `전세 ${deposit}천`;
  }
  return `${deposit}천/${props.property.monthlyRent}`;
});

// 안전 등급 배경 색상
const badgeColorClass = computed(() => {
  if (props.isSelected) return 'bg-blue-600 text-white ring-4 ring-blue-500/30 border-blue-700';
  const score = props.property.safetyScore || 0;
  if (score >= 80) return 'bg-slate-900 text-white hover:bg-emerald-600 border-slate-700';
  if (score >= 60) return 'bg-slate-900 text-white hover:bg-amber-600 border-slate-700';
  return 'bg-slate-900 text-white hover:bg-rose-600 border-slate-700';
});
</script>

<template>
  <button
    type="button"
    class="relative group transition-all duration-200 focus:outline-none"
    :class="[isSelected ? 'z-30 scale-110' : 'z-10 hover:z-20 hover:scale-105']"
    @click="emit('click', property)"
  >
    <!-- 마커 핀 배지 -->
    <div
      class="px-2.5 py-1.5 rounded-full font-bold text-xs shadow-lg border flex items-center gap-1 transition-colors"
      :class="badgeColorClass"
    >
      <span class="w-2 h-2 rounded-full" :class="[
        (property.safetyScore || 85) >= 80 ? 'bg-emerald-400' :
        (property.safetyScore || 85) >= 60 ? 'bg-amber-400' : 'bg-rose-400'
      ]"></span>
      <span>{{ priceLabel }}</span>
    </div>

    <!-- 핀 밑부분 삼각형 꼬리 -->
    <div
      class="w-2 h-2 mx-auto -mt-1 rotate-45 border-r border-b transition-colors"
      :class="[isSelected ? 'bg-blue-600 border-blue-700' : 'bg-slate-900 border-slate-700']"
    ></div>
  </button>
</template>
