<script setup>
import { computed } from 'vue';

const props = defineProps({
  tags: {
    type: Array,
    required: true,
    default: () => [],
  },
});

const emit = defineEmits(['show-more']);

// 표시할 태그 최대 개수
const MAX_DISPLAY = 2;

// 화면에 보여줄 태그
const displayedTags = computed(() => {
  return props.tags.slice(0, MAX_DISPLAY);
});

// 나머지 숨겨진 태그 개수
const remainingCount = computed(() => {
  return Math.max(0, props.tags.length - MAX_DISPLAY);
});
</script>

<template>
  <div v-if="tags.length" class="tag-list" aria-label="댓글 자동 분석 태그">
    <!-- 상세 리포트와 완전히 동일한 클래스와 구조 사용 -->
    <div
        v-for="tag in displayedTags"
        :key="tag.tagId || tag.tagName"
        class="summary-tag"
        :class="tag.type === 'NEGATIVE' ? 'negative' : 'positive'"
    >
      <span class="tag-icon">
        {{ tag.type === 'NEGATIVE' ? '👎' : '👍' }}
      </span>
      <span class="tag-name">{{ tag.tagName }}</span>
      <span class="tag-count-badge">{{ tag.count || 0 }}</span>
    </div>

    <!-- 더보기 버튼 -->
    <button
        v-if="remainingCount > 0"
        type="button"
        class="summary-tag more"
        @click="emit('show-more')"
    >
      +{{ remainingCount }} 더보기
    </button>
  </div>
</template>

<style scoped>
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 모던한 알약(Pill) 디자인 칩 (상세화면과 100% 통일) */
.summary-tag {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px 7px 10px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: default;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 0; /* 버튼일 경우 기본 테두리 제거 */
}

.summary-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.tag-icon {
  font-size: 14px;
  margin-right: 6px;
}

.tag-name {
  margin-right: 6px;
}

/* 카운트 숫자 뱃지 */
.tag-count-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
}

/* 긍정 태그 (세련된 파란색 계열) */
.summary-tag.positive {
  background: #f0f5ff;
  color: #3182f6;
}
.summary-tag.positive .tag-count-badge {
  background: #dbe4ff;
  color: #1c7ed6;
}

/* 부정 태그 (부드러운 빨간색 계열) */
.summary-tag.negative {
  background: #fff4f4;
  color: #f04452;
}
.summary-tag.negative .tag-count-badge {
  background: #ffe3e4;
  color: #e03131;
}

/* 더보기 버튼 전용 스타일 */
.summary-tag.more {
  padding: 7px 14px;
  background: #f2f4f6;
  color: #505967;
  cursor: pointer;
}

.summary-tag.more:hover {
  background: #e5e8eb;
}
</style>