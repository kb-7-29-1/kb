<script setup>
import { createApp, ref, shallowRef, onMounted, onUnmounted, watch } from 'vue';
import IsochroneOverlay from './IsochroneOverlay.vue';
import AmenityPin from './AmenityPin.vue';
import PropertyPin from './PropertyPin.vue';
import DestinationPin from './DestinationPin.vue';
import { getClusteredMarkers, renderClusterPinHTML } from '@/utils/mapClustering';

const props = defineProps({
  properties: {
    type: Array,
    default: () => [],
  },
  selectedProperty: {
    type: Object,
    default: null,
  },
  amenities: {
    type: Array,
    default: () => [],
  },
  destination: {
    type: Object,
    default: () => ({
      name: '세종대학교 (주 목적지)',
      lat: 37.5502,
      lng: 127.0731,
    }),
  },
  showIsochrone: {
    type: Boolean,
    default: true,
  },
  transportMode: {
    type: String,
    default: 'WALK', // 'WALK' | 'TRANSIT'
  },
  travelTime: {
    type: Number,
    default: 15, // 이동 시간 (분)
  },
  walkPace: {
    type: String,
    default: 'NORMAL', // 'SLOW' | 'NORMAL' | 'FAST'
  },
  flexTime: {
    type: Number,
    default: 10, // 여유 시간 (분)
  },
});

const emit = defineEmits(['select-property']);

const mapInstance = shallowRef(null);
const zoomLevel = ref(15);
const markersMap = ref([]);
let resizeObserver = null;

// 편의시설 마커 핀
const renderAmenityPin = (amenity) => {
  const container = document.createElement('div');
  const pinApp = createApp(AmenityPin, { amenity });
  pinApp.mount(container);
  const content = container.innerHTML;
  pinApp.unmount();
  return content;
};

// 매물 마커 핀 (PropertyPin 컴포넌트 렌더링)
const renderPropertyPin = (prop, isSelected) => {
  const depositNum = prop.deposit ? Math.round(prop.deposit / 1000) : 0;
  const priceText = prop.monthlyRent
    ? `${depositNum}천/${prop.monthlyRent}`
    : `전세 ${depositNum}천`;
  const bgClass = isSelected
    ? 'bg-blue-600 text-white ring-4 ring-blue-500/30 border-blue-700 scale-110 z-30'
    : 'bg-slate-900 text-white hover:bg-blue-600 border-slate-700 z-10';
  const icon = isSelected ? '📍' : '🏠';

  const isDb = prop.dataSource === 'DB';
  const tagBg = isDb
    ? 'bg-emerald-500/30 text-emerald-300 border-emerald-400/40'
    : 'bg-indigo-500/30 text-indigo-300 border-indigo-400/40';
  const tagText = isDb ? 'DB' : '공공';

  return `
    <div class="px-2.5 py-1.5 rounded-2xl text-xs font-black shadow-lg border transition-all cursor-pointer flex items-center gap-1.5 transform -translate-x-1/2 -translate-y-full select-none ${bgClass}">
      <span>${icon}</span>
      <span>${priceText}</span>
      <span class="text-[10px] px-1 py-0.2 rounded border font-bold ${tagBg}">${tagText}</span>
    </div>
  `;
};

// 목적지 마커 핀 (DestinationPin 컴포넌트 렌더링)
const renderDestinationPin = (destination) => {
  const name = destination?.name || '주 목적지';
  return `
    <div class="flex flex-col items-center pointer-events-auto cursor-pointer transform -translate-x-1/2 -translate-y-full select-none" title="${name}">
      <div class="px-3.5 py-1.5 rounded-full bg-blue-600 text-white font-bold text-xs shadow-xl flex items-center gap-1.5 border border-blue-400 hover:bg-blue-700 transition-all">
        <span class="inline-block animate-bounce">🚩</span>
        <span>${name}</span>
      </div>
      <div class="w-3 h-3 bg-blue-600 rotate-45 -mt-1.5"></div>
      <div class="w-8 h-8 bg-black/20 rounded-full blur-xs mt-1"></div>
    </div>
  `;
};

// 네이버 지도 SDK 마커 핀 (목적지 핀 + 매물 핀 + 클러스터 핀) 렌더링
const renderMarkers = () => {
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  // 1. 기존 마커 제거
  markersMap.value.forEach((m) => m.setMap(null));
  markersMap.value = [];

  const bounds = mapInstance.value.getBounds();
  const currentZoom = mapInstance.value.getZoom();

  const destLatLng = new window.naver.maps.LatLng(
    props.destination.lat || 37.5502,
    props.destination.lng || 127.0731,
  );

  // 2. 🚩 주 목적지 핀
  const destMarker = new window.naver.maps.Marker({
    position: destLatLng,
    map: mapInstance.value,
    icon: {
      content: renderDestinationPin(props.destination),
    },
  });
  markersMap.value.push(destMarker);

  // 3. 🏢 / 🏠 매물 및 클러스터 마커 렌더링
  const clusteredNodes = getClusteredMarkers(props.properties, currentZoom, bounds);

  clusteredNodes.forEach((node) => {
    if (node.isCluster) {
      const clusterMarker = new window.naver.maps.Marker({
        position: new window.naver.maps.LatLng(node.lat, node.lng),
        map: mapInstance.value,
        icon: {
          content: renderClusterPinHTML(node.count),
        },
      });

      // 클러스터 클릭 시 해당 그룹 영역으로 줌인
      window.naver.maps.Event.addListener(clusterMarker, 'click', () => {
        if (mapInstance.value) {
          mapInstance.value.morph(
            new window.naver.maps.LatLng(node.lat, node.lng),
            currentZoom + 2,
          );
        }
      });

      markersMap.value.push(clusterMarker);
    } else {
      const prop = node.item;
      const isSelected =
        props.selectedProperty &&
        props.selectedProperty.propertyId === prop.propertyId;

      const propMarker = new window.naver.maps.Marker({
        position: new window.naver.maps.LatLng(prop.latitude, prop.longitude),
        map: mapInstance.value,
        icon: {
          content: renderPropertyPin(prop, isSelected),
        },
      });

      window.naver.maps.Event.addListener(propMarker, 'click', () => {
        emit('select-property', prop);
      });

      markersMap.value.push(propMarker);
    }
  });

  // 4. 편의시설 마커
  props.amenities.forEach((amenity) => {
    if (amenity.amenityLatitude == null || amenity.amenityLongitude == null) return;

    const amenityLatLng = new window.naver.maps.LatLng(
      amenity.amenityLatitude,
      amenity.amenityLongitude,
    );
    if (bounds && !bounds.hasLatLng(amenityLatLng)) return;

    const amenityMarker = new window.naver.maps.Marker({
      position: amenityLatLng,
      map: mapInstance.value,
      icon: {
        content: renderAmenityPin(amenity),
      },
    });

    markersMap.value.push(amenityMarker);
  });
};

// 지도 구역 실시간 크기 변경 감지 Observer
const setupResizeObserver = () => {
  const container = document.getElementById('naver-map-container');
  if (!container || resizeObserver) return;

  resizeObserver = new ResizeObserver(() => {
    if (mapInstance.value) {
      if (typeof mapInstance.value.autoResize === 'function') {
        mapInstance.value.autoResize();
      }
      window.dispatchEvent(new Event('resize'));
    }
  });

  resizeObserver.observe(container);
};

// 순수 네이버 지도 SDK 초기화
const initMap = () => {
  if (window.naver && window.naver.maps) {
    try {
      const centerLatLng = new window.naver.maps.LatLng(
        props.destination.lat || 37.5502,
        props.destination.lng || 127.0731,
      );

      mapInstance.value = new window.naver.maps.Map('naver-map-container', {
        center: centerLatLng,
        zoom: zoomLevel.value,
        zoomControl: false,
      });

      // 지도 드래그/확대/축소 완료 시 가시 영역 및 클러스팅 핀 자동 재계산
      window.naver.maps.Event.addListener(mapInstance.value, 'idle', () => {
        renderMarkers();
      });

      renderMarkers();
      setupResizeObserver();
    } catch (e) {
      console.warn('Naver map init:', e);
    }
  }
};

watch(
  [
    () => props.properties,
    () => props.destination,
    () => props.selectedProperty,
    () => props.amenities,
  ],
  () => {
    renderMarkers();
  },
  { deep: true },
);

// 목적지 변경 시 해당 목적지 위치로 지도 부드럽게 이동 (panTo)
watch(
  () => props.destination,
  (newDest) => {
    if (mapInstance.value && window.naver && window.naver.maps && newDest?.lat && newDest?.lng) {
      const newCenter = new window.naver.maps.LatLng(newDest.lat, newDest.lng);
      mapInstance.value.panTo(newCenter, {
        duration: 800,
        easing: 'easeOutCubic',
      });
    }
  },
  { deep: true },
);

watch(zoomLevel, (newZoom) => {
  if (mapInstance.value) {
    mapInstance.value.setZoom(newZoom);
  }
});

onMounted(() => {
  const clientId = import.meta.env.VITE_NAVER_CLIENT_ID;
  if (!clientId) return;

  const scriptId = 'naver-map-sdk';
  if (!document.getElementById(scriptId)) {
    const script = document.createElement('script');
    script.id = scriptId;
    script.type = 'text/javascript';
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${clientId}&submodules=geocoder`;
    script.onload = () => {
      initMap();
    };
    document.head.appendChild(script);
  } else {
    initMap();
  }
});

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
});
</script>

<template>
  <div class="relative w-full h-full overflow-hidden select-none">
    <!-- 1. 순수 네이버 지도 SDK 전용 타일 캔버스 (z-0) -->
    <div id="naver-map-container" class="w-full h-full z-0"></div>

    <!-- 2. 이소크론 동심원 & 외부 암영 마스크 분리 전용 오버레이 컴포넌트 -->
    <IsochroneOverlay
      :map-instance="mapInstance"
      :destination="destination"
      :show-isochrone="showIsochrone"
      :transport-mode="transportMode"
      :travel-time="travelTime"
      :walk-pace="walkPace"
      :flex-time="flexTime"
    />

    <!-- 3. 지도 줌 오버레이 컨트롤 (z-20) -->
    <div
      class="absolute right-4 top-4 z-20 flex flex-col gap-0 rounded-lg border border-slate-200 bg-white p-0.5 shadow-md"
    >
      <button
        type="button"
        class="flex h-8 w-8 items-center justify-center rounded-md bg-transparent text-[16px] font-bold text-[#4058f5] transition-colors hover:bg-[#f8f9ff]"
        title="확대"
        @click="zoomLevel++"
      >
        +
      </button>
      <div class="h-px bg-slate-200"></div>
      <button
        type="button"
        class="flex h-8 w-8 items-center justify-center rounded-md bg-transparent text-[16px] font-bold text-[#4058f5] transition-colors hover:bg-[#f8f9ff]"
        title="축소"
        @click="zoomLevel--"
      >
        -
      </button>
    </div>
  </div>
</template>
