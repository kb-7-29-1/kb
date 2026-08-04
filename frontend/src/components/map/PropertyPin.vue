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

const depositNum = computed(() =>
  props.property.deposit ? Math.round(props.property.deposit / 1000) : 0,
);

const priceText = computed(() => {
  if (props.property.monthlyRent) {
    return `${depositNum.value}천/${props.property.monthlyRent}`;
  }
  return `전세 ${depositNum.value}천`;
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
    <span>{{ isSelected ? '📍' : '🏠' }}</span>
    <span>{{ priceText }}</span>
    <span
      class="text-[10px] px-1 py-0.2 rounded border font-bold"
      :class="
        property.dataSource === 'DB'
          ? 'bg-emerald-500/30 text-emerald-300 border-emerald-400/40'
          : 'bg-indigo-500/30 text-indigo-300 border-indigo-400/40'
      "
    >
      {{ property.dataSource === 'DB' ? 'DB' : '공공' }}
    </span>
  </div>
</template>
