<script setup>
import api from '@/api/api.js';
import { onMounted, ref } from 'vue';
import { formatDeposit } from '@/utils/priceFormatter.js';

const bookmarks = ref([]);
const isLoading = ref(true);
const pendingBookmarkIds = ref(new Set());
const emit = defineEmits(['open-property']);

const fetchBookmarks = async () => {
  try {
    const response = await api.get('/bookmark');
    // 화면에 처음 들어왔을 때의 목록만 서버 기준으로 구성
    bookmarks.value = (response.data || []).map((bookmark) => ({
      ...bookmark,
      isBookmarked: true,
    }));
  } catch (error) {
    console.error('BOOKMARK GET ERROR: ', error);
  } finally {
    isLoading.value = false;
  }
};

const toggleBookmark = async (item) => {
  const propertyId = item.propertyId;
  if (pendingBookmarkIds.value.has(propertyId)) return;

  pendingBookmarkIds.value.add(propertyId);
  try {
    await api.delete(`/bookmark/${propertyId}`);
    bookmarks.value = bookmarks.value.filter((b) => b.propertyId !== propertyId);
  } catch (error) {
    console.error('BOOKMARK TOGGLE ERROR: ', error);
  } finally {
    pendingBookmarkIds.value.delete(propertyId);
  }
};

const openPropertyDetail = (property) => {
  emit('open-property', property);
};

const safetyBadgeClass = (score) => {
  const value = Number(score);
  if (value >= 80) return 'safety-badge--safe';
  if (value >= 60) return 'safety-badge--caution';
  return 'safety-badge--warning';
};

onMounted(fetchBookmarks);
</script>
<template>
  <div class="bookmark-list">
    <h2 class="bookmark-title">
      관심 매물 <span v-if="!isLoading">{{ bookmarks.length }}</span>
    </h2>

    <div v-if="isLoading" class="bookmark-skeleton-list" aria-busy="true">
      <div v-for="index in 3" :key="index" class="bookmark-skeleton-item">
        <div>
          <span class="bookmark-skeleton-block bookmark-skeleton-tag"></span>
          <span class="bookmark-skeleton-block bookmark-skeleton-address"></span>
          <span class="bookmark-skeleton-block bookmark-skeleton-detail"></span>
        </div>
        <span class="bookmark-skeleton-block bookmark-skeleton-action"></span>
      </div>
    </div>
    <div v-else-if="bookmarks.length === 0" class="text-gray-400 text-sm text-center py-8">
      찜한 매물이 없습니다.
    </div>
    <div
      v-else
      class="bookmark-scroll-list bookmark-scroll-list--loaded overflow-y-auto space-y-3 pr-1"
    >
      <div
        v-for="item in bookmarks"
        :key="item.propertyId"
        class="bookmark-item"
        role="button"
        tabindex="0"
        @click="openPropertyDetail(item)"
        @keydown.enter="openPropertyDetail(item)"
      >
        <div class="bookmark-item__content">
          <div class="bookmark-item__tags">
            <span class="property-type-tag">{{ item.buildingTypeTag }}</span>
            <span v-if="item.isIllegalBuilding" class="warning-tag">⚠️ 위반 건축물</span>
          </div>
          <p class="bookmark-item__address">{{ item.address }}</p>
          <p class="bookmark-item__details">
            {{ item.monthlyRent > 0 ? '보증금' : '전세' }} {{ formatDeposit(item.deposit) }}
            <template v-if="item.monthlyRent > 0"> · 월세 {{ item.monthlyRent }}만</template>
            · {{ item.area }}㎡
          </p>
        </div>

        <div class="bookmark-item__actions">
          <span class="safety-badge" :class="safetyBadgeClass(item.safetyScore)">
            {{ item.safetyScore }}점
          </span>
          <button
            type="button"
            class="bookmark-remove-button"
            :class="{ 'bookmark-remove-button--active': item.isBookmarked !== false }"
            :aria-label="item.isBookmarked !== false ? '관심 매물 해제' : '관심 매물 등록'"
            :disabled="pendingBookmarkIds.has(item.propertyId)"
            @click.stop="toggleBookmark(item)"
          >
            <i
              class="fa-heart"
              :class="item.isBookmarked !== false ? 'fa-solid' : 'fa-regular'"
              aria-hidden="true"
            ></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bookmark-list {
  box-sizing: border-box;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.bookmark-title {
  margin: 0 0 16px;
  color: #17191d;
  font-size: 15px;
  font-weight: 700;
}

.bookmark-title span {
  color: #4767f7;
}

.bookmark-scroll-list {
  max-height: 296px;
  padding: 2px;
}

.bookmark-scroll-list--loaded {
  animation: bookmark-content-reveal 0.24s ease-out;
}

.bookmark-skeleton-list {
  display: grid;
  gap: 12px;
}

.bookmark-skeleton-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 82px;
  padding: 12px;
  border: 1px solid #e3e9f5;
  border-radius: 14px;
  background: #f7f9fe;
}

.bookmark-skeleton-item > div {
  display: grid;
  gap: 7px;
}

.bookmark-skeleton-block {
  display: block;
  border-radius: 999px;
  background: linear-gradient(90deg, #eaf0f8 25%, #f7f9fc 45%, #eaf0f8 65%);
  background-size: 220% 100%;
  animation: bookmark-skeleton-shimmer 1.25s ease-in-out infinite;
}

.bookmark-skeleton-tag {
  width: 42px;
  height: 16px;
}
.bookmark-skeleton-address {
  width: 154px;
  height: 13px;
}
.bookmark-skeleton-detail {
  width: 96px;
  height: 11px;
}
.bookmark-skeleton-action {
  width: 24px;
  height: 24px;
}

@keyframes bookmark-skeleton-shimmer {
  to {
    background-position: -120% 0;
  }
}

@keyframes bookmark-content-reveal {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.bookmark-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e3e9f5;
  border-radius: 14px;
  background: #f7f9fe;
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

/* 모바일 탭 후 hover 상태가 남아 아이콘 테두리가 변하지 않도록 PC 마우스에서만 적용 */
.bookmark-item:hover {
  border-color: #cbd6ff;
  background: #f0f4ff;
  box-shadow: 0 5px 12px rgb(71 103 247 / 8%);
  transform: translateY(-1px);
}

.bookmark-item__content {
  min-width: 0;
}

.bookmark-item__tags {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 5px;
}

.property-type-tag,
.warning-tag {
  display: inline-flex;
  align-items: center;
  min-height: 19px;
  padding: 0 6px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
}

.property-type-tag {
  background: #eef1ff;
  color: #4767f7;
}

.warning-tag {
  background: #fff7ed;
  color: #d97706;
}

.bookmark-item__address {
  overflow: hidden;
  margin: 0;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bookmark-item__details {
  margin: 4px 0 0;
  color: #8b95a7;
  font-size: 11px;
  font-weight: 500;
}

.bookmark-item__actions {
  display: flex;
  flex: 0 0 auto;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.safety-badge {
  display: inline-flex;
  align-items: center;
  min-height: 21px;
  padding: 0 6px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
}

.safety-badge--safe {
  background: #ecfdf5;
  color: #16a34a;
}

.safety-badge--caution {
  background: #fff1df;
  color: #d97706;
}

.safety-badge--warning {
  background: #fff1f2;
  color: #dc4b5d;
}

.bookmark-remove-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 1px solid #cbd5e1;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  font-size: 11px;
  cursor: pointer;
  transition:
    color 0.15s ease,
    opacity 0.15s ease,
    border-color 0.15s ease,
    background-color 0.15s ease,
    transform 0.15s ease;
}

@media (min-width: 768px) and (hover: hover) and (pointer: fine) {
  .bookmark-remove-button:hover {
    border-color: #dc4b5d;
    color: #dc4b5d;
    transform: scale(1.06);
  }
}

.bookmark-remove-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bookmark-remove-button--active {
  border-color: transparent;
  background: #fff1f2;
  color: #dc4b5d;
}
</style>
