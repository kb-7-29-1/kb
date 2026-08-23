<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore.js';

const router = useRouter();
const authStore = useAuthStore();

const goHome = () => {
  router.push({ name: authStore.isLoggedIn ? 'home' : 'login' });
};
</script>

<template>
  <div class="not-found">
    <p class="not-found__code">404</p>
    <p class="not-found__icon">🧭</p>
    <h1>페이지를 찾을 수 없어요</h1>
    <p>주소가 잘못되었거나 삭제된 페이지예요.<br />입력하신 주소를 다시 확인해 주세요.</p>
    <button type="button" @click="goHome">홈으로 가기</button>
  </div>
</template>

<style scoped>
.not-found {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: min(100%, 28rem);
  height: 100%;
  min-height: 100%;
  flex: 1;
  margin: 0 auto;
  padding: 32px 28px max(32px, env(safe-area-inset-bottom));
  background: #f6f8fc;
  text-align: center;
}

.not-found__code {
  margin: 0 0 8px;
  color: #4051db;
  font-size: 64px;
  font-weight: 800;
  line-height: 1;
}

.not-found__icon {
  margin: 0 0 18px;
  font-size: 40px;
}

.not-found h1 {
  margin: 0 0 10px;
  color: #20283a;
  font-size: 18px;
  font-weight: 700;
}

.not-found p {
  margin: 0 0 28px;
  color: #697386;
  font-size: 14px;
  line-height: 1.5;
}

.not-found button {
  width: 100%;
  max-width: 260px;
  height: 50px;
  border: 0;
  border-radius: 10px;
  background: #4051db;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.not-found button:hover {
  background: #3148e5;
}

.not-found button:active {
  transform: scale(0.99);
}
</style>