<script setup>
import { onMounted, ref } from 'vue';
import { getProfile, updateProfile } from '@/api/authService.js';

const profile = ref(null);
const isEditing = ref(false);
const isChangingPassword = ref(false);

const editForm = ref({ name: '', email: '' });
const passwordForm = ref({ currentPassword: '', newPassword: '', newPasswordConfirm: '' });

const updateError = ref('');
const passwordError = ref('');

const fetchProfile = async () => {
  const response = await getProfile();
  profile.value = response.data;
  editForm.value.name = response.data.name;
  editForm.value.email = response.data.email;
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
  <div v-if="profile" class="profile-card">
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
    <div
      v-if="isChangingPassword"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
    >
      <div class="bg-white rounded-2xl w-full max-w-md p-6">
        <h3 class="font-bold text-lg mb-4">비밀번호 변경</h3>

        <label class="block text-sm text-gray-600 mb-1">현재 비밀번호</label>
        <input
          v-model="passwordForm.currentPassword"
          type="password"
          class="w-full border rounded-lg px-4 py-3 mb-3"
          placeholder="현재 비밀번호를 입력해 주세요"
        />

        <label class="block text-sm text-gray-600 mb-1">새 비밀번호</label>
        <input
          v-model="passwordForm.newPassword"
          type="password"
          class="w-full border rounded-lg px-4 py-3 mb-3"
          placeholder="새 비밀번호를 입력해 주세요"
        />

        <label class="block text-sm text-gray-600 mb-1">새 비밀번호 재확인</label>
        <input
          v-model="passwordForm.newPasswordConfirm"
          type="password"
          class="w-full border rounded-lg px-4 py-3 mb-4"
          placeholder="새 비밀번호를 다시 입력해 주세요"
        />

        <p v-if="passwordError" class="text-red-500 text-sm mb-3">{{ passwordError }}</p>

        <div class="flex gap-2">
          <button @click="isChangingPassword = false" class="flex-1 border rounded-lg py-3">
            취소
          </button>
          <button
            @click="handleChangePassword"
            class="flex-1 bg-indigo-600 text-white rounded-lg py-3"
          >
            변경 완료
          </button>
        </div>
      </div>
    </div>
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
