<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '@/api/authService';
import onboardingApi from '@/api/onboardingApi';
import { useAuthStore } from '@/stores/useAuthStore.js';

const router = useRouter();
const authStore = useAuthStore();

const loginId = ref('');
const password = ref('');
const errorMessage = ref('');

const handleLogin = async () => {
  errorMessage.value = '';
  try {
    const response = await login({
      loginId: loginId.value,
      password: password.value,
    });
    authStore.setAuth(response.data.token, response.data.user);

    try {
      await onboardingApi.getOnboarding();
      router.push({ name: 'home' });
    } catch (onboardingError) {
      if (onboardingError.response?.status === 404) {
        router.push({ name: 'onboarding' });
        return;
      }

      errorMessage.value = '온보딩 정보를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.';
    }
  } catch (error) {
    errorMessage.value = '아이디 또는 비밀번호가 일치하지 않습니다.';
  }
};
</script>

<template>
  <form @submit.prevent="handleLogin" class="login-form">
    <div class="login-field">
      <label>아이디</label>
      <input v-model="loginId" type="text" placeholder="아이디를 입력하세요" required />
    </div>

    <div class="login-field">
      <label>비밀번호</label>
      <input v-model="password" type="password" placeholder="비밀번호를 입력하세요" required />
    </div>

    <div class="login-error-slot">
      <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>
    </div>

    <button type="submit" class="login-submit">로그인</button>
  </form>
</template>

<style scoped>
.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.login-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

.login-field label {
  color: #697386;
  font-size: 13px;
  font-weight: 600;
}

.login-field input {
  width: 100%;
  height: 48px;
  padding: 0 15px 0 42px;
  border: 1px solid #e0e5f5;
  border-radius: 10px;
  outline: none;
  background: #eef1ff;
  color: #20283a;
  font-size: 14px;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.login-field input::placeholder {
  color: #b2bac8;
}

.login-field::after {
  position: absolute;
  bottom: 15px;
  left: 15px;
  color: #8a96b5;
  font-family: 'Font Awesome 7 Free';
  font-size: 12px;
  font-weight: 900;
  content: '\f0e0';
  pointer-events: none;
}

.login-field:nth-child(2)::after {
  content: '\f023';
}
.login-field input:focus {
  border-color: #4058f5;
  box-shadow: 0 0 0 3px rgb(64 88 245 / 12%);
}

.login-error {
  margin: 0;
  color: #ef4444;
  font-size: 12px;
}

.login-error-slot {
  min-height: 18px;
  margin: -5px 0 0;
}

.login-submit {
  width: 100%;
  height: 50px;
  border: 0;
  border-radius: 10px;
  background: #4051db;
  box-shadow: none;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    transform 0.15s ease;
}

.login-submit:hover {
  background: #3148e5;
}
.login-submit:active {
  transform: scale(0.99);
}
</style>
