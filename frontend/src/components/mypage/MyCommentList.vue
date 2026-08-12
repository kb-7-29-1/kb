<script setup>
import { onMounted, ref } from 'vue';
import api from '@/api/api.js';

const comments = ref([]);
const isLoading = ref(true);

const fetchMyComments = async () => {
  try {
    const { data } = await api.get('/comments/me');
    comments.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error('MY COMMENTS GET ERROR:', error);
  } finally {
    isLoading.value = false;
  }
};

onMounted(fetchMyComments);
</script>

<template>
  <section class="my-comment-list" aria-labelledby="my-comments-title">
    <div class="my-comment-list__heading">
      <h2 id="my-comments-title">댓글 모아보기</h2>
      <span v-if="!isLoading">{{ comments.length }}</span>
    </div>

    <div v-if="isLoading" class="my-comment-list__loading">댓글을 불러오는 중이에요</div>
    <p v-else-if="!comments.length" class="my-comment-list__empty">
      작성한 댓글이 없습니다.
    </p>
    <div v-else class="my-comment-list__scroll">
      <div
        v-for="comment in comments"
        :key="comment.commentId"
        class="my-comment-list__item"
      >
        <span class="my-comment-list__property">{{ comment.propertyAddress || '매물 보기' }}</span>
        <span class="my-comment-list__content">{{ comment.content }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>

.my-comment-list {
  box-sizing: border-box;
  min-width: 0;
  padding: 18px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 16px rgb(15 23 42 / 4%);
}

.my-comment-list__heading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
}

.my-comment-list h2 {
  overflow: hidden;
  margin: 0;
  color: #17191d;
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-comment-list__heading span { color: #4767f7; font-size: 14px; font-weight: 700; }
.my-comment-list__loading, .my-comment-list__empty { margin: 0; padding: 22px 0; color: #94a3b8; font-size: 12px; text-align: center; }

.my-comment-list__item {
  display: grid;
  width: 100%;
  gap: 4px;
  padding: 9px 0;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.my-comment-list__scroll {
  max-height: 296px;
  overflow-y: auto;
}

.my-comment-list__item:last-of-type { border-bottom: 0; }
.my-comment-list__property { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.my-comment-list__property { color: #64748b; font-size: 11px; font-weight: 700; }
.my-comment-list__content { color: #334155; font-size: 12px; line-height: 1.55; overflow-wrap: anywhere; }
@media (hover: hover) and (pointer: fine) {
  .my-comment-list__item:hover .my-comment-list__content { color: #4767f7; }
}
</style>
