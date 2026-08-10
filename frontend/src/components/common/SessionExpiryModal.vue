<script setup>
import { computed } from 'vue';

const props = defineProps({
  remainingSeconds: { type: Number, required: true },
});
const emit = defineEmits(['extend', 'logout']);

const formatted = computed(() => {
  const m = String(Math.floor(props.remainingSeconds / 60)).padStart(2, '0');
  const s = String(props.remainingSeconds % 60).padStart(2, '0');
  return `${m}:${s}`;
});
</script>

<template>
  <div class="session-expiry-overlay">
    <div
        class="session-expiry-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="session-expiry-modal-title"
    >
      <div class="session-expiry-heading">
        <h3 id="session-expiry-modal-title">
            <span class="session-expiry-icon" aria-hidden="true">
              <i class="fa-regular fa-clock"></i>
            </span>
          세션 만료 안내
        </h3>
      </div>
      <p class="session-expiry-description">
        장시간 활동이 없어 곧 로그아웃됩니다. 계속 이용하시겠어요?
      </p>
      <p class="session-expiry-countdown">{{ formatted }}</p>
      <div class="session-expiry-actions">
        <button type="button" class="session-expiry-cancel-button" @click="emit('logout')">
          로그아웃
        </button>
        <button type="button" class="session-expiry-primary-button" @click="emit('extend')">
          계속 이용하기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.session-expiry-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgb(15 23 42 / 40%);
}

.session-expiry-modal {
  box-sizing: border-box;
  width: 100%;
  max-width: 360px;
  padding: 28px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 20px 48px rgb(15 23 42 / 22%);
  text-align: center;
}

.session-expiry-heading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  margin-bottom: 18px;
}

.session-expiry-heading h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #20283a;
  font-size: 16px;
  font-weight: 700;
}

.session-expiry-icon {
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

.session-expiry-description {
  margin: 0 0 8px;
  color: #7b8797;
  font-size: 13px;
}

.session-expiry-countdown {
  margin: 0 0 20px;
  color: #4051db;
  font-size: 28px;
  font-weight: 700;
}

.session-expiry-actions {
  display: flex;
  gap: 8px;
}

.session-expiry-actions button {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.session-expiry-cancel-button {
  border: 1px solid #e0e5f5;
  background: #fff;
  color: #697386;
}

.session-expiry-primary-button {
  border: 1px solid #4051db;
  background: #4051db;
  color: #fff;
}
</style>