<script setup>
defineProps({
  activeTab: {
    type: String,
    required: true,
  },
});

defineEmits(['change']);
</script>

<template>
  <nav>
    <button type="button" :aria-pressed="activeTab === 'all'" @click="$emit('change', 'all')">
      전체 필터
    </button>
    <button
      type="button"
      :aria-pressed="activeTab === 'amenity'"
      @click="$emit('change', 'amenity')"
    >
      편의시설 필터
    </button>
  </nav>
</template>
