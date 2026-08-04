<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import LoginForm from '@/components/auth/LoginForm.vue';
import SignupForm from '@/components/auth/SignupForm.vue';
import FindAccountModal from '@/components/auth/FindAccountModal.vue';

const route = useRoute();
const router = useRouter();
const mode = computed(() => (route.name === 'signup' ? 'signup' : 'login'));
const showFindModal = ref(false);
</script>
<template>
  <div class="auth-page">
    <main class="auth-card" :class="{ 'auth-card--login': mode === 'login' }">
      <div class="auth-heading">
        <h1 class="auth-logo" @click="router.push({ name: 'login' })">
          <span class="auth-logo__mark"
            ><i class="fa-solid fa-shield-halved" aria-hidden="true"></i
          ></span>
          살고싶오
        </h1>
        <template v-if="mode === 'login'">
          <p class="auth-greeting">
            낯선 동네도<br />
            <span>안심</span>하고 이사하세요
          </p>
        </template>
        <template v-else>
          <p class="auth-signup-title">회원가입</p>
        </template>
      </div>

      <LoginForm v-if="mode === 'login'" />
      <SignupForm v-else />

      <div class="auth-links">
        <template v-if="mode === 'login'">
          <button class="auth-find-button" @click="showFindModal = true">
            아이디 · 비밀번호 찾기
          </button>
          <p>
            계정이 없으신가요?
            <button @click="router.push({ name: 'signup' })">회원가입</button>
          </p>
        </template>
        <template v-else>
          <p>
            이미 계정이 있으신가요?
            <button @click="router.push({ name: 'login' })">로그인</button>
          </p>
        </template>
      </div>
    </main>
    <FindAccountModal v-if="showFindModal" @close="showFindModal = false" />
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f6f8fc;
}

.auth-card {
  width: 100%;
  max-width: 430px;
  padding: 34px 26px 30px;
  border: 1px solid #edf0f5;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 16px 38px rgb(45 62 100 / 8%);
}

.auth-heading {
  margin-bottom: 40px;
}

.auth-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #4058f5;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.4px;
  cursor: pointer;
}

.auth-logo__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: #4058f5;
  font-size: 20px;
}

.auth-greeting {
  margin: 19px 0 0;
  color: #242b3c;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.5;
  letter-spacing: -0.5px;
}

.auth-greeting span {
  color: #4058f5;
}
.auth-signup-title {
  margin: 22px 0 0;
  color: #151b2b;
  font-size: 22px;
  font-weight: 800;
}

.auth-links {
  margin-top: 26px;
  color: #98a1b2;
  font-size: 13px;
  line-height: 1.7;
  text-align: center;
}

.auth-find-button {
  margin-bottom: 8px;
}
.auth-links button {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}
.auth-links p {
  margin: 0;
}
.auth-links p button {
  margin-left: 4px;
  color: #4058f5;
  font-weight: 700;
}

@media (max-width: 480px) {
  .auth-page {
    align-items: flex-start;
    padding: 0;
    background: #fff;
  }
  .auth-card {
    min-height: 100dvh;
    padding: max(142px, calc(env(safe-area-inset-top) + 108px)) 24px 32px;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .auth-card--login {
    position: relative;
    display: block;
  }

  .auth-card--login .auth-heading {
    margin-bottom: 0;
  }
  .auth-card--login :deep(.login-form) {
    position: absolute;
    top: 50%;
    right: 24px;
    left: 24px;
    transform: translateY(-50%);
  }
  .auth-card--login .auth-links {
    position: absolute;
    top: calc(50% + 154px);
    right: 24px;
    left: 24px;
    margin-top: 0;
  }
}
</style>
