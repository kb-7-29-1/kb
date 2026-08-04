<script setup>
import { useRouter } from 'vue-router';
import { ref } from 'vue';
import { checkId, signup } from '@/api/authService.js';

const router = useRouter();

const form = ref({
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  email: '',
  birthDate: '',
  gender: 'M',
});

const idChecked = ref(false);
const idCheckMessage = ref('');
const errorMessage = ref('');

const handleCheckId = async () => {
  if (!form.value.loginId) {
    idCheckMessage.value = '아이디를 입력해주세요.';
    return;
  }
  try {
    const response = await checkId(form.value.loginId);
    if (response.data === true) {
      idCheckMessage.value = '사용 가능한 아이디 입니다.';
      idChecked.value = true;
    } else {
      idCheckMessage.value = '이미 사용중인 아이디 입니다.';
      idChecked.value = false;
    }
  } catch (error) {
    idCheckMessage.value = '중복 확인 중 오류가 발생 했습니다.';
  }
};

const handleSignup = async () => {
  errorMessage.value = '';
  if (!idChecked.value) {
    errorMessage.value = '아이디 중복확인을 해주세요.';
    return;
  }
  if (form.value.password !== form.value.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.';
    return;
  }
  try {
    await signup({
      loginId: form.value.loginId,
      password: form.value.password,
      name: form.value.name,
      email: form.value.email,
      birthDate: form.value.birthDate,
      gender: form.value.gender,
    });
    alert('회원가입이 완료되었습니다.');
    router.push({ name: 'login' });
  } catch (error) {
    errorMessage.value = '회원가입 중 오류가 발생했습니다.';
    console.log(error);
  }
};
</script>

<template>
  <form @submit.prevent="handleSignup" class="signup-form flex flex-col gap-4">
    <div>
      <label class="block text-sm text-gray-600 mb-1">이름</label>
      <i class="signup-field-icon fa-regular fa-user" aria-hidden="true"></i>
      <input
        v-model="form.name"
        type="text"
        placeholder="홍길동"
        class="w-full border rounded-lg px-4 py-3"
        required
      />
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">아이디</label>
      <i
        class="signup-field-icon signup-field-icon--login-id fa-regular fa-id-card"
        aria-hidden="true"
      ></i>
      <div class="flex gap-2">
        <input
          v-model="form.loginId"
          type="text"
          placeholder="ID를 입력하세요"
          class="flex-1 border rounded-lg px-4 py-3"
          @input="idChecked = false"
          required
        />
        <button
          type="button"
          @click="handleCheckId"
          class="rounded-xl px-4 py-3 text-sm font-semibold whitespace-nowrap text-[#4058f5] border border-200"
        >
          중복 확인
        </button>
      </div>
      <p
        v-if="idCheckMessage"
        class="text-sm mt-1"
        :class="idChecked ? 'text-green-600' : 'text-red-500'"
      >
        {{ idCheckMessage }}
      </p>
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">이메일</label>
      <i class="signup-field-icon fa-regular fa-envelope" aria-hidden="true"></i>
      <input
        v-model="form.email"
        type="email"
        placeholder="example@email.com"
        class="w-full border rounded-lg px-4 py-3"
        required
      />
    </div>

    <div class="signup-birth-gender-row flex gap-4">
      <div class="signup-birth-field flex-[1.8]">
        <label class="block text-sm text-gray-600 mb-1">출생연도</label>
        <div class="birth-date-field relative">
          <i
            class="fa-regular fa-calendar absolute left-3 top-1/2 -translate-y-1/2 text-xs text-slate-400"
            aria-hidden="true"
          ></i>
          <span v-if="!form.birthDate" class="birth-date-placeholder">YYYY-MM-DD</span>
          <input
            v-model="form.birthDate"
            type="date"
            class="w-full border rounded-lg py-3 pl-9 pr-4"
            :class="{ 'is-empty': !form.birthDate }"
            required
          />
        </div>
      </div>
      <div class="signup-gender-field flex-[0.8]">
        <label class="block text-sm text-gray-600 mb-1">성별</label>
        <div class="gender-select flex h-12 gap-2">
          <button
            type="button"
            @click="form.gender = 'M'"
            :class="form.gender === 'M' ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-slate-500'"
            class="flex flex-1 items-center justify-center gap-1 rounded-lg py-3 text-xs font-semibold"
          >
            남
          </button>
          <button
            type="button"
            @click="form.gender = 'F'"
            :class="form.gender === 'F' ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-slate-500'"
            class="flex flex-1 items-center justify-center gap-1 rounded-lg py-3 text-xs font-semibold"
          >
            여
          </button>
        </div>
      </div>
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">비밀번호</label>
      <i class="signup-field-icon fa-solid fa-lock" aria-hidden="true"></i>
      <input
        v-model="form.password"
        type="password"
        placeholder="8자 이상"
        class="w-full border rounded-lg px-4 py-3"
        required
      />
    </div>

    <div>
      <label class="block text-sm text-gray-600 mb-1">비밀번호 확인</label>
      <i class="signup-field-icon fa-solid fa-lock" aria-hidden="true"></i>
      <input
        v-model="form.passwordConfirm"
        type="password"
        placeholder="비밀번호를 다시 입력하세요"
        class="w-full border rounded-lg px-4 py-3"
        required
      />
    </div>

    <p v-if="errorMessage" class="text-red-500 text-sm">{{ errorMessage }}</p>

    <div class="signup-submit-area">
      <button type="submit" class="w-full bg-indigo-600 text-white rounded-lg py-3 font-semibold">
        회원가입 완료
      </button>
    </div>
  </form>
</template>

<style scoped>
@media (min-width: 481px) {
  .signup-form {
    gap: 16px;
  }

  .signup-form > div:has(> .signup-field-icon) {
    position: relative;
  }

  .signup-field-icon {
    position: absolute;
    top: 49px;
    left: 14px;
    z-index: 1;
    color: #9aa6bf;
    font-size: 13px;
    pointer-events: none;
    transform: translateY(-50%);
  }

  .signup-field-icon--login-id {
    top: 49px;
  }

  .signup-form input:not([type='date']) {
    min-height: 48px;
    padding-left: 38px;
    border-radius: 12px;
    font-size: 14px;
  }

  .signup-form input[type='date'] {
    border-radius: 12px;
    font-size: 14px;
  }

  .signup-form input[type='date'].is-empty {
    color: transparent;
  }

  .signup-form input[type='date'].is-empty::-webkit-datetime-edit {
    color: transparent;
  }

  .signup-form input:focus {
    border-color: #4058f5;
    outline: none;
    box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
  }

  .signup-form .flex > button[type='button'] {
    min-height: 48px;
    border-radius: 12px;
  }

  .signup-birth-gender-row {
    align-items: flex-end;
  }

  .signup-birth-field,
  .signup-gender-field {
    min-width: 0;
  }

  .birth-date-placeholder {
    position: absolute;
    top: 50%;
    left: 36px;
    z-index: 1;
    padding-right: 4px;
    background: #fff;
    color: #a7afbd;
    font-size: 13px;
    pointer-events: none;
    transform: translateY(-50%);
  }

  .gender-select button {
    border-radius: 12px;
    font-size: 13px;
  }

  .signup-submit-area {
    margin-top: 12px;
  }

  .signup-submit-area button {
    min-height: 60px;
    border-radius: 12px;
  }
}

@media (max-width: 480px) {
  .signup-form {
    flex: 1;
    min-height: 0;
    padding: 22px 24px 110px;
    overflow-y: auto;
    background: #f6f8fc;
  }

  .signup-form label {
    color: #697386;
    font-size: 13px;
    font-weight: 600;
  }

  .signup-form input {
    height: 48px;
    border-color: #e0e5f5;
    border-radius: 10px;
    background: #fff;
    font-size: 14px;
  }

  .signup-form input:focus {
    border-color: #4058f5;
    outline: none;
    box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
  }

  .signup-form > div:has(> .signup-field-icon) {
    position: relative;
  }

  .signup-field-icon {
    position: absolute;
    bottom: 17px;
    left: 13px;
    z-index: 1;
    color: #9aa6bf;
    font-size: 12px;
    pointer-events: none;
  }

  .signup-field-icon--login-id {
    top: 42px;
    bottom: auto;
  }

  .signup-form input:not([type='date']) {
    padding-left: 36px;
  }

  .birth-date-placeholder {
    position: absolute;
    top: 50%;
    left: 36px;
    z-index: 1;
    padding-right: 4px;
    background: #fff;
    color: #a7afbd;
    font-size: 12px;
    pointer-events: none;
    transform: translateY(-50%);
  }

  .signup-submit-area {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 20;
    box-sizing: border-box;
    width: 100%;
    min-height: 110px;
    padding: 16px 20px max(16px, calc(env(safe-area-inset-bottom) + 10px));
    border-top: 1px solid #e2e8f0;
    background: #f6f8fc;
  }

  .signup-submit-area button {
    height: 65px;
    border-radius: 14px;
    background: #4051db;
    font-size: 16px;
  }
}
</style>
