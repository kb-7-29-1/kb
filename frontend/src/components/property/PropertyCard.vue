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

const emit = defineEmits(['select', 'toggle-bookmark']);

// 건물 종류 텍스트 변환
const buildingTypeText = computed(() => {
  const types = { 1: '빌라/연립', 2: '다가구', 3: '오피스텔' };
  return types[props.property.buildingType] || props.property.propertyType || '주택';
});

// 방 종류 텍스트 변환
const roomTypeText = computed(() => {
  const rooms = { 1: '원룸', 2: '투룸' };
  return rooms[props.property.roomType] || '원룸';
});

// 가격 포맷팅 (예: 보증금 1,000 / 월세 65)
const formattedPrice = computed(() => {
  const deposit = props.property.deposit ? props.property.deposit.toLocaleString() : '0';
  if (!props.property.monthlyRent || props.property.monthlyRent === 0) {
    return `전세 ${deposit}만`;
  }
  return `${deposit} / ${props.property.monthlyRent}만`;
});

// 안전점수 색상 클래스
const safetyScoreClass = computed(() => {
  const score = props.property.safetyScore || 0;
  if (score >= 80) return 'bg-emerald-500/10 text-emerald-600 border-emerald-300';
  if (score >= 60) return 'bg-amber-500/10 text-amber-600 border-amber-300';
  return 'bg-rose-500/10 text-rose-600 border-rose-300';
});

const safetyGradeText = computed(() => {
  const score = props.property.safetyScore || 0;
  if (score >= 80) return '안심 🟢';
  if (score >= 60) return '보통 🟡';
  return '주의 🔴';
});
</script>

<template>
  <div
    class="group relative flex gap-3 p-3.5 rounded-xl bg-white border transition-all duration-200 cursor-pointer hover:shadow-md hover:-translate-y-0.5"
    :class="[
      isSelected
        ? 'border-blue-600 ring-2 ring-blue-500/20 bg-blue-50/30'
        : 'border-slate-200 hover:border-slate-300',
    ]"
    @click="emit('select', property)"
  >
    <!-- 매물 썸네일 -->
    <div class="relative w-28 h-28 rounded-lg overflow-hidden bg-slate-100 shrink-0">
      <img
        :src="property.thumbnailUrl || 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=400&q=80'"
        :alt="property.title"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
      />
      <!-- 찜 버튼 -->
      <button
        type="button"
        class="absolute top-1.5 right-1.5 w-7 h-7 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center text-white hover:bg-black/60 transition-colors"
        @click.stop="emit('toggle-bookmark', property.propertyId)"
      >
        <span class="text-xs" :class="{ 'text-rose-500': property.isBookmarked }">
          {{ property.isBookmarked ? '❤️' : '🤍' }}
        </span>
      </button>
    </div>

    <!-- 매물 정보 -->
    <div class="flex-1 flex flex-col justify-between min-w-0">
      <div>
        <div class="flex items-center gap-1.5 mb-1">
          <span class="px-1.5 py-0.5 rounded text-[11px] font-semibold bg-slate-100 text-slate-600">
            {{ buildingTypeText }} · {{ roomTypeText }}
          </span>
          <span
            class="px-1.5 py-0.5 rounded text-[11px] font-semibold border"
            :class="safetyScoreClass"
          >
            안전 {{ property.safetyScore || 85 }}점 ({{ safetyGradeText }})
          </span>
        </div>

        <h3 class="font-bold text-slate-900 text-base group-hover:text-blue-600 transition-colors truncate">
          {{ formattedPrice }}
        </h3>
        
        <p class="text-xs text-slate-500 truncate mt-0.5">
          {{ property.title || property.address }}
        </p>
      </div>

      <div class="flex items-center justify-between text-[11px] text-slate-500 mt-2">
        <span>{{ property.floor ? `${property.floor}층` : '3층' }} · {{ property.area ? `${property.area}m²` : '24.5m²' }}</span>
        <span class="text-slate-400 truncate max-w-[120px]">{{ property.address }}</span>
      </div>
    </div>
  </div>
</template>
