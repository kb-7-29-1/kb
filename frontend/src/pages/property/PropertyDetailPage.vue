<script setup>
import { ref, watch } from 'vue';
import WalkingTime from '@/components/property/WalkingTime.vue';
import amenityService from '@/api/amenityService';

const props = defineProps({
  propertyId: {
    type: Number,
    default: null,
  },
  appliedAmenityFilters: {
    type: Array,
    default: () => [],
  },
});

const amenities = ref([]);

const loadAmenities = async () => {
  if (!props.propertyId || !props.appliedAmenityFilters.length) {
    amenities.value = [];
    return;
  }

  try {
    amenities.value = await amenityService.filterAmenities(
      props.propertyId,
      props.appliedAmenityFilters,
    );
  } catch (error) {
    amenities.value = [];
    console.error('편의시설 도보시간 조회에 실패했습니다.', error);
  }
};

watch(
  () => [props.propertyId, props.appliedAmenityFilters],
  loadAmenities,
  { immediate: true, deep: true },
);
</script>

<template>
  <main class="property-detail-page">
    <WalkingTime :amenities="amenities" />
  </main>
</template>

<style scoped>
.property-detail-page {
  min-height: 100dvh;
  background: #fff;
}
</style>
