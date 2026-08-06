<script setup>
import { onMounted, ref } from 'vue';
import { getProfile, updateProfile } from '@/api/authService.js';

const profile = ref(null);
const isLoading = ref(true);
const isEditing = ref(false);

const editForm = ref({ name: '', email: '' });

const updateError = ref('');

const fetchProfile = async () => {
  try {
    const response = await getProfile();
    profile.value = response.data;
    editForm.value.name = response.data.name;
    editForm.value.email = response.data.email;
  } catch (error) {
    console.error('PROFILE GET ERROR: ', error);
  } finally {
    isLoading.value = false;
  }
};

onMounted(fetchProfile);

const startEdit = () => {
  isEditing.value = true;
  updateError.value = '';
};

const cancelEdit = () => {
  isEditing.value = false;
  editForm.value.name = profile.value.name;
  editForm.value.email = profile.value.email;
};

const handleUpdateProfile = async () => {
  updateError.value = '';
  try {
    await updateProfile({
      name: editForm.value.name,
      email: editForm.value.email,
    });
    await fetchProfile();
    isEditing.value = false;
  } catch (error) {
    updateError.value = '프로필 수정 중 오류가 발생했습니다.';
  }
};
</script>
<template>
  <div v-if="isLoading" class="profile-card profile-card--skeleton" aria-busy="true">
    <div class="profile-summary">
      <span class="skeleton-block skeleton-avatar"></span>
      <div class="skeleton-profile-copy">
        <span class="skeleton-block skeleton-name"></span>
        <span class="skeleton-block skeleton-email"></span>
      </div>
      <span class="skeleton-block skeleton-link"></span>
    </div>
  </div>

  <div v-else-if="profile" class="profile-card profile-card--loaded">
    <div class="profile-summary">
      <div
        class="w-12 h-12 rounded-full bg-indigo-600 text-white flex items-center justify-center font-bold"
      >
        {{ profile.name?.charAt(0) }}
      </div>
      <div>
        <p class="font-bold">{{ profile.name }}</p>
        <p class="text-sm text-gray-500">{{ profile.email }}</p>
      </div>
      <button type="button" class="profile-edit-link" @click="startEdit">
        정보 수정
        <i class="fa-solid fa-chevron-right" aria-hidden="true"></i>
      </button>
    </div>

    <!-- 정보 수정 모달 -->
    <Transition name="profile-modal">
      <div v-if="isEditing" class="profile-modal-overlay">
        <div
          class="profile-edit-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="profile-edit-title"
        >
          <div class="profile-modal-heading">
            <h3 id="profile-edit-title">
              <span class="profile-modal-icon" aria-hidden="true">
                <i class="fa-regular fa-user"></i>
              </span>
              프로필 정보 수정
            </h3>
            <button type="button" class="profile-modal-close" aria-label="닫기" @click="cancelEdit">
              <i class="fa-solid fa-xmark" aria-hidden="true"></i>
            </button>
          </div>

          <p class="profile-modal-description">변경할 이름과 이메일을 입력해 주세요.</p>

          <label class="profile-edit-label">이름</label>
          <input
            v-model="editForm.name"
            type="text"
            class="profile-edit-input"
            placeholder="변경할 이름을 입력해 주세요"
          />

          <label class="profile-edit-label">이메일</label>
          <input
            v-model="editForm.email"
            type="email"
            class="profile-edit-input"
            placeholder="example@email.com"
          />

          <p v-if="updateError" class="profile-edit-error">{{ updateError }}</p>

          <div class="profile-modal-actions">
            <button type="button" class="profile-cancel-button" @click="cancelEdit">취소</button>
            <button type="button" class="profile-save-button" @click="handleUpdateProfile">
              저장하기
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.profile-card {
  box-sizing: border-box;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.profile-card--loaded {
  animation: profile-content-reveal 0.24s ease-out;
}

.skeleton-block {
  display: block;
  border-radius: 999px;
  background: linear-gradient(90deg, #edf1f7 25%, #f7f9fc 45%, #edf1f7 65%);
  background-size: 220% 100%;
  animation: skeleton-shimmer 1.25s ease-in-out infinite;
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
}

.skeleton-profile-copy {
  display: grid;
  flex: 1;
  gap: 8px;
}

.skeleton-name {
  width: 70px;
  height: 14px;
}

.skeleton-email {
  width: 130px;
  height: 11px;
}

.skeleton-link {
  width: 42px;
  height: 11px;
}

@keyframes skeleton-shimmer {
  to {
    background-position: -120% 0;
  }
}

@keyframes profile-content-reveal {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.profile-edit-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #4767f7;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  margin-left: auto;
}

.profile-edit-link i {
  font-size: 10px;
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgb(15 23 42 / 40%);
}

.profile-modal-enter-active,
.profile-modal-leave-active {
  transition: opacity 0.2s ease;
}

.profile-modal-enter-active .profile-edit-modal,
.profile-modal-leave-active .profile-edit-modal {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.profile-modal-enter-from,
.profile-modal-leave-to {
  opacity: 0;
}

.profile-modal-enter-from .profile-edit-modal,
.profile-modal-leave-to .profile-edit-modal {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

.profile-edit-modal {
  box-sizing: border-box;
  width: 100%;
  max-width: 400px;
  padding: 28px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 20px 48px rgb(15 23 42 / 22%);
}

.profile-modal-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: 18px;
}

.profile-modal-heading h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #20283a;
  font-size: 16px;
  font-weight: 700;
}

.profile-modal-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eef1ff;
  color: #4058f5;
  font-size: 13px;
}

.profile-modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #9aa3b0;
  font-size: 14px;
  cursor: pointer;
}

.profile-modal-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.profile-modal-description {
  margin: 0 0 18px;
  color: #7b8797;
  font-size: 13px;
}

.profile-edit-label {
  display: block;
  margin: 0 0 6px;
  color: #697386;
  font-size: 13px;
  font-weight: 600;
}

.profile-edit-input {
  box-sizing: border-box;
  width: 100%;
  height: 48px;
  margin: 0 0 14px;
  padding: 0 14px;
  border: 1px solid #e0e5f5;
  border-radius: 12px;
  outline: none;
  color: #20283a;
  font: inherit;
  font-size: 14px;
}

.profile-edit-input:focus {
  border-color: #4058f5;
  box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
}

.profile-edit-error {
  margin: -2px 0 14px;
  color: #e05263;
  font-size: 12px;
}

.profile-modal-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.profile-modal-actions button {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.profile-cancel-button {
  border: 1px solid #e0e5f5;
  background: #fff;
  color: #697386;
}

.profile-save-button {
  border: 1px solid #4051db;
  background: #4051db;
  color: #fff;
}
</style>
