<script setup>
import { computed, ref, watch } from 'vue';
import CommentList from '@/components/property/CommentList.vue';
import { usePropertyComments } from '@/composables/usePropertyComments.js';
import { useAuthStore } from '@/stores/useAuthStore';
import TagBadge from '@/components/property/TagBadge.vue';

const props = defineProps({
  propertyId: {
    type: [Number, String],
    required: true,
  },
  property: {
    type: Object,
    required: true,
  },
});

const showCommentList = ref(false);
const authStore = useAuthStore();
const isLoggedIn = computed(() => authStore.isLoggedIn);
const {
  comments,
  tags,
  commentCount,
  isLoading,
  loadError,
  isSubmitting,
  submitError,
  load,
  create,
  update,
  remove,
} = usePropertyComments();

const openCommentList = async () => {
  showCommentList.value = true;
  await load(props.propertyId);
};

const createComment = async ({ content }) => {
  if (!isLoggedIn.value || !content?.trim()) return;
  await create(props.propertyId, content.trim());
};

const updateComment = async ({ commentId, content }) => {
  if (!isLoggedIn.value || !content?.trim()) return;
  await update(props.propertyId, commentId, content.trim());
};

const deleteComment = async (commentId) => {
  if (!isLoggedIn.value || !window.confirm('댓글을 삭제할까요?')) return;
  await remove(props.propertyId, commentId);
};

watch(
  () => props.propertyId,
  async (propertyId) => {
    showCommentList.value = false;
    if (propertyId) await load(propertyId);
  },
  { immediate: true },
);
</script>

<template>
  <section class="comment-section" v-bind="$attrs">
    <div class="section-header">
      <div class="section-title">
        <span class="title-icon">💬</span>
        <span>실거주 커뮤니티</span>
      </div>

      <button type="button" class="view-all-button" @click="openCommentList">
        댓글 {{ commentCount }}개 전체보기
        <span class="arrow">›</span>
      </button>
    </div>

    <TagBadge :tags="tags" @show-more="openCommentList" />

    <div class="notice">
      <span class="notice-icon" aria-hidden="true">⚠</span>
      <p>
        본 키워드는 인터넷 커뮤니티 게시물을 바탕으로 자동 분석된 결과이며, 당사는 사실 여부를
        보증하지 않습니다. (단순 참고용)
      </p>
    </div>
  </section>

  <aside v-if="showCommentList" class="comment-list-panel">
    <p v-if="isLoading" class="comment-status">댓글을 불러오는 중입니다.</p>
    <p v-else-if="loadError" class="comment-status error">{{ loadError }}</p>
    <CommentList
      v-else
      :property="property"
      :comments="comments"
      :tags="tags"
      :is-logged-in="isLoggedIn"
      :is-submitting="isSubmitting"
      @close="showCommentList = false"
      @submit-comment="createComment"
      @update-comment="updateComment"
      @delete-comment="deleteComment"
    />
    <p v-if="submitError" class="comment-submit-error">{{ submitError }}</p>
  </aside>
</template>

<style scoped>
.comment-section {
  width: 100%;
  padding: 14px 0 16px;
  border: 0;
  background: transparent;
  box-sizing: border-box;
}

.comment-section.detail-section-flush {
  width: 100%;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
}

.comment-list-panel {
  position: absolute;
  inset: 0;
  z-index: 60;
  display: flex;
  width: 100%;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
  box-shadow: -12px 0 30px rgb(15 23 42 / 18%);
  box-sizing: border-box;
}

.comment-status,
.comment-submit-error {
  margin: 0;
  padding: 48px 18px;
  color: #6b7280;
  text-align: center;
}

.comment-status.error,
.comment-submit-error {
  color: #dc2626;
}

.comment-submit-error {
  padding: 8px 18px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.title-icon {
  font-size: 15px;
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
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
}

.arrow {
  font-size: 16px;
  line-height: 1;
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
