<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { formatPropertyPriceDetail } from '@/utils/priceFormatter';
import WalkingTime from '@/components/property/WalkingTime.vue';
import CommentSection from '@/components/detail/CommentSection.vue';

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

watch(
  () => props.isOpen,
  async (isOpen, wasOpen) => {
    if (!isOpen || wasOpen) return;

    detailSessionKey.value += 1;
    await nextTick();

    if (detailScrollRef.value) {
      detailScrollRef.value.scrollTop = 0;
    }
  },
);

// 가격 포맷팅
const formattedPrice = computed(() => {
  if (!props.property) return '';
  return formatPropertyPriceDetail(
    props.property.deposit,
    props.property.monthlyRent,
  );
});

// 안전점수 색상
const safetyScoreClass = computed(() => {
  if (!props.property) return '';
  const score = props.property.safetyScore || 85;
  if (score >= 80) return 'bg-emerald-500/10 text-emerald-600';
  if (score >= 60) return 'bg-amber-500/10 text-amber-600';
  return 'bg-rose-500/10 text-rose-600';
});

const buildingAge = computed(() => {
  const builtYear = parseInt(props.property?.builtYear || '2022', 10);
  if (!Number.isInteger(builtYear) || builtYear <= 0) return null;
  return Math.max(new Date().getFullYear() - builtYear, 0);
});
</script>

<template>
  <div>
    <!-- Backdrop Overlay (패널 열렸을 때 오버레이) -->
    <!-- fixed inset-0 bg-slate-900/30 property-detail-backdrop-blur-xs -->
    <div
      v-if="isOpen"
      class="z-40 transition-opacity"
      @click="emit('close')"
    ></div>

    <!-- 560px Slide-Over Panel -->
    <aside
      class="property-detail-panel fixed top-0 right-0 bottom-0 w-full sm:w-[560px] bg-white z-50 shadow-2xl border-l border-slate-200 flex flex-col transition-transform duration-300 ease-in-out"
      :class="[isOpen ? 'translate-x-0' : 'translate-x-full']"
    >
      <!-- 패널 상단 헤더 -->
      <div
        class="min-h-[76px] px-5 py-5 flex items-center justify-between gap-3 bg-white shrink-0"
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
              <i
                class="fa-solid fa-shield-halved text-[10px]"
                aria-hidden="true"
              ></i>
              {{ property.safetyScore || 85 }}점
            </span>
          </div>
          <p class="truncate text-[15px] font-bold text-slate-800">
            {{ property.address }}
          </p>
        </div>

        <h2
          class="hidden font-bold text-lg text-slate-900 flex items-center gap-2"
        >
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
              :class="
                property?.isBookmarked
                  ? 'fill-[#dc4b5d] text-[#dc4b5d]'
                  : 'fill-none'
              "
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
        class="property-detail-scroll min-h-0 flex-1 overflow-y-auto"
      >
        <div class="flex min-h-full flex-col gap-6 p-6 py-0">
          <!-- 매물 갤러리/대표 사진 -->
          <div
            class="relative h-64 rounded-2xl overflow-hidden bg-slate-100 border border-slate-200"
          >
            <img
              :src="
                property.thumbnailUrl ||
                'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80'
              "
              :alt="property.title"
              class="w-full h-full object-cover"
            />
            <div
              class="absolute bottom-3 right-3 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md text-white text-xs font-medium"
            >
              사진 1 / 5
            </div>
          </div>

          <!-- 가격 및 타이틀 -->
          <div>
            <div class="hidden items-center gap-2 mb-1.5">
              <span
                class="px-2.5 py-1 rounded-md text-xs font-bold bg-blue-50 text-blue-600 border border-blue-200"
              >
                {{ property.buildingType === 3 ? '오피스텔' : '빌라/연립' }}
              </span>
              <span
                class="px-2.5 py-1 rounded-md text-xs font-bold border"
                :class="safetyScoreClass"
              >
                안전지수 {{ property.safetyScore || 85 }}점
              </span>
              <span
                v-if="property.dealCount && property.dealCount > 1"
                class="px-2.5 py-1 rounded-md text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200"
              >
                🏢 동일 건물 실거래 {{ property.dealCount }}건
              </span>
            </div>

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
          <section class="border-t border-slate-200 pt-5">
            <h3
              class="mb-3 flex items-center gap-1.5 text-[15px] font-bold text-slate-800"
            >
              <span aria-hidden="true">🏢</span>
              건물 안전 정보
            </h3>
            <div class="grid grid-cols-2 gap-3">
              <div
                class="flex min-h-[104px] flex-col items-center justify-center rounded-xl border px-4 py-3 text-center md:min-h-[124px]"
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
                  :class="
                    property.isIllegalBuilding
                      ? 'text-rose-500'
                      : 'text-emerald-600'
                  "
                >
                  {{ property.isIllegalBuilding ? '위반 건물' : '적법 건물' }}
                </p>
                <p class="mt-0.5 text-[10px] text-slate-500">건축물대장 기준</p>
              </div>
              <div
                class="flex min-h-[104px] flex-col items-center justify-center rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-center md:min-h-[124px]"
              >
                <p
                  class="text-[18px] font-extrabold leading-none text-slate-800"
                >
                  {{ buildingAge === null ? '-' : `${buildingAge}년` }}
                </p>
                <p class="mt-1 text-[11px] font-medium text-slate-500">
                  {{ property.builtYear || '2022' }}년 준공
                </p>
                <p class="mt-0.5 text-[10px] font-bold text-emerald-500">
                  {{
                    buildingAge !== null && buildingAge <= 5 ? '신축' : '준신축'
                  }}
                </p>
              </div>
            </div>
          </section>

          <!-- 🛡️ 안심 귀갓길 & 안전 지표 리포트 -->
          <section class="border-t border-slate-200 pt-5">
            <h3
              class="mb-3 flex items-center gap-1.5 text-[15px] font-bold text-slate-800"
            >
              <span aria-hidden="true">💡</span>
              귀갓길 안전 점수
            </h3>
            <div
              class="p-5 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 text-white shadow-xl space-y-4"
            >
              <div
                class="flex items-center justify-between border-b border-slate-700/80 pb-3"
              >
                <div class="flex items-center gap-2">
                  <span class="text-xl">🛡️</span>
                  <h3 class="font-bold text-base">골목 귀갓길 안전 리포트</h3>
                </div>
                <span
                  class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                >
                  안심 등급 🟢
                </span>
              </div>

              <div class="grid grid-cols-2 gap-3 pt-1">
                <div class="p-3 rounded-xl bg-white/5 border border-white/10">
                  <div class="text-xs text-slate-400 mb-1">500m 내 CCTV</div>
                  <div class="text-lg font-bold text-emerald-400">
                    {{ property.cctvCount || 0 }}개
                  </div>
                </div>
                <div class="p-3 rounded-xl bg-white/5 border border-white/10">
                  <div class="text-xs text-slate-400 mb-1">
                    경로 내 가로등/보안등
                  </div>
                  <div class="text-lg font-bold text-amber-400">
                    {{ property.streetlightCount || 0 }}개
                  </div>
                </div>
              </div>

              <div
                class="flex items-center justify-between text-xs text-slate-300 pt-1"
              >
                <span
                  >건물 위반건축물 여부:
                  <strong class="text-white">{{
                    property.isIllegalBuilding ? '위반' : '정상 (미해당)'
                  }}</strong></span
                >
                <span
                  >경찰서/파출소:
                  <strong class="text-emerald-400"
                    >도보 0분 내 위치</strong
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
          <section class="detail-community-section">
            <CommentSection
              class="detail-section-flush"
              :property-id="property.propertyId"
              :property="property"
            />
          </section>
        </div>
        <<<<<<< HEAD

        <!-- 가격 및 타이틀 -->
        <div>
          <div class="flex items-center gap-2 mb-1.5">
            <span
              class="px-2.5 py-1 rounded-md text-xs font-bold bg-blue-50 text-blue-600 border border-blue-200"
            >
              {{ property.buildingType === 3 ? '오피스텔' : '빌라/연립' }}
            </span>
            <span
              class="px-2.5 py-1 rounded-md text-xs font-bold border"
              :class="safetyScoreClass"
            >
              안전지수 {{ property.safetyScore || 85 }}점
            </span>
            <span
              v-if="property.dealCount && property.dealCount > 1"
              class="px-2.5 py-1 rounded-md text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200"
            >
              🏢 동일 건물 실거래 {{ property.dealCount }}건
            </span>
          </div>

          <h1 class="text-2xl font-black text-slate-900 mb-1">
            {{ formattedPrice }}
          </h1>
          <p class="text-sm text-slate-600 font-medium">
            {{ property.address }}
          </p>
        </div>

        <!-- 핵심 정보 스펙 요약 Grid -->
        <div
          class="grid grid-cols-3 gap-3 p-4 rounded-xl bg-slate-50 border border-slate-200 text-center"
        >
          <div>
            <div class="text-xs text-slate-500 mb-1">전용면적</div>
            <div class="font-bold text-slate-900 text-sm">
              {{ property.area || 24.5 }}m²
            </div>
          </div>
          <div class="border-x border-slate-200">
            <div class="text-xs text-slate-500 mb-1">층수</div>
            <div class="font-bold text-slate-900 text-sm">
              {{ property.floor || 3 }}층
            </div>
          </div>
          <div>
            <div class="text-xs text-slate-500 mb-1">준공연도</div>
            <div class="font-bold text-slate-900 text-sm">
              {{ property.builtYear || '2022년' }}
            </div>
          </div>
        </div>

        <!-- 🛡️ 안심 귀갓길 & 안전 지표 리포트 -->
        <div
          class="p-5 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 text-white shadow-xl space-y-4"
        >
          <div
            class="flex items-center justify-between border-b border-slate-700/80 pb-3"
          >
            <div class="flex items-center gap-2">
              <span class="text-xl">🛡️</span>
              <h3 class="font-bold text-base">골목 귀갓길 안전 리포트</h3>
            </div>
            <span
              class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
            >
              안심 등급 🟢
            </span>
          </div>

          <div class="grid grid-cols-2 gap-3 pt-1">
            <div class="p-3 rounded-xl bg-white/5 border border-white/10">
              <div class="text-xs text-slate-400 mb-1">500m 내 CCTV</div>
              <div class="text-lg font-bold text-emerald-400">
                {{ property.cctvCount || 0 }}개
              </div>
            </div>
            <div class="p-3 rounded-xl bg-white/5 border border-white/10">
              <div class="text-xs text-slate-400 mb-1">
                경로 내 가로등/보안등
              </div>
              <div class="text-lg font-bold text-amber-400">
                {{ property.streetlightCount || 0 }}개
              </div>
            </div>
          </div>

          <div
            class="flex items-center justify-between text-xs text-slate-300 pt-1"
          >
            <span
              >건물 위반건축물 여부:
              <strong class="text-white">{{
                property.isIllegalBuilding ? '위반' : '정상 (미해당)'
              }}</strong></span
            >
            <span
              >경찰서/파출소:
              <strong class="text-emerald-400">도보 0분 내 위치</strong></span
            >
          </div>
        </div>

        <!-- 상세 설명 -->
        <div>
          <h3 class="font-bold text-slate-900 mb-2 text-sm">
            매물 특징 & 안내
          </h3>
          <p
            class="text-xs text-slate-600 leading-relaxed bg-slate-50 p-3.5 rounded-xl border border-slate-200"
          >
            {{
              property.description ||
              '풀옵션(세탁기, 냉장고, 에어컨 포함), 채광 좋고 늦은 밤 귀갓길도 가로등과 CCTV가 촘촘하여 안심하고 거주할 수 있는 매물입니다.'
            }}
          </p>
        </div>
        <WalkingTime :amenities="amenities" />
        <CommentSection
          :property-id="property.propertyId"
          :property="property"
        />
        ======= >>>>>>> develop
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
  width: calc(100% + 48px);
  margin: 0 -24px;
  padding: 14px 24px 16px;
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
</style>
