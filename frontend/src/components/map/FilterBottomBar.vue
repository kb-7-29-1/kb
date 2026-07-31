<template>
  <footer class="filter-bottom-bar">
    <button class="reset-button" type="button">초기화</button>
    <button class="apply-button" type="button">적용 완료</button>
  </footer>
</template>

<style scoped>
.filter-bottom-bar {
  display: grid;
  grid-template-columns: minmax(88px, 1fr) 2fr;
  gap: 8px;
  flex: 0 0 auto;
  width: 100%;
  margin-top: auto;
  padding: 14px 20px calc(14px + env(safe-area-inset-bottom));
  border-top: 1px solid #edf0f5;
  background: #fff;
}

.filter-bottom-bar button {
  height: 65px;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.reset-button {
  border: 1px solid #dfe4ec;
  background: #fff;
  color: #6b7280;
}

.apply-button {
  border: 1px solid #3d55f6;
  background: #3d55f6;
  color: #fff;
}

@media (min-width: 768px) {
  .filter-bottom-bar {
    padding: 16px 28px;
  }
}
</style>
