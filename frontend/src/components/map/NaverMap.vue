<script setup>
import { ref, onMounted } from 'vue';
import PropertyPin from './PropertyPin.vue';

const props = defineProps({
  properties: {
    type: Array,
    default: () => [],
  },
  selectedProperty: {
    type: Object,
    default: null,
  },
  destination: {
    type: Object,
    default: () => ({
      name: '강남역 (주 목적지)',
      lat: 37.4979,
      lng: 127.0276,
    }),
  },
});

const emit = defineEmits(['select-property']);

const mapInstance = ref(null);
const zoomLevel = ref(15);

// 순수 네이버 지도 SDK 초기화
const initMap = () => {
  if (window.naver && window.naver.maps) {
    try {
      mapInstance.value = new window.naver.maps.Map('naver-map-container', {
        center: new window.naver.maps.LatLng(
          props.destination.lat,
          props.destination.lng,
        ),
        zoom: zoomLevel.value,
        zoomControl: false,
      });
    } catch (e) {
      console.warn('Naver map init:', e);
    }
  }
};

onMounted(() => {
  const clientId = import.meta.env.VITE_NAVER_CLIENT_ID;
  if (!clientId) return;

  const scriptId = 'naver-map-sdk';
  if (!document.getElementById(scriptId)) {
    const script = document.createElement('script');
    script.id = scriptId;
    script.type = 'text/javascript';
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpClientId=${clientId}&submodules=geocoder`;
    script.onload = () => {
      initMap();
    };
    document.head.appendChild(script);
  } else {
    initMap();
  }
});
</script>

<template>
  <div class="relative w-full h-full overflow-hidden select-none">
    <!-- 1. 순수 네이버 지도 SDK 전용 타일 캔버스 (z-0) -->
    <div id="naver-map-container" class="w-full h-full z-0"></div>

    <!-- 2. 지도 위 커스텀 마커 핀 오버레이 (z-10) -->
    <div class="absolute inset-0 z-10 pointer-events-none">
      <!-- 주 목적지 핀 (🚩 직장/학교) -->
      <div
        class="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 flex flex-col items-center pointer-events-auto cursor-pointer"
      >
        <div
          class="px-3.5 py-1.5 rounded-full bg-blue-600 text-white font-bold text-xs shadow-xl flex items-center gap-1.5 animate-bounce border border-blue-400"
        >
          <span>🚩</span>
          <span>{{ destination.name }}</span>
        </div>
        <div class="w-3 h-3 bg-blue-600 rotate-45 -mt-1.5"></div>
        <div class="w-8 h-3 bg-black/20 rounded-full blur-xs mt-1"></div>
      </div>

      <!-- 매물 마커 핀 목록 -->
      <div
        v-for="(prop, index) in properties"
        :key="prop.propertyId || index"
        class="absolute transform -translate-x-1/2 -translate-y-full transition-all duration-300 pointer-events-auto"
        :style="{
          top: `${32 + (index % 4) * 14 + (index % 2) * 6}%`,
          left: `${24 + ((index * 13) % 55)}%`,
        }"
      >
        <PropertyPin
          :property="prop"
          :is-selected="
            selectedProperty && selectedProperty.propertyId === prop.propertyId
          "
          @click="emit('select-property', prop)"
        />
      </div>
    </div>

    <!-- 3. 지도 줌 오버레이 컨트롤 (z-20) -->
    <div
      class="absolute right-6 top-6 z-20 flex flex-col gap-1.5 bg-white rounded-xl shadow-lg border border-slate-200 p-1"
    >
      <button
        type="button"
        class="w-9 h-9 rounded-lg flex items-center justify-center font-bold text-slate-700 hover:bg-slate-100 transition-colors"
        title="확대"
        @click="zoomLevel++"
      >
        +
      </button>
      <div class="h-px bg-slate-200"></div>
      <button
        type="button"
        class="w-9 h-9 rounded-lg flex items-center justify-center font-bold text-slate-700 hover:bg-slate-100 transition-colors"
        title="축소"
        @click="zoomLevel--"
      >
        -
      </button>
    </div>
  </div>
</template>
