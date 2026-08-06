<script setup>
import {computed, ref} from 'vue';

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

  emit('update-comment', {commentId, content: trimmedContent});
  cancelEdit();
};
</script>

<template>
  <section class="resident-report">
    <!-- 상단 매물 헤더 -->
    <header class="report-header">
      <button
          type="button"
          class="back-button"
          aria-label="뒤로 가기"
          @click="emit('close')"
      >
        ‹
      </button>

      <div class="property-information">
        <div class="property-title-row">
          <h2 class="property-name">
            💬실거주 리포트
          </h2>
        </div>

        <p class="property-address">
          {{ propertyAddress }}
        </p>
      </div>
    </header>

    <!-- 전체 태그 -->
    <section
        v-if="tags.length"
        class="tag-summary"
    >
      <h3 class="section-title">
        🏷️ 전체 빅데이터 추출 태그 ({{ tags.length }}종)
      </h3>

      <div class="tag-list">
        <button
            v-for="tag in tags"
            :key="tag.tagId || tag.id || tag.tagName"
            type="button"
            class="summary-tag"
            :class="{
            positive: tag.type === 'POSITIVE',
            negative: tag.type === 'NEGATIVE',
          }"
        >
          <span>
            {{ tag.type === 'NEGATIVE' ? '👎' : '👍' }}
          </span>

          <span>
            {{ tag.tagName || tag.name }}
          </span>

          <span>
            ({{ tag.count || 0 }})
          </span>
        </button>
      </div>
    </section>

    <!-- 댓글 목록 -->
    <section class="comment-list">
      <div v-if="!comments.length" class="empty-comment">
        <span class="empty-comment-icon">💬</span>
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
            <strong class="comment-nickname">👤 {{ comment.nickname || '익명 사용자' }}</strong>
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
            <button type="button" class="cancel-edit-button" @click="cancelEdit">취소</button>
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

/* 상단 헤더 */

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
  box-shadow: 0 3px 12px rgb(15 23 42 / 18%);
}

.back-button {
  display: flex;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: rgb(255 255 255 / 8%);
  color: #d5dcf0;
  font-size: 30px;
  font-weight: 300;
  line-height: 1;
  cursor: pointer;
}

.back-button:hover {
  background: rgb(255 255 255 / 15%);
}

.property-information {
  min-width: 0;
}

.property-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.property-icon {
  display: flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgb(189 183 255 / 16%);
  color: #d9d4ff;
  font-size: 12px;
}

.property-name {
  overflow: hidden;
  margin: 0;
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.property-address {
  overflow: hidden;
  margin: 5px 0 0;
  color: #aab5cc;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 태그 요약 */

.tag-summary {
  padding: 22px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.section-title {
  margin: 0 0 14px;
  color: #404757;
  font-size: 14px;
  font-weight: 800;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 10px;
  border: 1px solid #b8edcf;
  border-radius: 6px;
  background: #f2fff7;
  color: #238557;
  font-size: 12px;
  font-weight: 700;
}

.summary-tag.negative {
  border-color: #ffc7ce;
  background: #fff5f6;
  color: #dc4355;
}

/* 댓글 작성 폼 */

.empty-comment {
  display: flex;
  box-sizing: border-box;
  width: calc(100% - clamp(24px, 8vw, 44px));
  min-height: 180px;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 4px auto 20px;
  padding: 24px;
  border: 1px dashed #cfd6e3;
  border-radius: 14px;
  background: #fff;
  color: #667085;
  text-align: center;
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
  border: 1.5px solid #5B89FF;
  border-radius: 6px;
  background: #E8F0FF;
  color: #12379D;
  font-size: 10px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
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

.empty-comment-icon {
  display: flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  border-radius: 50%;
  background: #eef1ff;
  font-size: 20px;
}

.empty-comment strong {
  color: #374151;
  font-size: 14px;
}

.empty-comment p {
  margin: 6px 0 0;
  font-size: 12px;
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
  transition: background 0.2s,
  opacity 0.2s;
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
    min-height: 92px;
    padding: 17px 18px;
  }

  .property-name {
    font-size: 17px;
  }

  .tag-summary {
    padding: 20px 18px;
  }

  .comment-form {
    padding: 14px 18px 18px;
  }

  .submit-button {
    width: 68px;
  }
}
</style>
