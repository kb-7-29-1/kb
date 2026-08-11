<script setup>
import { computed, ref } from 'vue';
import TagBadge from './TagBadge.vue';

const props = defineProps({
  property: {
    type: Object,
    default: () => ({
      propertyId: null,
      propertyName: '',
      address: '',
    }),
  },
  comments: {
    type: Array,
    default: () => [],
  },
  isLoggedIn: {
    type: Boolean,
    default: false,
  },
  isSubmitting: {
    type: Boolean,
    default: false,
  },
  tags: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits([
  'close',
  'submit-comment',
  'update-comment',
  'delete-comment',
]);

const content = ref('');
const editingCommentId = ref(null);
const editingContent = ref('');

const propertyAddress = computed(() => {
  return (
      props.property?.roadAddress ||
      props.property?.address ||
      props.property?.jibunAddress ||
      ''
  );
});

const propertyName = computed(
  () => props.property?.title || props.property?.propertyName || props.property?.name || '매물 정보',
);

const formatDateTime = (dateTime) => {
  if (!dateTime) return '';

  const matched = String(dateTime).match(/(\d{4})-(\d{2})-(\d{2})/);
  return matched ? `${matched[1]}.${matched[2]}.${matched[3]}` : '';
};

const submitComment = () => {
  const trimmedContent = content.value.trim();

  if (!trimmedContent) {
    alert('실거주 후기 및 의견을 입력해주세요.');
    return;
  }

  emit('submit-comment', {
    propertyId: props.property?.propertyId,
    content: trimmedContent,
  });

  content.value = '';
};

const startEdit = (comment) => {
  editingCommentId.value = comment.commentId;
  editingContent.value = comment.content;
};

const cancelEdit = () => {
  editingCommentId.value = null;
  editingContent.value = '';
};

const submitEdit = (commentId) => {
  const trimmedContent = editingContent.value.trim();
  if (!trimmedContent) return;

  emit('update-comment', { commentId, content: trimmedContent });
  cancelEdit();
};
</script>

<template>
  <section class="resident-report">
    <header class="report-header">
      <button
          type="button"
          class="back-button"
          aria-label="뒤로 가기"
          @click="emit('close')"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="back-icon">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </button>

      <div class="property-information">
        <div class="property-title-row">
          <h2 class="property-name">
            💬 실거주 리포트
          </h2>
        </div>

        <p class="property-address">
          {{ propertyAddress }}
        </p>
      </div>
    </header>

    <section v-if="tags.length" class="tag-summary">
      <TagBadge :tags="tags" :show-all="true" />
    </section>

    <!-- 댓글 목록 -->
    <section class="comment-list">

      <!-- 빈 상태(Empty State) 디자인 -->
      <div v-if="!comments.length" class="empty-comment">
        <div class="empty-comment-icon">💬</div>
        <strong>아직 등록된 댓글이 없어요</strong>
        <p>이 매물의 첫 번째 의견을 남겨보세요.</p>
      </div>

      <article
          v-for="comment in comments"
          :key="comment.commentId"
          class="comment-card"
      >
        <div class="comment-header">
          <div class="nickname-wrapper">
            <strong class="comment-nickname"
            >👤 {{ comment.nickname || '익명 사용자' }}</strong
            >
            <span v-if="comment.isMine" class="my-badge">MY</span>
          </div>
          <div class="comment-meta">
            <span class="created-at">{{ formatDateTime(comment.createdAt) }}</span>
            <template v-if="comment.isMine">
              <span class="action-separator" aria-hidden="true">|</span>
              <div class="comment-actions">
                <button
                    type="button"
                    class="edit-button"
                    @click="startEdit(comment)"
                >
                  수정
                </button>
                <button
                    type="button"
                    class="delete-button"
                    @click="emit('delete-comment', comment.commentId)"
                >
                  삭제
                </button>
              </div>
            </template>
          </div>
        </div>
        <template v-if="editingCommentId === comment.commentId">
          <textarea
              v-model="editingContent"
              class="inline-edit-input"
              maxlength="255"
          ></textarea>
          <div class="inline-edit-actions">
            <button
                type="button"
                class="cancel-edit-button"
                @click="cancelEdit"
            >
              취소
            </button>
            <button
                type="button"
                class="save-edit-button"
                :disabled="isSubmitting || !editingContent.trim()"
                @click="submitEdit(comment.commentId)"
            >
              저장
            </button>
          </div>
        </template>
        <p v-else>{{ comment.content }}</p>
      </article>
    </section>

    <!-- 실거주 댓글 작성 -->
    <form
        v-if="isLoggedIn"
        class="comment-form"
        @submit.prevent="submitComment"
    >
      <div class="form-title">
        <span>✏️</span>
        <strong>실거주 댓글 작성</strong>
      </div>

      <div class="input-row">
        <textarea
            v-model="content"
            class="comment-input"
            rows="1"
            maxlength="255"
            placeholder="실거주 후기 및 의견을 입력하세요..."
            @keydown.enter.exact.prevent="submitComment"
        />

        <button
            type="submit"
            class="submit-button"
            :disabled="!content.trim()"
        >
          등록
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.resident-report {
  display: flex;
  flex: 1;
  width: 100%;
  min-height: 0;
  overflow: hidden;
  flex-direction: column;
  box-sizing: border-box;
  background: #f7f8fa;
}

/* ✨ 무거운 그라데이션 제거 & 모던 화이트 헤더 적용 ✨ */
.report-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  min-height: 82px;
  align-items: center;
  gap: 12px;
  padding: 14px 22px;
  border-bottom: 1px solid rgb(255 255 255 / 12%);
  background: linear-gradient(135deg, #17213d 0%, #11182d 100%);
  backdrop-filter: blur(8px); /* 스크롤 시 뒤가 은은하게 비치는 효과 */
  box-shadow: 0 3px 12px rgb(15 23 42 / 18%);
}

/* 세련된 아이콘 버튼 스타일 */
.back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: rgb(255 255 255 / 8%);
  color: #d5dcf0;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.back-button:hover {
  background-color: rgb(255 255 255 / 15%);
}

.back-icon {
  width: 22px;
  height: 22px;
}

.property-information {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.property-label {
  color: #aab5cc;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.property-name {
  margin: 0;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.property-address {
  margin: 0;
  color: #aab5cc;
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* --- 타이틀이 제거된 태그 섹션 --- */
.tag-summary {
  padding: 16px 22px;
  border-bottom: 1px solid #f0f2f5;
  background: #fff;
}

/* --- 댓글 목록 및 기타 CSS --- */
.empty-comment {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.empty-comment-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  font-size: 28px;
  animation: float 2.5s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.empty-comment strong {
  color: #333d4b;
  font-size: 16px;
  font-weight: 800;
}

.empty-comment p {
  margin: 8px 0 0;
  color: #8b95a1;
  font-size: 14px;
  line-height: 1.5;
}

.comment-list {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  padding: 18px 22px 20px;
  overflow-y: auto;
}

.comment-card {
  padding: 16px;
  border: 1px solid #e2e6ed;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 7px rgb(15 23 42 / 5%);
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-nickname {
  min-width: 0;
  overflow: hidden;
  color: #263244;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nickname-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.my-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 6px;
  border: 1.5px solid #5b89ff;
  border-radius: 6px;
  background: #e8f0ff;
  color: #12379d;
  font-size: 10px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-weight: 900;
  line-height: 1;
}

.comment-meta {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 7px;
}

.created-at {
  color: #98a2b3;
  font-size: 11px;
}

.action-separator {
  color: #d0d5dd;
  font-size: 11px;
}

.comment-card > p {
  margin: 14px 0 0;
  color: #4b5565;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.inline-edit-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 88px;
  margin-top: 14px;
  padding: 11px;
  border: 1px solid #9eabf8;
  border-radius: 9px;
  outline: none;
  color: #374151;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
}

.inline-edit-input:focus {
  box-shadow: 0 0 0 3px rgb(64 86 214 / 12%);
}

.inline-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 9px;
}

.inline-edit-actions button {
  min-width: 48px;
  padding: 7px 10px;
  border: 0;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.cancel-edit-button {
  background: #eef1f5;
  color: #586174;
}

.save-edit-button {
  background: #4056d6;
  color: #fff;
}

.save-edit-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.comment-actions {
  display: flex;
  flex-shrink: 0;
  gap: 5px;
}

.comment-actions button {
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.edit-button {
  color: #4056d6;
}

.edit-button:hover {
  text-decoration: underline;
}

.delete-button {
  color: #d64857;
}

.delete-button:hover {
  text-decoration: underline;
}

.comment-form {
  margin-top: auto;
  padding: 15px 22px 20px;
  border-top: 1px solid #dfe3e8;
  background: #fff;
  box-shadow: 0 -4px 12px rgb(17 24 39 / 5%);
}

.form-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 11px;
  color: #454d5d;
  font-size: 13px;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.comment-input {
  min-height: 48px;
  max-height: 120px;
  flex: 1;
  resize: vertical;
  padding: 13px 15px;
  border: 1px solid #dce1e9;
  border-radius: 12px;
  outline: none;
  background: #fafbfc;
  color: #303744;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.5;
}

.comment-input::placeholder {
  color: #b0b6c1;
}

.comment-input:focus {
  border-color: #293a8c;
  background: #fff;
  box-shadow: 0 0 0 3px rgb(41 58 140 / 10%);
}

.submit-button {
  width: 74px;
  height: 48px;
  flex-shrink: 0;
  border: 0;
  border-radius: 12px;
  background: #293a8c;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
}

.submit-button:hover:not(:disabled) {
  background: #1e2c70;
}

.submit-button:disabled {
  background: #cbd0dc;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .report-header {
    padding: 12px 16px;
  }
  .property-name {
    font-size: 16px;
  }
  .tag-summary {
    padding: 14px 18px;
  }
  .comment-form {
    padding: 14px 18px 18px;
  }
  .submit-button {
    width: 68px;
  }
}
</style>
