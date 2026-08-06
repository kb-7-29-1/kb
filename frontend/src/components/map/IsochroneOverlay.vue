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
      name: '세종대학교',
      lat: 37.5502,
      lng: 127.0731,
    }),
  },
  appliedFilter: {
    type: Object,
    default: () => ({
      showIsochrone: true,
      transportMode: 'WALK',
      travelTime: 15,
      walkPace: 'NORMAL',
      flexTime: 10,
    }),
  },
  liveFilter: {
    type: Object,
    default: () => ({
      showIsochrone: true,
      transportMode: 'WALK',
      travelTime: 15,
      walkPace: 'NORMAL',
      flexTime: 10,
    }),
  },
  isPreviewMode: {
    type: Boolean,
    default: false,
  },
});

const circlesList = ref([]);
const badgesList = ref([]);
const maskPolygonInstance = shallowRef(null);
let boundsListener = null;

// 고해상도 측지선 원형 경로 정점 생성 (128정점)
const createCirclePath = (
  centerLat,
  centerLng,
  radiusMeters,
  numPoints = 128,
) => {
  if (!window.naver || !window.naver.maps) return [];
  const points = [];
  const latRad = (centerLat * Math.PI) / 180;
  const earthRadius = 6378137;

  const latOffset = (radiusMeters / earthRadius) * (180 / Math.PI);
  const lngOffset =
    ((radiusMeters / (earthRadius * Math.cos(latRad))) * 180) / Math.PI;

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

  // 1. 기존 서클 및 뱃지 삭제
  circlesList.value.forEach((c) => c.setMap(null));
  circlesList.value = [];

  badgesList.value.forEach((b) => b.setMap(null));
  badgesList.value = [];

  // 2. 기존 마스크 폴리곤 삭제
  if (maskPolygonInstance.value) {
    maskPolygonInstance.value.setMap(null);
    maskPolygonInstance.value = null;
  }

  const af = props.appliedFilter || props.liveFilter;
  const lf = props.liveFilter;
  if (!af || !af.showIsochrone) return;

  const destLat = props.destination.lat || 37.5502;
  const destLng = props.destination.lng || 127.0731;
  const destLatLng = new window.naver.maps.LatLng(destLat, destLng);
  const earthRadius = 6378137;

  const outerBoxPath = [
    new window.naver.maps.LatLng(85, -180),
    new window.naver.maps.LatLng(85, 180),
    new window.naver.maps.LatLng(-85, 180),
    new window.naver.maps.LatLng(-85, -180),
  ];

  // ==========================================
  // 1. [현재 적용 중인 확정 이소크론 원 & 마스크]
  // 탭(도보 ↔ 대중교통)이 변경된 경우 이전 모드의 실선 원은 지도에서 완벽 제거한다.
  // ==========================================
  const isModeChanged =
    props.isPreviewMode &&
    lf &&
    af &&
    lf.transportMode !== af.transportMode;

  let outerBoundaryMeters = 900;

  if (!isModeChanged) {
    if (af.transportMode === 'WALK') {
      let speedMetersPerMin =
        af.walkPace === 'SLOW' ? 58 : af.walkPace === 'FAST' ? 92 : 75;
      outerBoundaryMeters = Math.max(
        200,
        (af.travelTime || 15) * speedMetersPerMin,
      );

      const walkCircle = new window.naver.maps.Circle({
        map: props.mapInstance,
        center: destLatLng,
        radius: outerBoundaryMeters,
        fillColor: 'transparent',
        fillOpacity: 0,
        strokeColor: '#2563eb',
        strokeWeight: 2.5,
        strokeOpacity: 0.95,
      });
      circlesList.value.push(walkCircle);

      const walkLatOffset =
        (outerBoundaryMeters / earthRadius) * (180 / Math.PI);
      const walkTopPos = new window.naver.maps.LatLng(
        destLat + walkLatOffset,
        destLng,
      );
      const walkBadge = new window.naver.maps.Marker({
        map: props.mapInstance,
        position: walkTopPos,
        icon: {
          content: `
            <div style="transform: translate(-50%, -100%); margin-top: -6px; pointer-events: none;">
              <div style="background: rgba(15, 23, 42, 0.92); backdrop-filter: blur(4px); color: #f8fafc; font-size: 11px; font-weight: 800; padding: 4px 10px; border-radius: 9999px; border: 1.5px solid #2563eb; box-shadow: 0 4px 12px rgba(0,0,0,0.25); white-space: nowrap; display: flex; items-center; gap: 4px;">
                <span>🚫 도보 ${af.travelTime}분 초과는 제외할게요</span>
              </div>
            </div>
          `,
          anchor: new window.naver.maps.Point(0, 0),
        },
      });
      badgesList.value.push(walkBadge);
    } else {
      const travelTime = af.travelTime || 15;
      const flexTime = af.flexTime != null ? af.flexTime : 10;
      const transitMaxRadius = Math.max(500, travelTime * 180);
      const minTime = Math.max(0, travelTime - flexTime);
      const transitBaseRadius = Math.max(200, minTime * 180);

      outerBoundaryMeters = transitMaxRadius;

      const outerCircle = new window.naver.maps.Circle({
        map: props.mapInstance,
        center: destLatLng,
        radius: transitMaxRadius,
        fillColor: 'transparent',
        fillOpacity: 0,
        strokeColor: '#4767f7',
        strokeWeight: 2.5,
        strokeOpacity: 0.95,
      });

      const innerCircle = new window.naver.maps.Circle({
        map: props.mapInstance,
        center: destLatLng,
        radius: transitBaseRadius,
        fillColor: 'transparent',
        fillOpacity: 0,
        strokeColor: '#f59e0b',
        strokeWeight: 2.5,
        strokeOpacity: 0.95,
      });

      circlesList.value.push(outerCircle);
      circlesList.value.push(innerCircle);

      const outerLatOffset =
        (transitMaxRadius / earthRadius) * (180 / Math.PI);
      const outerTopPos = new window.naver.maps.LatLng(
        destLat + outerLatOffset,
        destLng,
      );
      const outerBadge = new window.naver.maps.Marker({
        map: props.mapInstance,
        position: outerTopPos,
        icon: {
          content: `
            <div style="transform: translate(-50%, -100%); margin-top: -6px; pointer-events: none;">
              <div style="background: rgba(15, 23, 42, 0.92); backdrop-filter: blur(4px); color: #f8fafc; font-size: 11px; font-weight: 800; padding: 4px 10px; border-radius: 9999px; border: 1.5px solid #4767f7; box-shadow: 0 4px 12px rgba(0,0,0,0.25); white-space: nowrap; display: flex; items-center; gap: 4px;">
                <span>🚫 대중교통 ${travelTime}분 초과 제외 (현재 적용)</span>
              </div>
            </div>
          `,
          anchor: new window.naver.maps.Point(0, 0),
        },
      });
      badgesList.value.push(outerBadge);

      if (minTime > 0 && transitBaseRadius < transitMaxRadius - 100) {
        const innerLatOffset =
          (transitBaseRadius / earthRadius) * (180 / Math.PI);
        const innerTopPos = new window.naver.maps.LatLng(
          destLat + innerLatOffset,
          destLng,
        );
        const innerBadge = new window.naver.maps.Marker({
          map: props.mapInstance,
          position: innerTopPos,
          icon: {
            content: `
              <div style="transform: translate(-50%, -100%); margin-top: -6px; pointer-events: none;">
                <div style="background: rgba(254, 243, 199, 0.95); backdrop-filter: blur(4px); color: #92400e; font-size: 11px; font-weight: 800; padding: 4px 10px; border-radius: 9999px; border: 1.5px solid #f59e0b; box-shadow: 0 4px 12px rgba(0,0,0,0.15); white-space: nowrap; display: flex; items-center; gap: 4px;">
                  <span>⚠️ ${minTime}분 미만은 제외할게요</span>
                </div>
              </div>
            `,
            anchor: new window.naver.maps.Point(0, 0),
          },
        });
        badgesList.value.push(innerBadge);
      }
    }
  }

  // 2. 미리보기 권역 반경 미리 연산 (마스크 구멍 가림 방지)
  let pTransitMaxRadius = 0;
  let pTransitBaseRadius = 0;
  let pMinTime = 0;
  let pTravelTime = 15;
  let previewRadius = 0;

  if (props.isPreviewMode && lf) {
    if (lf.transportMode === 'WALK') {
      let speedMetersPerMin =
        lf.walkPace === 'SLOW' ? 58 : lf.walkPace === 'FAST' ? 92 : 75;
      previewRadius = Math.max(200, (lf.travelTime || 15) * speedMetersPerMin);
    } else {
      pTravelTime = lf.travelTime || 15;
      const pFlexTime = lf.flexTime != null ? lf.flexTime : 10;
      pTransitMaxRadius = Math.max(500, pTravelTime * 180);
      pMinTime = Math.max(0, pTravelTime - pFlexTime);
      pTransitBaseRadius = Math.max(200, pMinTime * 180);
    }
  }

  // 🌑 외부 마스크 폴리곤 (미리보기 점선 원이 어둡게 가려지지 않도록 최대 반경으로 구멍 생성)
  const maxMaskRadius =
    props.isPreviewMode && lf
      ? Math.max(outerBoundaryMeters, previewRadius, pTransitMaxRadius)
      : outerBoundaryMeters;

  const holeCirclePath = createCirclePath(destLat, destLng, maxMaskRadius, 128);
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

  // ==========================================
  // 2. [독립 미리보기 전용 초선명 네온 점선 원 (슬라이더 조절 중 추가 등장)]
  // ==========================================
  if (props.isPreviewMode && lf) {
    if (lf.transportMode === 'WALK') {
      const previewWalkCircle = new window.naver.maps.Circle({
        map: props.mapInstance,
        center: destLatLng,
        radius: previewRadius,
        fillColor: 'transparent',
        fillOpacity: 0,
        strokeColor: '#00d2ff', // 초선명 네온 시안 블루
        strokeWeight: 4,
        strokeStyle: 'dash font-bold',
        strokeOpacity: 1.0,
      });
      circlesList.value.push(previewWalkCircle);

      const pLatOffset = (previewRadius / earthRadius) * (180 / Math.PI);
      const pTopPos = new window.naver.maps.LatLng(
        destLat + pLatOffset,
        destLng,
      );
      const pBadge = new window.naver.maps.Marker({
        map: props.mapInstance,
        position: pTopPos,
        icon: {
          content: `
            <div style="transform: translate(-50%, -100%); margin-top: -6px; pointer-events: none;">
              <div style="background: rgba(3, 105, 161, 0.95); backdrop-filter: blur(4px); color: #f0f9ff; font-size: 11px; font-weight: 800; padding: 4px 10px; border-radius: 9999px; border: 2px dashed #38bdf8; box-shadow: 0 4px 14px rgba(0,0,0,0.35); white-space: nowrap; display: flex; align-items: center; gap: 4px;">
                <span>⚡ 도보 ${lf.travelTime}분 (미리보기 점선)</span>
              </div>
            </div>
          `,
          anchor: new window.naver.maps.Point(0, 0),
        },
      });
      badgesList.value.push(pBadge);
    } else {
      const pTravelTime = lf.travelTime || 15;
      const pFlexTime = lf.flexTime != null ? lf.flexTime : 10;
      const pTransitMaxRadius = Math.max(500, pTravelTime * 180);
      const pMinTime = Math.max(0, pTravelTime - pFlexTime);
      const pTransitBaseRadius = Math.max(200, pMinTime * 180);

      const previewOuterCircle = new window.naver.maps.Circle({
        map: props.mapInstance,
        center: destLatLng,
        radius: pTransitMaxRadius,
        fillColor: 'transparent',
        fillOpacity: 0,
        strokeColor: '#00d2ff', // 초선명 네온 시안 블루 점선
        strokeWeight: 4,
        strokeStyle: 'dash font-bold',
        strokeOpacity: 1.0,
      });

      const previewInnerCircle = new window.naver.maps.Circle({
        map: props.mapInstance,
        center: destLatLng,
        radius: pTransitBaseRadius,
        fillColor: 'transparent',
        fillOpacity: 0,
        strokeColor: '#ff9100', // 초선명 네온 오렌지/호박색 점선
        strokeWeight: 4,
        strokeStyle: 'dash font-bold',
        strokeOpacity: 1.0,
      });

      circlesList.value.push(previewOuterCircle);
      circlesList.value.push(previewInnerCircle);

      // 외접원 미리보기 뱃지
      const pOuterLatOffset =
        (pTransitMaxRadius / earthRadius) * (180 / Math.PI);
      const pOuterTopPos = new window.naver.maps.LatLng(
        destLat + pOuterLatOffset,
        destLng,
      );
      const pOuterBadge = new window.naver.maps.Marker({
        map: props.mapInstance,
        position: pOuterTopPos,
        icon: {
          content: `
            <div style="transform: translate(-50%, -100%); margin-top: -6px; pointer-events: none; z-index: 999;">
              <div style="background: rgba(3, 105, 161, 0.95); backdrop-filter: blur(4px); color: #ffffff; font-size: 11px; font-weight: 900; padding: 5px 12px; border-radius: 9999px; border: 2px dashed #00d2ff; box-shadow: 0 4px 16px rgba(0,210,255,0.5); white-space: nowrap; display: flex; align-items: center; gap: 4px;">
                <span>⚡ 대중교통 ${pTravelTime}분 (미리보기 점선)</span>
              </div>
            </div>
          `,
          anchor: new window.naver.maps.Point(0, 0),
        },
      });
      badgesList.value.push(pOuterBadge);

      // 내접원 미리보기 뱃지
      if (pMinTime > 0 && pTransitBaseRadius < pTransitMaxRadius - 100) {
        const pInnerLatOffset =
          (pTransitBaseRadius / earthRadius) * (180 / Math.PI);
        const pInnerTopPos = new window.naver.maps.LatLng(
          destLat + pInnerLatOffset,
          destLng,
        );
        const pInnerBadge = new window.naver.maps.Marker({
          map: props.mapInstance,
          position: pInnerTopPos,
          icon: {
            content: `
              <div style="transform: translate(-50%, -100%); margin-top: -6px; pointer-events: none; z-index: 999;">
                <div style="background: rgba(180, 83, 9, 0.95); backdrop-filter: blur(4px); color: #ffffff; font-size: 11px; font-weight: 900; padding: 5px 12px; border-radius: 9999px; border: 2px dashed #ff9100; box-shadow: 0 4px 16px rgba(255,145,0,0.5); white-space: nowrap; display: flex; align-items: center; gap: 4px;">
                  <span>⚡ ${pMinTime}분 미만 (미리보기 점선)</span>
                </div>
              </div>
            `,
            anchor: new window.naver.maps.Point(0, 0),
          },
        });
        badgesList.value.push(pInnerBadge);
      }
    }
  }
};

// 지도 인스턴스 준비 및 props 변경 감시
watch(
  [
    () => props.mapInstance,
    () => props.destination,
    () => props.appliedFilter,
    () => props.liveFilter,
    () => props.isPreviewMode,
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
  badgesList.value.forEach((b) => b.setMap(null));
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
