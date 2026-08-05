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
    class="absolute left-0 top-full z-50 mt-2 w-full rounded-xl border border-slate-200 bg-white p-4 shadow-xl xl:left-full xl:top-0 xl:ml-3 xl:mt-0 xl:w-80"
  >
    <div class="mb-3 flex items-center justify-between">
      <h3 class="text-sm font-black text-slate-900">상세 필터</h3>
      <div class="flex items-center gap-1">
        <button
          v-if="amenities.length"
          type="button"
          class="rounded-md px-2 py-1 text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800"
          @click="$emit('reset')"
        >
          초기화
        </button>
        <button
          type="button"
          class="h-7 w-7 rounded-full text-slate-500 hover:bg-slate-100 hover:text-slate-800"
          aria-label="상세 필터 닫기"
          @click="$emit('close')"
        >
          ×
        </button>
      </div>
    </div>

    <p v-if="!amenities.length" class="text-xs text-slate-500">
      먼저 아래에서 편의시설을 선택해 주세요.
    </p>
    <div v-else class="space-y-3">
      <AmenityWalkingTimeFilter
        :amenities="amenities"
        @update-time-limit="$emit('update-time-limit', $event)"
      />
      <button
        type="button"
        class="w-full rounded-lg bg-blue-600 py-2 text-xs font-bold text-white hover:bg-blue-700"
        @click="$emit('apply')"
      >
        상세 조건 적용
      </button>
    </div>
  </div>
</template>
