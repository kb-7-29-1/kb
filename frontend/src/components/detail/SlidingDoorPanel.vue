<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { formatPropertyPriceDetail } from '@/utils/priceFormatter';
import WalkingTime from '@/components/property/WalkingTime.vue';
import CommentSection from '@/components/detail/CommentSection.vue';
import { useAuthStore } from '@/stores/useAuthStore.js';
import api from '@/api/api.js';

const authStore = useAuthStore();

const age = computed(() => {
  const birthDate = authStore.user?.birthDate;
  if (!birthDate) return null;
  return new Date().getFullYear() - new Date(birthDate).getFullYear() + 1;
});

const loanList = ref([]);
const loanListLoading = ref(false);
const isLoanOpen = ref(false);

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false,
  },
  property: {
    type: Object,
    default: null,
  },
  amenities: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['close', 'toggle-bookmark']);

const detailScrollRef = ref(null);
const detailSessionKey = ref(0);

const resetDetailView = async () => {
  isLoanOpen.value = false;
  detailSessionKey.value += 1;
  await nextTick();
  detailScrollRef.value?.scrollTo({ top: 0 });
};

watch(
  () => props.isOpen,
  (isOpen, wasOpen) => {
    if (!isOpen || wasOpen) return;
    resetDetailView();
  },
);

// 가격 포맷팅
const formattedPrice = computed(() => {
  if (!props.property) return '';
  return formatPropertyPriceDetail(props.property.deposit, props.property.monthlyRent);
});

const hasSafetyScore = computed(() => {
  const value = props.property?.safetyScore;
  return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value));
});

// 안전점수 색상
const safetyScoreClass = computed(() => {
  if (!hasSafetyScore.value) return 'bg-slate-100 text-slate-500';
  const score = Number(props.property.safetyScore);
  if (score >= 80) return 'bg-emerald-500/10 text-emerald-600';
  if (score >= 60) return 'bg-amber-500/10 text-amber-600';
  return 'bg-rose-500/10 text-rose-600';
});

const safetyGradeLabel = computed(() => {
  if (!hasSafetyScore.value) return '계산되지 않음';
  if (props.property.safetyGrade === 'SAFE') return '안심';
  if (props.property.safetyGrade === 'WARNING') return '주의';
  return '위험';
});

const safetyGradeBadgeClass = computed(() => {
  if (!hasSafetyScore.value) return 'bg-slate-500/20 text-slate-300 border-slate-500/30';
  if (props.property.safetyGrade === 'SAFE') {
    return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
  }
  if (props.property.safetyGrade === 'WARNING') {
    return 'bg-amber-500/20 text-amber-400 border-amber-500/30';
  }
  return 'bg-rose-500/20 text-rose-400 border-rose-500/30';
});

const policeFacilityText = computed(() =>
  props.property?.hasPoliceStation ? '경로 300m 내 위치' : '경로 300m 내 없음',
);

const buildingAge = computed(() => {
  const builtYear = parseInt(props.property?.builtYear || '2022', 10);
  if (!Number.isInteger(builtYear) || builtYear <= 0) return null;
  return Math.max(new Date().getFullYear() - builtYear, 0);
});

const fetchLoanList = async () => {
  if (!props.property) return;
  loanListLoading.value = true;
  try {
    const response = await api.get('/loan/property-recommend', {
      params: {
        deposit: props.property.deposit,
        monthlyRent: props.property.monthlyRent,
        age: age.value,
      },
    });
    loanList.value = response.data;
  } catch (error) {
    loanList.value = [];
  } finally {
    loanListLoading.value = false;
  }
};

watch(
  () => props.property?.propertyId,
  (propertyId) => {
    if (!propertyId) return;
    resetDetailView();
    fetchLoanList();
  },
  { immediate: true },
);

const SAMPLE_PROPERTY_IMAGES = [
  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80',
];

const detailImageUrl = computed(() => {
  if (props.property?.thumbnailUrl) return props.property.thumbnailUrl;
  const idx = Math.abs(Number(props.property?.propertyId || 0)) % SAMPLE_PROPERTY_IMAGES.length;
  return SAMPLE_PROPERTY_IMAGES[idx];
});
</script>

<template>
  <div>
    <!-- Backdrop Overlay (패널 열렸을 때 오버레이) -->
    <div
      v-if="isOpen"
      class="property-detail-backdrop fixed inset-0 bg-slate-900/30 backdrop-blur-xs z-40 transition-opacity"
      @click="emit('close')"
    ></div>

    <!-- 420px Slide-Over Panel (슬림형 가로폭 조정) -->
    <aside
      class="property-detail-panel fixed top-0 right-0 bottom-0 w-full sm:w-[420px] overflow-x-hidden bg-white z-50 shadow-2xl border-l border-slate-200 flex flex-col transition-transform duration-300 ease-in-out"
      :class="[isOpen ? 'translate-x-0' : 'translate-x-full']"
    >
      <!-- 패널 상단 헤더 -->
      <div
        class="min-h-[68px] px-6 pt-4 pb-2.5 flex items-center justify-between gap-3 bg-white shrink-0"
      >
        <div v-if="property" class="min-w-0">
          <div class="mb-1.5 flex items-center gap-1.5">
            <span
              class="inline-flex items-center rounded-md bg-[#eef1ff] px-2 py-1 text-[11px] font-bold text-[#4767f7]"
            >
              {{ property.buildingType === 3 ? '오피스텔' : '빌라/연립' }}
            </span>
            <span
              class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[11px] font-bold"
              :class="safetyScoreClass"
            >
              <i class="fa-solid fa-shield-halved text-[10px]" aria-hidden="true"></i>
              {{ hasSafetyScore ? `${property.safetyScore}점` : '점수 없음' }}
            </span>
          </div>
          <p class="truncate text-[15px] font-bold text-slate-800">
            {{ property.address }}
          </p>
        </div>

        <h2 class="hidden font-bold text-lg text-slate-900 flex items-center gap-2">
          <span>🏠</span>
          <span>매물 상세 리포트</span>
        </h2>

        <div class="flex shrink-0 items-center">
          <!-- 찜 버튼 -->
          <button
            type="button"
            class="inline-flex h-9 w-9 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-[#dc4b5d]"
            @click="property && emit('toggle-bookmark', property.propertyId)"
          >
            <svg
              viewBox="0 0 24 24"
              class="h-5 w-5 transition-colors"
              :class="property?.isBookmarked ? 'fill-[#dc4b5d] text-[#dc4b5d]' : 'fill-none'"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path
                d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78Z"
              />
            </svg>
          </button>

          <!-- 닫기 버튼 -->
          <button
            type="button"
            class="inline-flex h-9 w-9 items-center justify-center rounded-full text-[19px] font-normal leading-none text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
            @click="emit('close')"
          >
            ✕
          </button>
        </div>
      </div>

      <!-- 패널 메인 스크롤 콘텐츠 -->
      <div
        v-if="property"
        ref="detailScrollRef"
        class="property-detail-scroll min-h-0 flex-1 overflow-x-hidden overflow-y-auto"
      >
        <div class="flex min-h-full flex-col gap-4 p-4 py-0">
          <!-- 매물 갤러리/대표 사진 -->
          <div
            class="relative h-60 rounded-2xl overflow-hidden bg-slate-100 border border-slate-200"
          >
            <img :src="detailImageUrl" :alt="property.title" class="w-full h-full object-cover" />
            <!-- 하드코딩된 사진 개수 뱃지 주석 처리 -->
            <!-- <div
              class="absolute bottom-3 right-3 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md text-white text-xs font-medium"
            >
              사진 1 / 5
            </div> -->
          </div>

          <!-- 가격 및 타이틀 -->
          <div>
            <h1
              class="mb-1 text-[21px] font-extrabold tracking-tight text-slate-800 sm:text-[22px]"
            >
              {{ formattedPrice }}
            </h1>
            <p class="text-[14px] font-medium text-slate-500">
              {{ property.area || 24.5 }}m² · {{ property.floor || 3 }}층
            </p>
          </div>

          <!-- 건물 안전 정보 -->
          <section class="border-t border-slate-200 pt-3">
            <h3 class="mb-3 flex items-center gap-1.5 text-[15px] font-bold text-slate-800">
              <span aria-hidden="true">🏢</span>
              건물 안전 정보
            </h3>
            <div class="grid grid-cols-2 gap-3">
              <div
                class="flex min-h-[110px] flex-col items-center justify-center rounded-xl border px-4 py-3 text-center md:min-h-[110px]"
                :class="
                  property.isIllegalBuilding
                    ? 'border-rose-200 bg-rose-50'
                    : 'border-emerald-200 bg-emerald-50'
                "
              >
                <i
                  class="fa-solid mb-1.5 text-[16px]"
                  :class="
                    property.isIllegalBuilding
                      ? 'fa-triangle-exclamation text-rose-500'
                      : 'fa-circle-check text-emerald-500'
                  "
                  aria-hidden="true"
                ></i>
                <p
                  class="text-sm font-bold"
                  :class="property.isIllegalBuilding ? 'text-rose-500' : 'text-emerald-600'"
                >
                  {{ property.isIllegalBuilding ? '위반 건물' : '적법 건물' }}
                </p>
                <p class="mt-0.5 text-[10px] text-slate-500">건축물대장 기준</p>
              </div>
              <div
                class="flex min-h-[104px] flex-col items-center justify-center rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-center md:min-h-[124px]"
              >
                <p class="text-[18px] font-extrabold leading-none text-slate-800">
                  {{ buildingAge === null ? '-' : `${buildingAge}년` }}
                </p>
                <p class="mt-1 text-[11px] font-medium text-slate-500">
                  {{ property.builtYear || '2022' }}년 준공
                </p>
                <p class="mt-0.5 text-[10px] font-bold text-emerald-500">
                  {{ buildingAge !== null && buildingAge <= 5 ? '신축' : '준신축' }}
                </p>
              </div>
            </div>
          </section>

          <!-- 🛡️ 안심 귀갓길 & 안전 지표 리포트 -->
          <section class="border-t border-slate-200 pt-3">
            <h3 class="mb-3 flex items-center gap-1.5 text-[15px] font-bold text-slate-800">
              <span aria-hidden="true">💡</span>
              귀갓길 안전 점수
            </h3>
            <div
              class="p-5 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 text-white shadow-xl space-y-4"
            >
              <div class="flex items-center justify-between border-b border-slate-700/80 pb-3">
                <div class="flex items-center gap-2">
                  <span class="text-xl">🛡️</span>
                  <h3 class="font-bold text-base">골목 귀갓길 안전 리포트</h3>
                </div>
                <span
                  class="px-2.5 py-0.5 rounded-full border text-xs font-bold"
                  :class="safetyGradeBadgeClass"
                >
                  {{ safetyGradeLabel }} 등급
                </span>
              </div>

              <div class="grid grid-cols-2 gap-3 pt-1">
                <div class="p-3 rounded-xl bg-white/5 border border-white/10">
                  <div class="text-xs text-slate-400 mb-1">경로 50m 내 CCTV</div>
                  <div class="text-lg font-bold text-emerald-400">
                    {{ property.cctvCount || 0 }}개
                  </div>
                </div>
                <div class="p-3 rounded-xl bg-white/5 border border-white/10">
                  <div class="text-xs text-slate-400 mb-1">경로 내 가로등/보안등</div>
                  <div class="text-lg font-bold text-amber-400">
                    {{ property.streetLampCount ?? property.streetlightCount ?? 0 }}개
                  </div>
                </div>
              </div>

              <div class="flex items-center justify-between text-xs text-slate-300 pt-1">
                <span
                  >건물 위반건축물 여부:
                  <strong class="text-white">{{
                    property.isIllegalBuilding ? '위반' : '정상 (미해당)'
                  }}</strong></span
                >
                <span
                  >경찰서/파출소:
                  <strong
                    :class="property.hasPoliceStation ? 'text-emerald-400' : 'text-rose-400'"
                    >{{ policeFacilityText }}</strong
                  ></span
                >
              </div>
            </div>
          </section>

          <WalkingTime
            :key="detailSessionKey"
            class="detail-section-flush detail-section-divider"
            :amenities="amenities"
          />
          <section class="finance-section border-t border-slate-200 pt-3">
            <div class="finance-section-header" @click="isLoanOpen = !isLoanOpen">
              <div class="finance-section-title">
                <span aria-hidden="true">🏦</span>
                추천 금융 상품
              </div>
              <button
                type="button"
                class="finance-toggle-button"
                :aria-expanded="isLoanOpen"
                aria-label="맞춤 금융 상품 펼치기"
                @click.stop="isLoanOpen = !isLoanOpen"
              >
                <svg class="finance-toggle-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path :d="isLoanOpen ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'" />
                </svg>
              </button>
            </div>

            <div v-show="isLoanOpen">
              <div v-if="loanListLoading" class="text-gray-400 text-sm text-center py-8">
                상품을 찾고 있어요...
              </div>
              <div v-else-if="loanList.length === 0" class="text-gray-400 text-sm text-center py-8">
                추천 가능한 대출 상품이 없습니다.
              </div>
              <div v-else class="loan-scroll-list overflow-y-auto space-y-3 pr-1">
                <div v-for="item in loanList" :key="item.productName" class="loan-item">
                  <span class="loan-bank-tag">{{ item.companyName }}</span>
                  <p class="loan-item__name">{{ item.productName }}</p>
                  <p class="loan-item__details">{{ item.rateInfo }} · {{ item.loanLimit }}</p>
                </div>
              </div>
            </div>
          </section>
          <section class="detail-community-section">
            <CommentSection
              class="detail-section-flush"
              :property-id="property.propertyId"
              :property="property"
            />
          </section>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.property-detail-backdrop,
.property-detail-panel {
  top: var(--app-header-height, 56px);
}

:deep(.detail-section-divider) {
  border-top: 1px solid #e2e8f0;
  padding-top: 12px !important;
}

:deep(.walking-time.detail-section-flush) {
  padding-left: 0 !important;
  padding-right: 0 !important;
  padding-bottom: 0 !important;
}

.detail-community-section {
  width: calc(100% + 24px);
  margin: 0 -12px;
  padding: 14px 12px 16px;
  border-top: 1px solid #e2e8f0;
  background: #f5f7fb;
}

@media (min-width: 768px) {
  .property-detail-backdrop {
    display: none;
  }

  .detail-community-section {
    margin-top: auto;
  }

  .property-detail-scroll {
    scrollbar-gutter: stable both-edges;
    scrollbar-width: thin;
    scrollbar-color: #d7deea transparent;
  }

  .property-detail-scroll::-webkit-scrollbar {
    width: 5px;
  }

  .property-detail-scroll::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: #d7deea;
  }
}

:deep(.comment-section.detail-section-flush) {
  border-top: 0 !important;
}

.finance-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  cursor: pointer;
}

.finance-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.finance-section-title > span:last-child {
  line-height: 1.2;
}

.finance-section-title > span:first-child {
  font-size: 16px;
}

.finance-toggle-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #222;
  cursor: pointer;
}

.finance-toggle-icon {
  display: block;
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.5;
}

.loan-scroll-list {
  max-height: 210px;
  padding: 2px;
}

.loan-item {
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid #e3e9f5;
  border-radius: 14px;
  background: #f7f9fe;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.loan-item:hover {
  transform: translateY(-1px);
  border-color: #c7d2f5;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.06);
}

.loan-bank-tag {
  display: inline-flex;
  align-items: center;
  min-height: 19px;
  padding: 0 6px;
  border-radius: 5px;
  background: #eef1ff;
  color: #4767f7;
  font-size: 10px;
  font-weight: 700;
}

.loan-item__name {
  margin: 5px 0 0;
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.loan-item__details {
  margin: 4px 0 0;
  color: #8b95a7;
  font-size: 11px;
}
</style>
