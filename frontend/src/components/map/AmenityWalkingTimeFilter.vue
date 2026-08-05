<template>
  <section v-if="amenities.length" class="amenity-walking-time-filter">
    <h3 class="section-title">편의시설 도보 시간 제한</h3>
    <div class="slider-list">
      <div v-for="item in amenities" :key="item.id" class="slider-item">
        <div class="slider-label">
          <span>{{ item.icon }}</span>
          <span>{{ item.name }}</span>
        </div>
        <div class="slider-control">
          <input
            :value="item.timeLimit"
            type="range"
            min="0"
            max="30"
            step="1"
            class="styled-slider"
            :style="sliderStyle(item.timeLimit)"
            @input="
              $emit('update-time-limit', { id: item.id, timeLimit: Number($event.target.value) })
            "
          />
        </div>
        <div class="slider-value">{{ item.timeLimit }}분</div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  amenities: {
    type: Array,
    required: true,
  },
});

defineEmits(['update-time-limit']);

const sliderStyle = (timeLimit) => {
  const percent = (Number(timeLimit) / 30) * 100;
  return {
    background: `linear-gradient(to right, #3b82f6 0%, #3b82f6 ${percent}%, #e2e8f0 ${percent}%, #e2e8f0 100%)`,
  };
};
</script>

<style scoped>
.amenity-walking-time-filter {
  padding: 16px 0 20px;
}

.section-title {
  margin: 0 0 14px;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
}

.slider-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.slider-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.slider-label {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 83px;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.slider-control {
  flex: 1;
  margin: 0 14px;
}

.styled-slider {
  width: 100%;
  height: 10px;
  margin: 0;
  appearance: none;
  border-radius: 999px;
  box-shadow: inset 0 1px 2px rgb(15 23 42 / 8%);
  cursor: pointer;
}

.styled-slider::-webkit-slider-thumb {
  width: 18px;
  height: 18px;
  appearance: none;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #3b82f6;
  box-shadow: 0 2px 6px rgb(15 23 42 / 25%);
}

.styled-slider::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #3b82f6;
  box-shadow: 0 2px 6px rgb(15 23 42 / 25%);
}

.slider-value {
  width: 40px;
  text-align: right;
  color: #3b82f6;
  font-size: 12px;
  font-weight: 800;
}
</style>
