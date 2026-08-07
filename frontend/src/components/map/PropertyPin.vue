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

const safetyScore = computed(() => {
  const value = props.property.safetyScore;
  if (value === null || value === undefined || value === '') return null;
  const score = Number(value);
  return Number.isFinite(score) ? score : null;
});

const safetyPinTheme = computed(() => {
  if (safetyScore.value === null) {
    return {
      border: 'border-slate-400',
      badge: 'bg-slate-100 text-slate-500',
      background: 'bg-slate-500',
      pointer: 'bg-slate-500',
    };
  }

  if (safetyScore.value >= 80) {
    return {
      border: 'border-emerald-500',
      badge: 'bg-emerald-500/10 text-emerald-600',
      background: 'bg-emerald-500',
      pointer: 'bg-emerald-500',
    };
  }

  if (safetyScore.value >= 60) {
    return {
      border: 'border-amber-500',
      badge: 'bg-amber-500/10 text-amber-600',
      background: 'bg-amber-500',
      pointer: 'bg-amber-500',
    };
  }

  return {
    border: 'border-rose-500',
    badge: 'bg-rose-500/10 text-rose-600',
    background: 'bg-rose-500',
    pointer: 'bg-rose-500',
  };
});

const priceText = computed(() => {
  const deposit = Number(props.property.deposit || 0);
  const rent = Number(props.property.monthlyRent || 0);
  const depositText =
    deposit >= 10000
      ? `${(Math.floor((deposit / 10000) * 10) / 10).toFixed(1).replace(/\.0$/, '')}억`
      : `${deposit.toLocaleString()}만`;

  return rent === 0 ? `전세 ${depositText}` : `${depositText}/${rent}`;
});
</script>

<template>
  <div
    class="inline-flex w-max -translate-x-1/2 -translate-y-full flex-col items-center cursor-pointer select-none transform"
  >
    <div
      class="flex w-max items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-1.5 text-xs font-bold shadow-lg transition-all"
      :class="[
        safetyPinTheme.border,
        isSelected
          ? `${safetyPinTheme.background} text-white z-30`
          : 'bg-white text-slate-800 hover:-translate-y-0.5 z-10',
      ]"
    >
      <span class="shrink-0">{{ priceText }}</span>
      <span
        class="shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-bold"
        :class="isSelected ? 'bg-white/20 text-white' : safetyPinTheme.badge"
      >
        {{ safetyScore === null ? '--' : `${safetyScore}점` }}
      </span>
    </div>
    <div class="-mt-1.5 h-2.5 w-2.5 rotate-45" :class="safetyPinTheme.pointer"></div>
    <div class="mt-1 h-2 w-6 rounded-full bg-black/20 blur-sm"></div>
  </div>
</template>
