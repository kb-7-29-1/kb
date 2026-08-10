<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
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

const isDeletedCommentError = computed(
    () => submitError.value === '이미 삭제된 댓글입니다.',
);
const showDeletedCommentNotice = ref(false);
let deletedNoticeTimer;

watch(isDeletedCommentError, (isDeleted) => {
  if (deletedNoticeTimer) clearTimeout(deletedNoticeTimer);
  showDeletedCommentNotice.value = isDeleted;

  if (isDeleted) {
    deletedNoticeTimer = setTimeout(() => {
      showDeletedCommentNotice.value = false;
    }, 2000); // 사용자가 인지하기 쉽게 2초 유지
  }
});

onBeforeUnmount(() => {
  if (deletedNoticeTimer) clearTimeout(deletedNoticeTimer);
});

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
  <!-- 사용자 원본 유지 영역 -->
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

  <Transition name="slide-panel">
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

      <p v-if="submitError && !isDeletedCommentError" class="comment-submit-error" role="status">
        {{ submitError }}
      </p>

      <!-- 모던한 하단 토스트 팝업으로 디자인 변경 -->
      <Transition name="toast">
        <div v-if="showDeletedCommentNotice" class="deleted-comment-notice" role="status">
          <span v-if="isDeletedCommentError" class="deleted-comment-icon" aria-hidden="true">!</span>
          <span>{{ submitError }}</span>
        </div>
      </Transition>
    </aside>
  </Transition>
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

.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: transform 0.35s cubic-bezier(0.2, 0.8, 0.2, 1), opacity 0.35s ease;
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  transform: translateX(100%);
  opacity: 0;
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

.deleted-comment-notice {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 110;
  display: flex;
  align-items: center;
  gap: 8px;
  width: max-content;
  max-width: calc(100% - 40px);
  padding: 12px 20px;
  border-radius: 30px;
  background: rgba(15, 23, 42, 0.85);
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(4px);
  pointer-events: none;
}

.deleted-comment-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ef4444;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.toast-enter-from {
  opacity: 0;
  transform: translate(-50%, 20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}
</style>
