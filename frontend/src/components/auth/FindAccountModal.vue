<script setup>
import { findId, resetPassword, verifyForPasswordReset } from '@/api/authService.js';
import { ref } from 'vue';

const emit = defineEmits(['close']);

const tab = ref('findId');

// 아이디 찾기
const email = ref('');
const maskedLoginId = ref('');
const findIdError = ref('');

const handleFindId = async () => {
  findIdError.value = '';
  maskedLoginId.value = '';
  try {
    const response = await findId(email.value);
    maskedLoginId.value = response.data.maskedLoginId;
  } catch (error) {
    findIdError.value = '해당 이메일로 가입된 계정이 없습니다.';
  }
};

// 비밀번호 찾기
const pwLoginId = ref('');
const pwName = ref('');
const pwEmail = ref('');
const verifiedUserId = ref(null);
const verifyError = ref('');

const handleVerify = async () => {
  verifyError.value = '';
  try {
    const response = await verifyForPasswordReset({
      loginId: pwLoginId.value,
      name: pwName.value,
      email: pwEmail.value,
    });
    verifiedUserId.value = response.data;
  } catch (error) {
    verifyError.value = '일치하는 계정 정보가 없습니다.';
  }
};

// 비밀번호 찾기 - 재설정 단계
const newPassword = ref('');
const newPasswordConfirm = ref('');
const resetError = ref('');
const resetSuccess = ref(false);

const handleResetPassword = async () => {
  resetError.value = '';
  if (newPassword.value !== newPasswordConfirm.value) {
    resetError.value = '비밀번호가 일치하지않습니다';
    return;
  }

  try {
    await resetPassword({
      userId: verifiedUserId.value,
      newPassword: newPassword.value,
    });
    resetSuccess.value = true;
  } catch (error) {
    resetError.value = '비밀번호 변경 중 오류가 발생했습니다.';
  }
};

const switchTab = (target) => {
  tab.value = target;
  // 탭 전환시 상태 초기화
  maskedLoginId.value = '';
  findIdError.value = '';
  verifiedUserId.value = null;
  verifyError.value = '';
  resetError.value = '';
  resetSuccess.value = false;
};
</script>

<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
    <div class="bg-white rounded-2xl w-full max-w-md p-7 relative">
      <div class="mb-5 flex h-8 items-center justify-between">
        <h2 class="flex items-center gap-2 text-base font-bold text-gray-800">
          <span
            class="flex h-7 w-7 items-center justify-center rounded-full bg-amber-100 text-sm text-amber-600"
          >
            <i class="fa-solid fa-key" aria-hidden="true"></i>
          </span>
          계정 / 비밀번호 찾기
        </h2>
        <button
          @click="emit('close')"
          class="flex h-8 w-8 items-center justify-center rounded-full text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
          aria-label="닫기"
        >
          <i class="fa-solid fa-xmark" aria-hidden="true"></i>
        </button>
      </div>

      <div class="flex rounded-full bg-gray-100 p-1 mb-6">
        <button
          @click="switchTab('findId')"
          :class="
            tab === 'findId' ? 'bg-white shadow font-semibold text-indigo-600' : 'text-gray-500'
          "
          class="flex-1 rounded-full py-2 text-sm"
        >
          아이디 찾기
        </button>
        <button
          @click="switchTab('findPassword')"
          :class="
            tab === 'findPassword'
              ? 'bg-white shadow font-semibold text-indigo-600'
              : 'text-gray-500'
          "
          class="flex-1 rounded-full py-2 text-sm"
        >
          비밀번호 찾기 & 즉시 변경
        </button>
      </div>

      <!-- 아이디 찾기 -->
      <div v-if="tab === 'findId'">
        <p class="text-sm text-gray-500 mb-4">
          가입 시 등록한 <span class="font-semibold">이메일 주소</span>를 입력하시면 마스킹된
          아이디를 즉시 안내해드립니다.
        </p>
        <label class="block text-sm text-gray-600 mb-1">이메일</label>
        <input
          v-model="email"
          type="email"
          placeholder="example@email.com"
          class="w-full border rounded-lg px-4 py-3 mb-4"
        />

        <div v-if="maskedLoginId" class="find-id-result">
          <span>조회된 계정 아이디 (마스킹)</span>
          <strong>{{ maskedLoginId }}</strong>
        </div>
        <p v-if="findIdError" class="text-red-500 text-sm mb-4">{{ findIdError }}</p>

        <button
          @click="handleFindId"
          class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold"
        >
          아이디 찾기
        </button>
      </div>

      <!-- 비밀번호 찾기 -->
      <div v-else>
        <template v-if="!verifiedUserId">
          <p class="text-sm text-gray-500 mb-4">아이디, 이름, 이메일을 입력해주세요.</p>

          <label class="block text-sm text-gray-600 mb-1">아이디</label>
          <input
            v-model="pwLoginId"
            type="text"
            placeholder="아이디를 입력하세요"
            class="w-full border rounded-lg px-4 py-3 mb-3"
          />

          <label class="block text-sm text-gray-600 mb-1">이름</label>
          <input
            v-model="pwName"
            type="text"
            placeholder="이름을 입력하세요"
            class="w-full border rounded-lg px-4 py-3 mb-3"
          />

          <label class="block text-sm text-gray-600 mb-1">이메일</label>
          <input
            v-model="pwEmail"
            type="email"
            placeholder="example@email.com"
            class="w-full border rounded-lg px-4 py-3 mb-4"
          />

          <p v-if="verifyError" class="text-red-500 text-sm mb-4">{{ verifyError }}</p>

          <button
            @click="handleVerify"
            class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold"
          >
            본인 확인
          </button>
        </template>

        <template v-else-if="!resetSuccess">
          <div class="find-password-result">
            <span>본인 인증 완료!</span>
            <strong>새 비밀번호를 설정해 주세요</strong>
          </div>

          <label class="block text-sm text-gray-600 mb-1">새 비밀번호</label>
          <input
            v-model="newPassword"
            type="password"
            placeholder="새 비밀번호를 입력하세요"
            class="w-full border rounded-lg px-4 py-3 mb-3"
          />

          <label class="block text-sm text-gray-600 mb-1">새 비밀번호 재확인</label>
          <input
            v-model="newPasswordConfirm"
            type="password"
            placeholder="새 비밀번호를 다시 입력하세요"
            class="w-full border rounded-lg px-4 py-3 mb-4"
          />

          <p v-if="resetError" class="text-red-500 text-sm mb-4">{{ resetError }}</p>

          <button
            @click="handleResetPassword"
            class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold"
          >
            비밀번호 변경 완료
          </button>
        </template>

        <template v-else>
          <p class="text-center text-green-600 font-semibold py-8">
            비밀번호가 변경되었습니다.<br />새 비밀번호로 로그인해주세요.
          </p>
          <button
            @click="emit('close')"
            class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold"
          >
            확인
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
input {
  border-radius: 12px;
  font-size: 14px;
  font-weight: 400;
}

input::placeholder {
  color: #a7afbd;
  font-size: 14px;
  font-weight: 400;
}

button.bg-indigo-600 {
  border-radius: 12px;
}

input:focus {
  border-color: #4058f5;
  outline: none;
  box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
}

.find-id-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  margin-bottom: 16px;
  padding: 13px 16px;
  border: 1px solid #dce5ff;
  border-radius: 14px;
  background: #f3f6ff;
  color: #4058f5;
  text-align: center;
}

.find-id-result span {
  font-size: 11px;
  font-weight: 700;
}
.find-id-result strong {
  color: #1f2b55;
  font-size: 15px;
}

.find-password-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  margin-bottom: 16px;
  padding: 13px 16px;
  border: 1px solid #ccebd8;
  border-radius: 14px;
  background: #f1fbf5;
  text-align: center;
}

.find-password-result span {
  color: #1f9a60;
  font-size: 11px;
  font-weight: 700;
}

.find-password-result strong {
  color: #177446;
  font-size: 14px;
}
</style>
