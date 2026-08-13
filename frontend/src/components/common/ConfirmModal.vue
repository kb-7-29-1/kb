<script setup>
defineProps({
  message: {
    type: String,
    default: '진행하시겠습니까?',
  },
  confirmLabel: {
    type: String,
    default: '이동',
  },
  cancelLabel: {
    type: String,
    default: '취소',
  },
});

const emit = defineEmits(['confirm', 'cancel']);
</script>

<template>
  <div class="confirm-modal-overlay" @click.self="emit('cancel')">
    <div
      class="confirm-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-modal-message"
    >
      <p id="confirm-modal-message" class="confirm-modal-message">{{ message }}</p>
      <div class="confirm-modal-actions">
        <button type="button" class="confirm-modal-cancel-button" @click="emit('cancel')">
          {{ cancelLabel }}
        </button>
        <button type="button" class="confirm-modal-primary-button" @click="emit('confirm')">
          {{ confirmLabel }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgb(15 23 42 / 40%);
}

.confirm-modal {
  box-sizing: border-box;
  width: 100%;
  max-width: 320px;
  padding: 24px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 20px 48px rgb(15 23 42 / 22%);
  text-align: center;
}

.confirm-modal-message {
  margin: 0 0 20px;
  color: #20283a;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
}

.confirm-modal-actions {
  display: flex;
  gap: 8px;
}

.confirm-modal-actions button {
  flex: 1;
  height: 46px;
  border-radius: 12px;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.confirm-modal-cancel-button {
  border: 1px solid #e0e5f5;
  background: #fff;
  color: #697386;
}

.confirm-modal-primary-button {
  border: 1px solid #4051db;
  background: #4051db;
  color: #fff;
}
</style>