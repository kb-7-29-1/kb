<script setup>
const props = defineProps({
  propertyId: {
    type: [Number, String],
    required: true,
  },
  commentCount: {
    type: Number,
    default: 0,
  },
});

const goToComments = () => {
  emit('view-all', props.propertyId);
};

const emit = defineEmits(['view-all']);
</script>

<template>
  <section class="comment-section">
    <div class="section-header">
      <div class="section-title">
        <span class="title-icon">💬</span>
        <span>실거주 커뮤니티</span>
      </div>

      <button
          type="button"
          class="view-all-button"
          @click="goToComments"
      >

        댓글 {{ commentCount }}개 전체보기
        <span class="arrow">›</span>
      </button>
    </div>

  </section>
</template>

<style scoped>
.comment-section {
  width: 100%;
  padding: 20px 18px;
  border-top: 10px solid #f5f6f8;
  background: #fff;
  box-sizing: border-box;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #1f2937;
  font-size: 16px;
  font-weight: 700;
}

.title-icon {
  font-size: 17px;
}

.bigdata-label {
  padding: 3px 7px;
  border-radius: 5px;
  background: #e8edff;
  color: #4f64ff;
  font-size: 11px;
  font-weight: 700;
}

.view-all-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #4f64ff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
}

.arrow {
  font-size: 20px;
  line-height: 1;
}

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

.keyword-item.more {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #6b7280;
}

.keyword-item strong {
  color: #4b5563;
  font-size: 12px;
}

.notice {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin-top: 14px;
  color: #9ca3af;
}

.notice-icon {
  flex-shrink: 0;
  font-size: 12px;
}

.notice p {
  margin: 0;
  font-size: 11px;
  line-height: 1.5;
  word-break: keep-all;
}
</style>
