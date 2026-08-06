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
  isPreviewMode: {
    type: Boolean,
    default: false,
  },
  appliedFilter: {
    type: Object,
    default: null,
  },
  liveFilter: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(['select-property']);

const mapInstance = shallowRef(null);
const zoomLevel = ref(15);
const markersMap = ref([]);
const amenityMarkers = new Map();
const expandedAmenityMarkerKeys = ref(new Set());
let resizeObserver = null;

// 편의시설 마커 핀
const renderAmenityPin = (amenity, isExpanded = false) => {
  const container = document.createElement('div');
  const pinApp = createApp(AmenityPin, { amenity, isExpanded });
  pinApp.mount(container);
  const content = container.innerHTML;
  pinApp.unmount();
  return content;
};

const activePropertyMarkersMap = new Map();
let activeDestMarker = null;
let pendingRenderFrame = null;

// 네이버 지도 SDK 마커 핀 (목적지 핀 + 매물 핀 + 클러스터 핀) 렌더링
// 🚀 [비동기 타임 슬라이싱 (Time-Slicing / Chunking) 최적화]
// - 1단계: 필요 없는 마커 0ms 동기식 즉시 제거 (Quick Removal)
// - 2단계: 신규 마커 생성을 requestAnimationFrame으로 25개씩 시분할 렌더링 (Lazy Async Drawing)
const renderMarkers = () => {
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  if (pendingRenderFrame) {
    cancelAnimationFrame(pendingRenderFrame);
    pendingRenderFrame = null;
  }

  const bounds = mapInstance.value.getBounds();
  const currentZoom = mapInstance.value.getZoom();

  // 1. 🚩 주 목적지 핀 관리 (기존 마커 재생성 방지)
  const destLat = props.destination.lat || 37.5502;
  const destLng = props.destination.lng || 127.0731;
  const destLatLng = new window.naver.maps.LatLng(destLat, destLng);
  const destKey = `${destLat}_${destLng}_${props.destination.name || ''}`;

  if (!activeDestMarker || activeDestMarker._key !== destKey) {
    if (activeDestMarker) activeDestMarker.setMap(null);
    activeDestMarker = new window.naver.maps.Marker({
      position: destLatLng,
      map: mapInstance.value,
      icon: {
        content: renderDestinationPinHTML(props.destination),
      },
    });
    activeDestMarker._key = destKey;
  }

  // 2. 🏢 / 🏠 매물 및 클러스터 마커 렌더링 준비 (Diffing)
  const clusteredNodes = getClusteredMarkers(props.properties, currentZoom, bounds);
  const nextMarkerKeys = new Set();
  const nodesToCreate = [];

  clusteredNodes.forEach((node) => {
    if (node.isCluster) {
      const clusterKey = `cluster_${node.lat.toFixed(5)}_${node.lng.toFixed(5)}_${node.count}`;
      nextMarkerKeys.add(clusterKey);
      if (!activePropertyMarkersMap.has(clusterKey)) {
        nodesToCreate.push({ type: 'cluster', key: clusterKey, node });
      }
    } else {
      const prop = node.item;
      const isSelected =
        props.selectedProperty && props.selectedProperty.propertyId === prop.propertyId;
      const propKey = `prop_${prop.propertyId}_${isSelected ? 'selected' : 'normal'}`;
      nextMarkerKeys.add(propKey);

      if (!activePropertyMarkersMap.has(propKey)) {
        nodesToCreate.push({ type: 'prop', key: propKey, prop, isSelected });
      }
    }
  });

  // 3. 🧹 [1단계: 동기식 즉시 삭제] 필터 범위 벗어난 기존 마커 0ms 내 즉시 제거
  activePropertyMarkersMap.forEach((marker, key) => {
    if (!nextMarkerKeys.has(key)) {
      marker.setMap(null);
      activePropertyMarkersMap.delete(key);
    }
  });

  // 4. ⚡ [2단계: 비동기 시분할 Lazy Chunking] 신규 마커 25개씩 프레임 분할 생성
  const chunkSize = 25;
  let currentIndex = 0;

  const processNextChunk = () => {
    const end = Math.min(currentIndex + chunkSize, nodesToCreate.length);

    for (let i = currentIndex; i < end; i++) {
      const task = nodesToCreate[i];
      if (task.type === 'cluster') {
        if (!activePropertyMarkersMap.has(task.key)) {
          const clusterMarker = new window.naver.maps.Marker({
            position: new window.naver.maps.LatLng(task.node.lat, task.node.lng),
            map: mapInstance.value,
            icon: {
              content: renderClusterPinHTML(task.node.count, task.node.items),
            },
          });

          window.naver.maps.Event.addListener(clusterMarker, 'click', () => {
            if (mapInstance.value) {
              mapInstance.value.morph(
                new window.naver.maps.LatLng(task.node.lat, task.node.lng),
                currentZoom + 2,
              );
            }
          });

          activePropertyMarkersMap.set(task.key, clusterMarker);
        }
      } else {
        const { prop, isSelected, key } = task;
        const oldNormalKey = `prop_${prop.propertyId}_normal`;
        const oldSelectedKey = `prop_${prop.propertyId}_selected`;
        if (activePropertyMarkersMap.has(oldNormalKey)) {
          activePropertyMarkersMap.get(oldNormalKey).setMap(null);
          activePropertyMarkersMap.delete(oldNormalKey);
        }
        if (activePropertyMarkersMap.has(oldSelectedKey)) {
          activePropertyMarkersMap.get(oldSelectedKey).setMap(null);
          activePropertyMarkersMap.delete(oldSelectedKey);
        }

        if (!activePropertyMarkersMap.has(key)) {
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

          activePropertyMarkersMap.set(key, propMarker);
        }
      }
    }

    currentIndex = end;
    if (currentIndex < nodesToCreate.length) {
      pendingRenderFrame = requestAnimationFrame(processNextChunk);
    } else {
      pendingRenderFrame = null;
    }
  };

  if (nodesToCreate.length > 0) {
    processNextChunk();
  }
};

const getAmenityMarkerKey = (amenity) =>
  `${amenity.propertyId ?? 'map'}-${amenity.amenityType}-${amenity.amenityLatitude}-${amenity.amenityLongitude}`;

const clearAmenityMarkers = () => {
  amenityMarkers.forEach(({ marker }) => marker.setMap(null));
  amenityMarkers.clear();
  expandedAmenityMarkerKeys.value = new Set();
};

const refreshAmenityMarkerContents = () => {
  if (!window.naver || !window.naver.maps) return;

  amenityMarkers.forEach(({ marker, amenity }, key) => {
    const isExpanded = expandedAmenityMarkerKeys.value.has(key);
    marker.setIcon({
      content: renderAmenityPin(amenity, isExpanded),
      anchor: new window.naver.maps.Point(0, 0),
    });
    marker.setZIndex(isExpanded ? 35 : 30);
  });
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
        content: renderAmenityPin(amenity, expandedAmenityMarkerKeys.value.has(key)),
        anchor: new window.naver.maps.Point(0, 0),
      },
    });

    window.naver.maps.Event.addListener(amenityMarker, 'click', () => {
      const nextExpandedKeys = new Set(expandedAmenityMarkerKeys.value);
      if (nextExpandedKeys.has(key)) {
        nextExpandedKeys.delete(key);
      } else {
        nextExpandedKeys.add(key);
      }
      expandedAmenityMarkerKeys.value = nextExpandedKeys;
      refreshAmenityMarkerContents();
    });

    amenityMarkers.set(key, { marker: amenityMarker, amenity });
  });

  amenityMarkers.forEach(({ marker }, key) => {
    if (!nextKeys.has(key)) {
      marker.setMap(null);
      amenityMarkers.delete(key);
      if (expandedAmenityMarkerKeys.value.has(key)) {
        const nextExpandedKeys = new Set(expandedAmenityMarkerKeys.value);
        nextExpandedKeys.delete(key);
        expandedAmenityMarkerKeys.value = nextExpandedKeys;
      }
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
  () => props.selectedProperty?.propertyId,
  (currentPropertyId, previousPropertyId) => {
    if (currentPropertyId === previousPropertyId || expandedAmenityMarkerKeys.value.size === 0)
      return;

    expandedAmenityMarkerKeys.value = new Set();
    refreshAmenityMarkerContents();
  },
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

  const filter = props.liveFilter || props.appliedFilter;
  if (!filter || filter.showIsochrone === false) return;

  let radiusMeters = 900;
  if (filter.transportMode === 'WALK') {
    let speedMetersPerMin = 75;
    if (filter.walkPace === 'SLOW') speedMetersPerMin = 58;
    if (filter.walkPace === 'FAST') speedMetersPerMin = 92;
    radiusMeters = Math.max(200, (filter.travelTime || 15) * speedMetersPerMin);
  } else {
    radiusMeters = Math.max(500, (filter.travelTime || 15) * 180);
  }

  const earthRadius = 6378137;
  const centerLat = Number(props.destination.lat || props.destination.destLatitude);
  const centerLng = Number(props.destination.lng || props.destination.destLongitude);
  const latRad = (centerLat * Math.PI) / 180;

  // 15% 여유 공간 마진
  const latOffset = (radiusMeters / earthRadius) * (180 / Math.PI) * 1.15;
  const lngOffset = (((radiusMeters / (earthRadius * Math.cos(latRad))) * 180) / Math.PI) * 1.15;

  const bounds = new window.naver.maps.LatLngBounds(
    new window.naver.maps.LatLng(centerLat - latOffset, centerLng - lngOffset),
    new window.naver.maps.LatLng(centerLat + latOffset, centerLng + lngOffset),
  );

  const currentMapBounds = mapInstance.value.getBounds();

  // 프리뷰 점선 원이 현재 지도 화면(currentMapBounds)을 벗어나는 경우에만 단방향 화면 축소(fitBounds)
  if (
    !currentMapBounds ||
    !currentMapBounds.hasLatLng(bounds.getNE()) ||
    !currentMapBounds.hasLatLng(bounds.getSW())
  ) {
    mapInstance.value.fitBounds(bounds);
  }
};

// 실시간 프리뷰 점선 원 및 목적지 변경 시 줌 자동 조율
watch(
  [
    () => props.destination,
    () => props.liveFilter,
    () => props.appliedFilter,
  ],
  () => {
    fitToIsochroneRadius();
  },
  { deep: true, immediate: true },
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
  activePropertyMarkersMap.forEach((marker) => marker.setMap(null));
  activePropertyMarkersMap.clear();
  if (activeDestMarker) {
    activeDestMarker.setMap(null);
    activeDestMarker = null;
  }
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
      :applied-filter="appliedFilter"
      :live-filter="liveFilter"
      :is-preview-mode="isPreviewMode"
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
