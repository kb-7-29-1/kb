<script setup>
const reload = () => window.location.reload();
</script>

<template>
  <div class="error-fallback">
    <p class="error-fallback__icon">😥</p>
    <h1>일시적인 문제가 발생했습니다</h1>
    <p>불편을 드려 죄송합니다. 새로고침 후 다시 시도해주세요.</p>
    <button type="button" @click="reload">새로고침</button>
  </div>
</template>

<style scoped>
.error-fallback {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: min(100%, 28rem);
  height: 100dvh;
  margin: 0 auto;
  padding: 32px 28px max(32px, env(safe-area-inset-bottom));
  background: #f6f8fc;
  text-align: center;
}

.error-fallback__icon {
  margin: 0 0 18px;
  font-size: 48px;
  line-height: 1;
}

.error-fallback h1 {
  margin: 0 0 10px;
  color: #20283a;
  font-size: 18px;
  font-weight: 700;
}

.error-fallback p {
  margin: 0 0 28px;
  color: #697386;
  font-size: 14px;
  line-height: 1.5;
}

.error-fallback button {
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

.error-fallback button:hover {
  background: #3148e5;
}

.error-fallback button:active {
  transform: scale(0.99);
}
</style>