<script setup>
import { ref, shallowRef, markRaw, watch, onUnmounted } from 'vue';

const props = defineProps({
  mapInstance: {
    type: Object,
    default: null,
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
    default: 15,
  },
  walkPace: {
    type: String,
    default: 'NORMAL', // 'SLOW' | 'NORMAL' | 'FAST'
  },
  flexTime: {
    type: Number,
    default: 10,
  },
});

const circlesList = ref([]);
const maskPolygonInstance = shallowRef(null);
let boundsListener = null;

// 고해상도 측지선 원형 경로 정점 생성 (128정점 - 확대 시 찌그러짐 방지)
const createCirclePath = (
  centerLat,
  centerLng,
  radiusMeters,
  numPoints = 128,
) => {
  if (!window.naver || !window.naver.maps) return [];
  const points = [];
  const latRad = (centerLat * Math.PI) / 180;
  const earthRadius = 6378137; // 지구 반지름 (m)

  const latOffset = (radiusMeters / earthRadius) * (180 / Math.PI);
  const lngOffset =
    ((radiusMeters / (earthRadius * Math.cos(latRad))) * 180) / Math.PI;

  // Polygon Hole 구멍 생성을 위해 역순(Counter-Clockwise) 생성
  for (let i = numPoints; i >= 0; i--) {
    const angle = (i * 2 * Math.PI) / numPoints;
    const pLat = centerLat + latOffset * Math.sin(angle);
    const pLng = centerLng + lngOffset * Math.cos(angle);
    points.push(new window.naver.maps.LatLng(pLat, pLng));
  }
  return points;
};

// 이소크론 원형 테두리 서클 및 외부 암영 마스크 폴리곤 렌더링
const updateIsochroneOverlays = () => {
  if (!props.mapInstance || !window.naver || !window.naver.maps) return;

  // 1. 기존 서클 삭제
  circlesList.value.forEach((c) => c.setMap(null));
  circlesList.value = [];

  // 2. 기존 마스크 폴리곤 삭제
  if (maskPolygonInstance.value) {
    maskPolygonInstance.value.setMap(null);
    maskPolygonInstance.value = null;
  }

  if (!props.showIsochrone) return;

  const destLatLng = new window.naver.maps.LatLng(
    props.destination.lat || 37.5502,
    props.destination.lng || 127.0731,
  );

  // 전역 좌표계 기반 고정 외곽 사각형 (지도 확대/축소 시 재계산 파괴 방지)
  const outerBoxPath = [
    new window.naver.maps.LatLng(85, -180),
    new window.naver.maps.LatLng(85, 180),
    new window.naver.maps.LatLng(-85, 180),
    new window.naver.maps.LatLng(-85, -180),
  ];

  let outerBoundaryMeters = 900;

  if (props.transportMode === 'WALK') {
    // 🚶 [도보 모드]: 원 1개 (테두리만 선명, 내부 색상 없음)
    let speedMetersPerMin = 75; // 보통 걸음 (~4.5 km/h)
    if (props.walkPace === 'SLOW') speedMetersPerMin = 58; // 천천히 (~3.5 km/h)
    if (props.walkPace === 'FAST') speedMetersPerMin = 92; // 빠른 걸음 (~5.5 km/h)

    outerBoundaryMeters = Math.max(200, props.travelTime * speedMetersPerMin);

    const walkCircle = new window.naver.maps.Circle({
      map: props.mapInstance,
      center: destLatLng,
      radius: outerBoundaryMeters,
      fillColor: 'transparent',
      fillOpacity: 0,
      strokeColor: '#10b981', // Emerald
      strokeWeight: 3,
      strokeOpacity: 0.95,
    });
    circlesList.value.push(walkCircle);
  } else {
    // 🚌 [대중교통 모드]: 원 2개 (내접원 + 외접원 동심원 구조, 테두리만 선명)
    const transitBaseRadius = Math.max(500, props.travelTime * 180); // 내접원
    const transitMaxRadius = Math.max(
      transitBaseRadius + 200,
      (props.travelTime + props.flexTime) * 180,
    ); // 외접원

    outerBoundaryMeters = transitMaxRadius;

    // 1) 외접원 (Outer Circle - 최대 권역 테두리)
    const outerCircle = new window.naver.maps.Circle({
      map: props.mapInstance,
      center: destLatLng,
      radius: transitMaxRadius,
      fillColor: 'transparent',
      fillOpacity: 0,
      strokeColor: '#6366f1', // Indigo
      strokeWeight: 3,
      strokeStyle: 'dash font-bold',
      strokeOpacity: 0.9,
    });

    // 2) 내접원 (Inner Circle - 기본 권역 테두리 & 외접원 바깥과 동일한 베이스 톤)
    const innerCircle = new window.naver.maps.Circle({
      map: props.mapInstance,
      center: destLatLng,
      radius: transitBaseRadius,
      fillColor: '#0f172a', // 외접원 바깥 암영과 동일한 슬레이트 베이스 색상
      fillOpacity: 0.25, // 반투명 음영
      strokeColor: '#2563eb', // Blue
      strokeWeight: 3,
      strokeOpacity: 0.95,
    });

    circlesList.value.push(outerCircle);
    circlesList.value.push(innerCircle);
  }

  // 🌑 외부 미포함 영역만 55% 어둡게 가리는 내이티브 Polygon Hole 마스크
  const holeCirclePath = createCirclePath(
    destLatLng.lat(),
    destLatLng.lng(),
    outerBoundaryMeters,
    128,
  );

  maskPolygonInstance.value = markRaw(
    new window.naver.maps.Polygon({
      map: props.mapInstance,
      paths: [outerBoxPath, holeCirclePath],
      fillColor: '#0f172a',
      fillOpacity: 0.55,
      strokeWeight: 0,
      clickable: false,
    }),
  );
};

// 지도 인스턴스 준비 및 props 변경 감시
watch(
  [
    () => props.mapInstance,
    () => props.destination,
    () => props.showIsochrone,
    () => props.transportMode,
    () => props.travelTime,
    () => props.walkPace,
    () => props.flexTime,
  ],
  ([newMap]) => {
    if (newMap && window.naver && window.naver.maps) {
      updateIsochroneOverlays();
    }
  },
  { immediate: true },
);

onUnmounted(() => {
  circlesList.value.forEach((c) => c.setMap(null));
  if (maskPolygonInstance.value) {
    maskPolygonInstance.value.setMap(null);
  }
  if (boundsListener && window.naver && window.naver.maps) {
    window.naver.maps.Event.removeListener(boundsListener);
  }
});
</script>

<template>
  <!-- 렌더링 전용 렌더리스 컴포넌트 (DOM 미출력) -->
  <div class="hidden"></div>
</template>
