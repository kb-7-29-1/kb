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

// 화면에 보여줄 3개의 태그
const displayedTags = computed(() => {
  return props.tags.slice(0, MAX_DISPLAY);
});

// 나머지 숨겨진 태그 개수
const remainingCount = computed(() => {
  return Math.max(0, props.tags.length - MAX_DISPLAY);
});
</script>

<template>
  <div v-if="tags.length" class="keyword-list" aria-label="댓글 자동 분석 태그">
    <span
        v-for="tag in displayedTags"
        :key="tag.tagType"
        class="keyword-item"
        :class="tag.type === 'NEGATIVE' ? 'negative' : 'positive'"
    >
      {{ tag.type === 'NEGATIVE' ? '👎' : '👍' }}
      {{ tag.tagName }}
      <strong>{{ tag.count }}</strong>
    </span>

    <button
        v-if="remainingCount > 0"
        type="button"
        class="keyword-item more"
        @click="emit('show-more')"
    >
      +{{ remainingCount }}개 더보기
    </button>
  </div>
</template>

<style scoped>
.keyword-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  box-sizing: border-box;
}

.keyword-item.positive {
  border: 1px solid #6ee7b7;
  background: #f0fdf7;
  color: #24815f;
}

.keyword-item.negative {
  border: 1px solid #fecdd3;
  background: #fff1f2;
  color: #be123c;
}

/* 더보기 버튼 스타일 (기존에 정의해두신 스타일 적용) */
.keyword-item.more {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  transition: background-color 0.2s;
}

.keyword-item.more:hover {
  background: #f3f4f6;
}

.keyword-item strong {
  color: #4b5563;
  font-size: 12px;
}
</style>