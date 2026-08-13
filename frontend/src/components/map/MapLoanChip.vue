<script setup>
import { computed } from 'vue';
import { getLoanChipInfo } from '@/utils/loanChip.js';

const props = defineProps({
  property: {
    type: Object,
    required: true,
  },
  selectedLoanId: {
    type: String,
    default: null,
  },
});

const info = computed(() => getLoanChipInfo(props.property, props.selectedLoanId));
</script>

<template>
  <div
    class="flex items-center gap-1 rounded-full border border-blue-200/90 bg-blue-50/95 px-1.5 py-0.5 text-[9px] font-bold text-blue-700 shadow-sm transition-all hover:bg-blue-600 hover:text-white hover:border-blue-600 group/chip cursor-pointer"
  >
    <span class="rounded bg-blue-200/80 px-1 py-0.2 text-[7px] font-black text-blue-900 group-hover/chip:bg-white/30 group-hover/chip:text-white shrink-0">AD</span>
    <span class="inline-block transition-transform duration-300 group-hover/chip:-translate-y-0.5 text-[10px] shrink-0">{{ info.icon }}</span>
    <span class="whitespace-nowrap tracking-tighter font-extrabold text-[9px]">{{ info.displayText }}</span>
    <span class="whitespace-nowrap tracking-tighter text-[7px] text-blue-500/80 group-hover/chip:text-blue-100 shrink-0 font-medium">※참고용</span>
  </div>
</template>
