<script setup>
import { createApp, ref, shallowRef, computed, onMounted, onUnmounted, watch } from 'vue';
import IsochroneOverlay from './IsochroneOverlay.vue';
import AmenityPin from './AmenityPin.vue';
import {
  getClusteredMarkers,
  renderClusterPinHTML,
  renderPropertyPinHTML,
  renderDestinationPinHTML,
} from '@/utils/mapClustering';

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
      name: '세종대학교',
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
const amenityMarkers = new Map();
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
      content: renderDestinationPinHTML(props.destination),
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
          content: renderClusterPinHTML(node.count, node.items),
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
        props.selectedProperty && props.selectedProperty.propertyId === prop.propertyId;

      const propMarker = new window.naver.maps.Marker({
        position: new window.naver.maps.LatLng(prop.latitude, prop.longitude),
        map: mapInstance.value,
        icon: {
          content: renderPropertyPinHTML(prop, isSelected),
        },
      });

      window.naver.maps.Event.addListener(propMarker, 'click', () => {
        emit('select-property', prop);
      });

      markersMap.value.push(propMarker);
    }
  });

  // 4. 편의시설 마커
};

const getAmenityMarkerKey = (amenity) =>
  `${amenity.propertyId ?? 'map'}-${amenity.amenityType}-${amenity.amenityLatitude}-${amenity.amenityLongitude}`;

const clearAmenityMarkers = () => {
  amenityMarkers.forEach(({ marker }) => marker.setMap(null));
  amenityMarkers.clear();
};

const renderAmenityMarkers = () => {
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  const nextKeys = new Set();
  props.amenities.forEach((amenity) => {
    if (amenity.amenityLatitude == null || amenity.amenityLongitude == null) return;

    const key = getAmenityMarkerKey(amenity);
    nextKeys.add(key);
    if (amenityMarkers.has(key)) return;

    const amenityMarker = new window.naver.maps.Marker({
      position: new window.naver.maps.LatLng(amenity.amenityLatitude, amenity.amenityLongitude),
      map: mapInstance.value,
      zIndex: 30,
      icon: {
        content: renderAmenityPin(amenity),
        anchor: new window.naver.maps.Point(0, 0),
      },
    });

    amenityMarkers.set(key, { marker: amenityMarker });
  });

  amenityMarkers.forEach(({ marker }, key) => {
    if (!nextKeys.has(key)) {
      marker.setMap(null);
      amenityMarkers.delete(key);
    }
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

      // 지도 드래그/확대/축소 완료 시 가시 영역 및 클러스팅 핀 자동 재계산 및 목적지 거리 감지
      window.naver.maps.Event.addListener(mapInstance.value, 'idle', () => {
        renderMarkers();
        checkDistanceToDestination();
      });
      window.naver.maps.Event.addListener(mapInstance.value, 'center_changed', () => {
        checkDistanceToDestination();
      });

      renderMarkers();
      renderAmenityMarkers();
      setupResizeObserver();
      checkDistanceToDestination();
    } catch (e) {
      console.warn('Naver map init:', e);
    }
  }
};

watch(
  [() => props.properties, () => props.destination, () => props.selectedProperty],
  () => {
    renderMarkers();
    checkDistanceToDestination();
  },
  { deep: true },
);

watch(
  () => props.amenities,
  () => renderAmenityMarkers(),
  { deep: true },
);

// 도보/대중교통 이동시간 최대 원 범위에 맞추어 지도 줌/카메라 범위 자동 조율
const fitToIsochroneRadius = () => {
  if (
    !mapInstance.value ||
    !window.naver ||
    !window.naver.maps ||
    !props.destination?.lat ||
    !props.destination?.lng
  )
    return;
  if (!props.showIsochrone) return;

  let radiusMeters = 900;
  if (props.transportMode === 'WALK') {
    let speedMetersPerMin = 75;
    if (props.walkPace === 'SLOW') speedMetersPerMin = 58;
    if (props.walkPace === 'FAST') speedMetersPerMin = 92;
    radiusMeters = Math.max(200, props.travelTime * speedMetersPerMin);
  } else {
    const transitBaseRadius = Math.max(500, props.travelTime * 180);
    radiusMeters = Math.max(transitBaseRadius + 200, (props.travelTime + props.flexTime) * 180);
  }

  const earthRadius = 6378137;
  const centerLat = props.destination.lat;
  const centerLng = props.destination.lng;
  const latRad = (centerLat * Math.PI) / 180;

  // 15% 여유 공간 마진
  const latOffset = (radiusMeters / earthRadius) * (180 / Math.PI) * 1.15;
  const lngOffset = (((radiusMeters / (earthRadius * Math.cos(latRad))) * 180) / Math.PI) * 1.15;

  const bounds = new window.naver.maps.LatLngBounds(
    new window.naver.maps.LatLng(centerLat - latOffset, centerLng - lngOffset),
    new window.naver.maps.LatLng(centerLat + latOffset, centerLng + lngOffset),
  );

  mapInstance.value.fitBounds(bounds);
};

// 목적지 및 이동시간 필터 변경 시(적용하기 클릭 시) 해당 최대 원 크기에 맞춰 줌 조율
watch(
  [
    () => props.destination,
    () => props.travelTime,
    () => props.transportMode,
    () => props.walkPace,
    () => props.flexTime,
    () => props.showIsochrone,
  ],
  () => {
    fitToIsochroneRadius();
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
  markersMap.value.forEach((marker) => marker.setMap(null));
  clearAmenityMarkers();
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
});
const isFarFromDestination = ref(false);

const destinationName = computed(() => {
  const raw = props.destination?.name || props.destination?.destName || '내 목적지';
  return raw.replace(/\s*\(주 목적지\)$/, '');
});

const checkDistanceToDestination = () => {
  if (!mapInstance.value || !props.destination || !window.naver || !window.naver.maps) return;
  const targetLat = Number(props.destination.lat || props.destination.destLatitude) || 37.5502;
  const targetLng = Number(props.destination.lng || props.destination.destLongitude) || 127.0731;

  const bounds = mapInstance.value.getBounds();
  const destLatLng = new window.naver.maps.LatLng(targetLat, targetLng);

  // 목적지 핀(🚩)이 화면 가시 영역(Viewport)을 완전히 벗어났을 때만 스르륵 버튼 등장
  isFarFromDestination.value = bounds ? !bounds.hasLatLng(destLatLng) : false;
};

// 내 목적지로 카메라 빠른 이동 (도보/대중교통 및 이동시간 범위에 맞추어 줌 레벨 자동 조율)
const moveMapToDestination = () => {
  if (!mapInstance.value || !props.destination) return;
  // 선택된 이동 수단(도보/대중교통), 걸음 속도, 이동 시간에 맞춰 최적 줌 레벨 및 위치로 이동
  fitToIsochroneRadius();
  isFarFromDestination.value = false;
};
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

    <!-- 3. 목적지에서 멀어졌을 때 상단 중앙 스르륵 등장하는 플로팅 복귀 캡슐 버튼 (z-30) -->
    <Transition name="slide-fade">
      <button
        v-if="isFarFromDestination"
        type="button"
        class="absolute top-16 left-1/2 -translate-x-1/2 z-30 flex items-center gap-2 px-4 py-2.5 rounded-full bg-slate-900/90 text-white border border-blue-400/50 shadow-2xl text-xs font-bold transition-all hover:bg-blue-600 hover:scale-105 active:scale-95 cursor-pointer backdrop-blur-md"
        title="목적지 위치로 지도 카메라 복귀"
        @click="moveMapToDestination"
      >
        <span class="text-blue-400 animate-pulse text-sm">📍</span>
        <span
          ><strong class="text-blue-300">{{ destinationName }}</strong
          >(으)로 돌아갈래요</span
        >
      </button>
    </Transition>

    <!-- 4. 지도 줌 오버레이 컨트롤 (z-20) -->
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

<style scoped>
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translate(-50%, -20px);
  opacity: 0;
}
</style>
