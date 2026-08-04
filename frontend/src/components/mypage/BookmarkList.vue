<script setup>
import api from "@/api/api.js";
import {onMounted, ref} from "vue";

const bookmarks = ref([]);
const router = useRouter();

const fetchBookmarks = async () => {
  const response = await api.get('/bookmark');
  bookmarks.value = response.data;
};

const removeBookmark = async (propertyId) => {
  await api.delete(`/bookmark/${propertyId}`);
  await fetchBookmarks();
};

const openPropertyDetail = (property) => {
  sessionStorage.setItem('selectedBookmarkProperty', JSON.stringify(property));

  router.push({
    name: 'home',
    query: { propertyId: String(property.propertyId) },
  });
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
      관심 매물 <span>{{ bookmarks.length }}</span>
    </h2>

    <div v-if="bookmarks.length === 0" class="text-gray-400 text-sm text-center py-8">
      찜한 매물이 없습니다.
    </div>
    <div v-else class="bookmark-scroll-list overflow-y-auto space-y-3 pr-1">
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
          <p class="bookmark-item__details">보증금 {{ item.deposit }}만 · {{ item.area }}㎡</p>
        </div>

        <div class="bookmark-item__actions">
          <span class="safety-badge" :class="safetyBadgeClass(item.safetyScore)">
            {{ item.safetyScore }}점
          </span>
          <button
            type="button"
            class="bookmark-remove-button"
            aria-label="관심 매물에서 삭제"
            @click.stop="removeBookmark(item.propertyId)"
          >
            <i class="fa-solid fa-heart" aria-hidden="true"></i>
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
  border: 0;
  border-radius: 50%;
  background: #fff1f2;
  color: #dc4b5d;
  font-size: 11px;
  cursor: pointer;
}
</style>
