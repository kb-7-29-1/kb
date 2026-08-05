<script setup>
import AmenityWalkingTimeFilter from './AmenityWalkingTimeFilter.vue';

defineProps({
  amenities: {
    type: Array,
    default: () => [],
  },
});

defineEmits(['apply', 'close', 'reset', 'update-time-limit']);
</script>

<template>
  <div
    class="absolute left-0 top-full z-40 mt-2 w-full rounded-2xl border border-slate-200 bg-white p-4 shadow-xl xl:left-full xl:top-0 xl:ml-5 xl:mt-0 xl:w-80"
  >
    <div class="mb-0 flex items-center justify-between border-b border-slate-200 pb-3">
      <h3 class="text-sm font-black text-slate-800">상세 필터</h3>
      <div class="flex items-center gap-1">
        <button
          v-if="amenities.length"
          type="button"
          class="flex h-10 items-center gap-1 rounded-md px-2 text-xs font-bold text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-800"
          @click="$emit('reset')"
        >
          <i class="fa-solid fa-rotate-left text-[10px]" aria-hidden="true"></i>
          초기화
        </button>
        <button
          type="button"
          class="flex h-10 w-10 items-center justify-center rounded-full text-[20px] text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
          aria-label="상세 필터 닫기"
          @click="$emit('close')"
        >
          ×
        </button>
      </div>
    </div>

    <p v-if="!amenities.length" class="pt-4 text-xs text-slate-500">
      먼저 편의시설을 선택해 주세요.
    </p>
    <div v-else class="space-y-4">
      <AmenityWalkingTimeFilter
        :amenities="amenities"
        @update-time-limit="$emit('update-time-limit', $event)"
      />
      <button
        type="button"
        class="mt-4 w-full rounded-xl bg-blue-600 py-3 text-sm font-black text-white shadow-md transition-all hover:bg-blue-700"
        @click="$emit('apply')"
      >
        적용하기
      </button>
    </div>
  </div>
</template>
