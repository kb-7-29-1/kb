<script setup>
import {
  createApp,
  ref,
  shallowRef,
  computed,
  onMounted,
  onUnmounted,
  watch,
} from 'vue';
import IsochroneOverlay from './IsochroneOverlay.vue';
import AmenityPin from './AmenityPin.vue';
import { reverseGeocodeCoord } from '@/utils/geo';
import {
  getClusteredMarkers,
  renderClusterPinHTML,
  renderPropertyPinHTML,
  renderDestinationPinHTML,
} from '@/utils/mapClustering';
import { getFeaturedLoanPropertyIds } from '@/utils/loanChip.js';
import { isTouchEvent, isMousePointer } from '@/utils/deviceUtils';
import { useAuthStore } from '@/stores/useAuthStore.js';
import onboardingApi from '@/api/onboardingApi.js';
import {
  getRecentDestinations,
  findMatchingDestination,
} from '@/utils/recentDestinations.js';
import {
  renderUnsupportedDistrictGeoJson,
  clearUnsupportedDistrictGeoJson,
} from '@/utils/districtPolygonOverlay.js';
import {
  renderHybridRouteOverlays,
  clearHybridRouteOverlays,
} from '@/utils/hybridRouteOverlay.js';

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
  // 백엔드가 TMAP/DB 캐시에서 반환한 선택 매물의 안전 경로
  safetyRoute: {
    type: Object,
    default: null,
  },
  // "경로 자세히 보기" 토글 ON 시 표시할 CCTV/가로등/파출소 원본 좌표 목록
  routeFacilities: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits([
  'select-property',
  'change-destination',
  'bounds-change',
]);

const mapInstance = shallowRef(null);
const zoomLevel = ref(15);
const markersMap = ref([]);
const amenityMarkers = new Map();
const expandedAmenityMarkerKeys = ref(new Set());
let resizeObserver = null;

// 길(경로)을 보는 동안에는 겉에 어둡게 하는 이소크론 원&마스크를 숨기고,
// 모바일/PC 상세정보에서 X 버튼을 눌러 경로를 닫으면 이소크론 원이 즉시 다시 나타납니다.
const showIsochroneOverlay = computed(() => {
  return !props.selectedProperty;
});

// 편의시설 마커 핀 (순수 초고속 HTML 스트링 템플릿)
const amenityIcons = {
  1: '🏪',
  2: '☕',
  3: '🧺',
  4: '🍔',
  5: '🛍️',
  6: '💄',
  7: '🛒',
};

const renderAmenityPin = (amenity, isExpanded = false) => {
  const icon = amenityIcons[amenity.amenityType] ?? '📍';
  let walkingInfo = '';
  const minutes = amenity.walkTimeMinutes;
  const distance = amenity.distanceMeters;

  if (minutes != null || distance != null) {
    if (distance == null) walkingInfo = `도보 ${minutes}분`;
    else if (minutes == null) walkingInfo = `${distance}m`;
    else walkingInfo = `도보 ${minutes}분 · ${distance}m`;
  }

  const detailBadge =
    isExpanded && walkingInfo
      ? `<span class="amenity-detail shrink-0 rounded-md bg-white/20 px-1.5 py-0.5 text-[10px] font-bold text-white">${walkingInfo}</span>`
      : '';

  const expandedClass = isExpanded ? 'gap-1.5 px-3.5' : 'gap-1.5';

  return `
    <div class="group inline-flex w-max flex-col items-center cursor-pointer transform -translate-x-1/2 -translate-y-full transition-transform duration-200 ease-out hover:-translate-y-[calc(100%+4px)]" title="${amenity.amenityName || ''}">
      <div class="relative z-10 flex h-[34px] w-max min-w-10 items-center whitespace-nowrap rounded-full border border-violet-600 bg-violet-600 px-2.5 text-xs font-bold text-white shadow-lg transition-all duration-200 group-hover:bg-violet-700 group-hover:shadow-xl ${expandedClass}">
        <span class="shrink-0">${icon}</span>
        <span class="shrink-0">${amenity.amenityName || ''}</span>
        ${detailBadge}
      </div>
      <div class="relative -mt-1.5 z-0 h-2.5 w-2.5 rotate-45 bg-violet-600 transition-colors duration-200 group-hover:bg-violet-700"></div>
      <div class="mt-1 h-2 w-6 rounded-full bg-black/20 blur-xs"></div>
    </div>
  `;
};

const activePropertyMarkersMap = new Map();
let activeDestMarker = null;
const destinationMarkers = new Set();
let pendingRenderFrame = null;
let safetyRoutePolyline = null;
let safetyRouteScoreLabel = null;
let lastSafetyRouteKey = '';
let selectedContextFitFrame = null;
let routeFacilityOverlays = [];

// SafetyScoreCalculator.java의 반경 상수와 반드시 일치시켜야 함
const ROUTE_FACILITY_COLOR = {
  CCTV: '#2a60f7',
  STREET_LIGHT: '#f5b301',
  POLICE: '#7c3aed',
};
const ROUTE_FACILITY_RADIUS = {
  CCTV: 50,
  STREET_LIGHT: 15,
  POLICE: 500,
};
const ROUTE_FACILITY_LABEL = {
  CCTV: '📷 CCTV',
  STREET_LIGHT: '💡 가로등',
  POLICE: '👮 파출소',
};
const GRADE_COLOR = {
  SAFE: '#22a06b',
  WARNING: '#e69a1d',
  DANGER: '#dc4b5d',
};

const getSelectedContextFitMargin = () => {
  const mapElement = document.getElementById('naver-map-container');
  if (!mapElement) return { top: 88, right: 28, bottom: 28, left: 28 };

  const { width, height } = mapElement.getBoundingClientRect();
  const isDesktop = window.matchMedia('(min-width: 1280px)').matches;

  if (isDesktop) {
    // PC 우측 상세 패널에 가려지지 않도록 여백 확보
    const detailPanel = document.querySelector('.property-detail-panel');
    const panelWidth = detailPanel?.getBoundingClientRect().width ?? 0;

    return {
      // 패널 너비 + 마커·경로가 패널 경계에 닿지 않도록 여백 함께 확보
      top: 104,
      right: Math.min(panelWidth + 200, Math.max(200, width * 0.62)),
      bottom: 64,
      left: 52,
    };
  }

  // 모바일 하단 매물/상세 시트와 헤더에 가리지 않는 범위로 맞춤
  const mobilePanel = document.querySelector('.mobile-aside-panel');
  const panelHeight = mobilePanel?.getBoundingClientRect().height ?? height / 3;

  return {
    top: 118,
    right: 24,
    bottom: Math.min(panelHeight + 20, Math.max(80, height * 0.7)),
    left: 24,
  };
};

const fitToSelectedPropertyContext = () => {
  if (
    !mapInstance.value ||
    !window.naver ||
    !window.naver.maps ||
    !props.selectedProperty
  ) {
    return;
  }

  const points = [];
  const addPoint = (latitude, longitude) => {
    const lat = Number(latitude);
    const lng = Number(longitude);
    if (Number.isFinite(lat) && Number.isFinite(lng)) {
      points.push(new window.naver.maps.LatLng(lat, lng));
    }
  };

  // 선택 매물, 목적지 포함
  addPoint(props.selectedProperty.latitude, props.selectedProperty.longitude);
  addPoint(
    props.destination?.lat ?? props.destination?.destLatitude,
    props.destination?.lng ?? props.destination?.destLongitude,
  );

  // 귀갓길 경로, 편의시설 마커 한 화면에 포함
  (props.safetyRoute?.routePoints ?? []).forEach((point) => {
    addPoint(point.latitude ?? point.lat, point.longitude ?? point.lng);
  });
  props.amenities.forEach((amenity) => {
    addPoint(amenity.amenityLatitude, amenity.amenityLongitude);
  });

  if (points.length < 2) return;

  const bounds = new window.naver.maps.LatLngBounds(points[0], points[0]);
  points.slice(1).forEach((point) => bounds.extend(point));

  mapInstance.value.panToBounds(
    bounds,
    { duration: 650, easing: 'easeOutCubic' },
    getSelectedContextFitMargin(),
  );
};

const scheduleSelectedPropertyContextFit = () => {
  if (selectedContextFitFrame) cancelAnimationFrame(selectedContextFitFrame);
  selectedContextFitFrame = requestAnimationFrame(() => {
    selectedContextFitFrame = null;
    fitToSelectedPropertyContext();
  });
};

let activeHybridRouteState = { polylines: [], markers: [], lastRouteKey: '' };

const clearSafetyRoutePolyline = () => {
  if (safetyRoutePolyline) {
    safetyRoutePolyline.setMap(null);
    safetyRoutePolyline = null;
  }
  if (safetyRouteScoreLabel) {
    safetyRouteScoreLabel.setMap(null);
    safetyRouteScoreLabel = null;
  }
  clearHybridRouteOverlays(activeHybridRouteState);
  activeHybridRouteState = { polylines: [], markers: [], lastRouteKey: '' };
  lastSafetyRouteKey = '';
};

const renderSafetyRoute = () => {
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  // 🚌 [발할라/호퍼 하이브리드 라우트 및 미지원 구역 분할 렌더러 우선 실행]
  clearSafetyRoutePolyline();
  activeHybridRouteState = renderHybridRouteOverlays({
    mapInstance: mapInstance.value,
    safetyRoute: props.safetyRoute,
    gradeColors: GRADE_COLOR,
  });
  if (activeHybridRouteState.polylines.length > 0) {
    scheduleSelectedPropertyContextFit();
    return;
  }

  // 🚶 [기존 단일 안전 경로 및 미지원 구역 렌더링 원본 로직 100% 보존]
  const rawPoints = props.safetyRoute?.routePoints;
  if (!Array.isArray(rawPoints) || rawPoints.length < 2) {
    clearSafetyRoutePolyline();
    return;
  }

  const points = rawPoints
    .map((point) => ({
      lat: Number(point.latitude),
      lng: Number(point.longitude),
    }))
    .filter(
      (point) =>
        Number.isFinite(point.lat) &&
        Number.isFinite(point.lng) &&
        point.lat >= -90 &&
        point.lat <= 90 &&
        point.lng >= -180 &&
        point.lng <= 180,
    );

  if (points.length < 2) {
    clearSafetyRoutePolyline();
    return;
  }

  const first = points[0];
  const last = points[points.length - 1];
  const routeKey = `${props.safetyRoute?.routeId || 'route'}_${points.length}_${first.lat}_${first.lng}_${last.lat}_${last.lng}`;
  if (safetyRoutePolyline && routeKey === lastSafetyRouteKey) return;

  if (safetyRoutePolyline) {
    safetyRoutePolyline.setMap(null);
  }
  if (safetyRouteScoreLabel) {
    safetyRouteScoreLabel.setMap(null);
    safetyRouteScoreLabel = null;
  }

  const path = points.map(
    (point) => new window.naver.maps.LatLng(point.lat, point.lng),
  );

  const score = props.safetyRoute?.safetyScore;
  const grade = props.safetyRoute?.safetyGrade;
  const isDataMissing = score == null;
  const color = isDataMissing ? '#94a3b8' : GRADE_COLOR[grade] || '#4058f5';

  safetyRoutePolyline = new window.naver.maps.Polyline({
    map: mapInstance.value,
    path,
    strokeColor: color,
    strokeWeight: isDataMissing ? 6 : 7,
    strokeOpacity: isDataMissing ? 0.82 : 0.92,
    strokeStyle: isDataMissing ? 'dash' : 'solid',
    zIndex: 18,
  });
  lastSafetyRouteKey = routeKey;

  const midPoint = path[Math.floor(path.length / 2)];
  const labelContent = isDataMissing
    ? `<div style="background:#64748b;color:#fff;font-size:11px;font-weight:700;padding:3px 8px;border-radius:999px;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,0.35);border:1.5px solid rgba(255,255,255,0.85);display:flex;align-items:center;gap:3px;"><span style="font-size:9.5px;">🛡️</span> 데이터 부족</div>`
    : `<div style="background:${color};color:#fff;font-size:12px;font-weight:800;padding:3px 9px;border-radius:999px;white-space:nowrap;box-shadow:0 2px 5px rgba(0,0,0,0.3);border:1.5px solid rgba(255,255,255,0.85);">${score}점</div>`;

  safetyRouteScoreLabel = new window.naver.maps.Marker({
    map: mapInstance.value,
    position: midPoint,
    icon: {
      content: labelContent,
      anchor: new window.naver.maps.Point(isDataMissing ? 38 : 24, 12),
    },
    zIndex: 19,
  });

  // 경로·목적지·선택 매물·편의시설을 포함하도록 카메라 조정
  scheduleSelectedPropertyContextFit();
};

const clearRouteFacilityOverlays = () => {
  routeFacilityOverlays.forEach(({ circle, dot }) => {
    circle.setMap(null);
    dot.setMap(null);
  });
  routeFacilityOverlays = [];
};

const renderRouteFacilities = () => {
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  clearRouteFacilityOverlays();

  const facilities = props.routeFacilities;
  if (!Array.isArray(facilities) || facilities.length === 0) return;

  facilities.forEach((facility) => {
    const lat = Number(facility.latitude);
    const lng = Number(facility.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

    const color = ROUTE_FACILITY_COLOR[facility.facilityType] || '#94a3b8';
    const radius = ROUTE_FACILITY_RADIUS[facility.facilityType] || 50;
    const labelText =
      ROUTE_FACILITY_LABEL[facility.facilityType] || facility.facilityType;
    const unitCount = Math.max(1, Number(facility.facilityCount) || 1);
    const countBadge = unitCount > 1 ? ` ×${unitCount}` : '';

    const circle = new window.naver.maps.Circle({
      map: mapInstance.value,
      center: new window.naver.maps.LatLng(lat, lng),
      radius,
      strokeWeight: 1.5,
      strokeColor: color,
      strokeOpacity: 0.8,
      fillColor: color,
      fillOpacity: 0.15,
      zIndex: 15,
    });

    const dot = new window.naver.maps.Marker({
      map: mapInstance.value,
      position: new window.naver.maps.LatLng(lat, lng),
      icon: {
        content: `<div style="background:${color};color:#fff;font-size:9px;font-weight:900;padding:1.5px 5px;border-radius:4px;white-space:nowrap;box-shadow:0 1.5px 4px rgba(0,0,0,0.35);border:1px solid #fff;">${labelText}${countBadge}</div>`,
        anchor: new window.naver.maps.Point(20, 8),
      },
      zIndex: 16,
    });

    routeFacilityOverlays.push({ circle, dot });
  });
};

const clearDestinationMarkers = () => {
  destinationMarkers.forEach((marker) => marker.setMap(null));
  destinationMarkers.clear();
  activeDestMarker = null;
};

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
    clearDestinationMarkers();
    activeDestMarker = new window.naver.maps.Marker({
      position: destLatLng,
      map: mapInstance.value,
      icon: {
        content: renderDestinationPinHTML(props.destination),
      },
    });
    activeDestMarker._key = destKey;
    destinationMarkers.add(activeDestMarker);
  }

  // 2. 🏢 / 🏠 매물 및 클러스터 마커 렌더링 준비 (Diffing)
  // 선택 매물은 클러스터 계산에서 제외, 개별 마커로 유지
  const selectedPropertyId = Number(props.selectedProperty?.propertyId);
  const hasSelectedProperty = Number.isFinite(selectedPropertyId);
  const featuredLoanSet = getFeaturedLoanPropertyIds(props.properties, 3);
  const propertiesForClustering = hasSelectedProperty
    ? props.properties.filter(
        (property) => Number(property.propertyId) !== selectedPropertyId,
      )
    : props.properties;
  const clusteredNodes = getClusteredMarkers(
    propertiesForClustering,
    currentZoom,
    bounds,
  );

  if (
    hasSelectedProperty &&
    props.selectedProperty?.latitude != null &&
    props.selectedProperty?.longitude != null
  ) {
    clusteredNodes.push({
      isCluster: false,
      item: props.selectedProperty,
    });
  }
  const nextMarkerKeys = new Set();
  const nodesToCreate = [];

  clusteredNodes.forEach((node) => {
    if (node.isCluster) {
      const clusterKey = `cluster_${node.lat.toFixed(5)}_${node.lng.toFixed(5)}_${node.count}_${hasSelectedProperty ? 'dimmed' : 'normal'}`;
      nextMarkerKeys.add(clusterKey);
      if (!activePropertyMarkersMap.has(clusterKey)) {
        nodesToCreate.push({ type: 'cluster', key: clusterKey, node });
      }
    } else {
      const prop = node.item;
      const isSelected =
        Number(props.selectedProperty?.propertyId) === Number(prop.propertyId);
      const isFeaturedLoan = featuredLoanSet.has(prop.propertyId);
      const propKey = `prop_${prop.propertyId}_${prop.safetyScore ?? 'null'}_${isFeaturedLoan ? 'loan' : 'normal'}_${isSelected ? 'selected' : hasSelectedProperty ? 'dimmed' : 'normal'}`;
      nextMarkerKeys.add(propKey);

      if (!activePropertyMarkersMap.has(propKey)) {
        nodesToCreate.push({
          type: 'prop',
          key: propKey,
          prop,
          isSelected,
          isFeaturedLoan,
        });
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
            position: new window.naver.maps.LatLng(
              task.node.lat,
              task.node.lng,
            ),
            map: mapInstance.value,
            zIndex: 8,
            icon: {
              content: renderClusterPinHTML(
                task.node.count,
                hasSelectedProperty,
              ),
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
        const { prop, isSelected, isFeaturedLoan, key } = task;
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
            position: new window.naver.maps.LatLng(
              prop.latitude,
              prop.longitude,
            ),
            map: mapInstance.value,
            // 선택한 매물만 강조, 나머지 매물은 경로 아래에 둠
            zIndex: isSelected ? 40 : 10,
            icon: {
              content: renderPropertyPinHTML(
                prop,
                isSelected,
                hasSelectedProperty,
                isFeaturedLoan,
              ),
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
    marker.setZIndex(isExpanded ? 55 : 50);
  });
};

const renderAmenityMarkers = () => {
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  const nextKeys = new Set();
  props.amenities.forEach((amenity) => {
    if (amenity.amenityLatitude == null || amenity.amenityLongitude == null)
      return;

    const key = getAmenityMarkerKey(amenity);
    nextKeys.add(key);
    if (amenityMarkers.has(key)) return;

    const amenityMarker = new window.naver.maps.Marker({
      position: new window.naver.maps.LatLng(
        amenity.amenityLatitude,
        amenity.amenityLongitude,
      ),
      map: mapInstance.value,
      // 선택 매물에 가려지지 않도록 편의시설을 위에 표시
      zIndex: 50,
      icon: {
        content: renderAmenityPin(
          amenity,
          expandedAmenityMarkerKeys.value.has(key),
        ),
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
        renderDebugViewportRectangle();
        if (mapInstance.value) {
          const bounds = mapInstance.value.getBounds();
          const center = mapInstance.value.getCenter();
          if (bounds && center) {
            const sw = bounds.getSW();
            const ne = bounds.getNE();
            const centerLat = center.lat();
            const centerLng = center.lng();
            // 백엔드 DB 재호출 없이 프론트엔드 카메라 뷰포트 필터링 조건만 완화하는 1.2배 버퍼 좌표 계산
            const latSpan = (ne.lat() - sw.lat()) * 1.2;
            const lngSpan = (ne.lng() - sw.lng()) * 1.2;

            emit('bounds-change', {
              swLat: centerLat - latSpan / 2,
              swLng: centerLng - lngSpan / 2,
              neLat: centerLat + latSpan / 2,
              neLng: centerLng + lngSpan / 2,
              centerLat: centerLat,
              centerLng: centerLng,
            });
          }
        }
      });
      window.naver.maps.Event.addListener(
        mapInstance.value,
        'zoom_changed',
        () => {
          if (mapInstance.value) {
            zoomLevel.value = mapInstance.value.getZoom();
          }
          renderDebugViewportRectangle();
        },
      );
      window.naver.maps.Event.addListener(
        mapInstance.value,
        'center_changed',
        () => {
          checkDistanceToDestination();
          renderDebugViewportRectangle();
        },
      );

      // PC 마우스 우클릭 (Right Click) 시 역지오코딩 & 목적지 확인 카드 팝업 (PC 마우스 전용)
      window.naver.maps.Event.addListener(
        mapInstance.value,
        'rightclick',
        (e) => {
          if (!isMousePointer(e)) return;
          handleMapRightClick(e);
        },
      );

      // 모바일 손가락 터치 롱프레스 (~500ms 꾹 누르기 & 햅틱 진동 피드백 - 모바일 터치 전용)
      window.naver.maps.Event.addListener(
        mapInstance.value,
        'mousedown',
        (e) => {
          handleLongPressStart(e);
        },
      );
      window.naver.maps.Event.addListener(
        mapInstance.value,
        'mousemove',
        (e) => {
          handleLongPressMove(e);
        },
      );
      window.naver.maps.Event.addListener(mapInstance.value, 'mouseup', () => {
        handleLongPressEnd();
      });
      window.naver.maps.Event.addListener(
        mapInstance.value,
        'dragstart',
        () => {
          handleLongPressEnd();
        },
      );

      window.naver.maps.Event.addListener(mapInstance.value, 'click', () => {
        handleLongPressEnd();
        clearPendingDestinationOverlay();
      });

      renderMarkers();
      renderAmenityMarkers();
      renderSafetyRoute();
      renderUnsupportedDistrictGeoJson(mapInstance.value);
      renderDebugViewportRectangle();
      setupResizeObserver();
      checkDistanceToDestination();
    } catch (e) {
      console.warn('Naver map init:', e);
    }
  }
};

let pendingDestMarker = null;
let pendingDestInfoWindow = null;
let longPressVisualMarker = null;
let touchTimer = null;
let touchStartCoord = null;
let touchStartPixel = null;

// 모바일 햅틱 진동 피드백 유틸 (디바이스 지원 시 40ms 미세 진동)
const triggerHapticFeedback = () => {
  if (typeof window !== 'undefined' && 'vibrate' in navigator) {
    try {
      navigator.vibrate([40]);
    } catch (err) {
      // 햅틱 미지원 디바이스 예외 무시
    }
  }
};

const clearLongPressVisual = () => {
  if (longPressVisualMarker) {
    longPressVisualMarker.setMap(null);
    longPressVisualMarker = null;
  }
};

const showLongPressVisual = (coord) => {
  clearLongPressVisual();
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  const latLng = new window.naver.maps.LatLng(coord.lat(), coord.lng());

  longPressVisualMarker = new window.naver.maps.Marker({
    position: latLng,
    map: mapInstance.value,
    icon: {
      content: `
        <div class="relative flex items-center justify-center pointer-events-none" style="transform: translate(-50%, -50%); width: 64px; height: 64px;">
          <!-- 1. 외곽 팽창 네온 아우라 리플 -->
          <div class="absolute inset-0 rounded-full border-2 border-blue-400/80 animate-ping opacity-60"></div>
          <!-- 2. 500ms 진행률 SVG 프로그레스 링 -->
          <svg class="absolute inset-0 w-full h-full transform -rotate-90 drop-shadow-md" viewBox="0 0 64 64">
            <circle cx="32" cy="32" r="26" stroke="#93c5fd" stroke-width="4" fill="none" opacity="0.35"/>
            <circle cx="32" cy="32" r="26" stroke="#2563eb" stroke-width="4" fill="none"
              stroke-dasharray="163.36" stroke-dashoffset="163.36"
              style="animation: fillLongPressRing 500ms linear forwards; stroke-linecap: round;"/>
          </svg>
          <!-- 3. 중앙 네온 블루 코어 닷 -->
          <div class="h-4 w-4 rounded-full bg-blue-600 border-2 border-white shadow-xl animate-pulse"></div>
        </div>
      `,
      anchor: new window.naver.maps.Point(32, 32),
    },
    zIndex: 300,
  });
};

const handleLongPressStart = (e) => {
  clearTimeout(touchTimer);
  clearLongPressVisual();
  if (!e || !e.coord) return;

  // PC(마우스) 환경의 클릭인 경우 모바일 전용 롱프레스 동작을 즉시 차단
  if (!isTouchEvent(e)) return;

  touchStartCoord = e.coord;
  const point = e.offset || e.pointerEvent || {};
  touchStartPixel = { x: point.clientX || 0, y: point.clientY || 0 };

  // 시각적 500ms SVG 링 렌더링
  showLongPressVisual(touchStartCoord);

  touchTimer = setTimeout(() => {
    // 500ms 꾹 누르기 달성 시 시각 효과 제거 -> 손끝 햅틱 진동 -> 목적지 지정 팝업 출력
    clearLongPressVisual();
    triggerHapticFeedback();
    handleMapRightClick({ coord: touchStartCoord });
  }, 500);
};

const handleLongPressMove = (e) => {
  if (!touchTimer) return;
  const point = e.offset || e.pointerEvent || {};
  const currentX = point.clientX || 0;
  const currentY = point.clientY || 0;
  const dx = currentX - (touchStartPixel?.x || 0);
  const dy = currentY - (touchStartPixel?.y || 0);

  // 10px 이상 지도 스크롤/드래그 시 롱프레스 및 시각 애니메이션 취소
  if (Math.sqrt(dx * dx + dy * dy) > 10) {
    handleLongPressEnd();
  }
};

const handleLongPressEnd = () => {
  clearTimeout(touchTimer);
  touchTimer = null;
  clearLongPressVisual();
};

const clearPendingDestinationOverlay = () => {
  handleLongPressEnd();
  if (pendingDestInfoWindow) {
    pendingDestInfoWindow.close();
    pendingDestInfoWindow = null;
  }
  if (pendingDestMarker) {
    pendingDestMarker.setMap(null);
    pendingDestMarker = null;
  }
};

const authStore = useAuthStore();

const handleMapRightClick = async (e) => {
  if (!e || !e.coord || !window.naver || !window.naver.maps) return;
  const lat = e.coord.lat();
  const lng = e.coord.lng();

  clearPendingDestinationOverlay();

  try {
    const geoResult = await reverseGeocodeCoord(lat, lng);
    let placeName = geoResult.name;
    const roadOrJibunAddress =
      geoResult.roadAddress || geoResult.jibunAddress || placeName;

    // 0. 우클릭 시 구/행정동 단위 키워드로 백엔드 DB (/api/destinations/search) 0순위 최우선 탐색 호출
    let dbMatch = null;
    try {
      // 주소에서 '구' 또는 '동/로' 추출하여 DB 등록 장소 전체 탐색
      const guMatch = (placeName || roadOrJibunAddress).match(
        /([가-휘]+구|[가-휘]+시|[가-휘]+동)/,
      );
      const searchKeyword = guMatch ? guMatch[1] : placeName;
      const dbResults = await onboardingApi.searchPlaces(searchKeyword);
      if (dbResults && dbResults.length > 0) {
        dbMatch = findMatchingDestination(
          placeName,
          roadOrJibunAddress,
          dbResults,
          lat,
          lng,
        );
      }
    } catch (err) {
      console.warn(
        'DB destination search failed, fallback to local/geocode:',
        err,
      );
    }

    // 1. 유저 별칭/최근 목적지(recentList) 1순위 -> 백엔드 DB 장소(dbMatch) 2순위 통합 판단
    const userId = authStore.user?.userId || authStore.user?.id;
    const recentList = getRecentDestinations(userId) || [];
    const matched =
      findMatchingDestination(
        placeName,
        roadOrJibunAddress,
        recentList,
        lat,
        lng,
      ) || dbMatch;

    if (matched?.destName) {
      placeName = matched.destName;
    }

    const pendingLatLng = new window.naver.maps.LatLng(lat, lng);

    pendingDestMarker = new window.naver.maps.Marker({
      position: pendingLatLng,
      map: mapInstance.value,
      icon: {
        content: `
          <div class="relative flex items-center justify-center">
            <div class="h-7 w-7 rounded-full bg-blue-600 border-2 border-white shadow-xl flex items-center justify-center text-white text-xs font-black animate-bounce">
              📍
            </div>
          </div>
        `,
        anchor: new window.naver.maps.Point(14, 14),
      },
      zIndex: 250,
    });

    const cardContainer = document.createElement('div');
    cardContainer.className =
      'p-3.5 rounded-2xl bg-white/95 backdrop-blur-md shadow-2xl border border-blue-200 text-slate-800 text-xs w-68 space-y-2.5 pointer-events-auto';
    cardContainer.innerHTML = `
      <div class="flex items-center justify-between border-b pb-1.5 border-slate-100">
        <span class="text-blue-600 font-black text-xs flex items-center gap-1">
          <span>📍</span>
          <span>목적지 변경 안내</span>
        </span>
        <button type="button" class="btn-close flex h-5 w-5 items-center justify-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-600 text-xs font-bold transition-all">✕</button>
      </div>
      <div>
        <div class="text-[11px] text-slate-500 font-medium">이 위치를 목적지로 지정하시겠습니까?</div>
        <div class="text-sm font-black text-slate-900 mt-1 break-all leading-snug">${placeName}</div>
        ${(() => {
          const normPlace = (placeName || '').replace(/\s+/g, ' ').trim();
          const normAddr = (roadOrJibunAddress || '')
            .replace(/\s+/g, ' ')
            .trim();
          if (
            !normAddr ||
            normPlace === normAddr ||
            normPlace.includes(normAddr) ||
            normAddr.includes(normPlace)
          ) {
            return '';
          }
          return `<div class="text-[11px] text-slate-400 font-normal mt-0.5 break-all">${roadOrJibunAddress}</div>`;
        })()}
      </div>
      <div class="flex items-center gap-1.5 pt-1">
        <button type="button" class="btn-confirm flex-1 rounded-xl bg-blue-600 hover:bg-blue-700 active:scale-95 text-white font-black py-2 text-xs transition-all shadow-md">
          목적지로 지정
        </button>
        <button type="button" class="btn-cancel px-3 rounded-xl bg-slate-100 hover:bg-slate-200 active:scale-95 text-slate-600 font-extrabold py-2 text-xs transition-all">
          취소
        </button>
      </div>
    `;

    cardContainer.querySelector('.btn-close').addEventListener('click', () => {
      clearPendingDestinationOverlay();
    });
    cardContainer.querySelector('.btn-cancel').addEventListener('click', () => {
      clearPendingDestinationOverlay();
    });
    cardContainer
      .querySelector('.btn-confirm')
      .addEventListener('click', () => {
        emit('change-destination', {
          name: placeName,
          address: geoResult.roadAddress || geoResult.jibunAddress || placeName,
          lat,
          lng,
        });
        clearPendingDestinationOverlay();
      });

    pendingDestInfoWindow = new window.naver.maps.InfoWindow({
      content: cardContainer,
      borderWidth: 0,
      disableAnchor: false,
      backgroundColor: 'transparent',
      pixelOffset: new window.naver.maps.Point(0, -10),
    });

    pendingDestInfoWindow.open(mapInstance.value, pendingDestMarker);
  } catch (err) {
    console.warn('Map rightclick geocode error:', err);
  }
};

watch(
  [
    () => props.properties,
    () => props.destination,
    () => props.selectedProperty,
  ],
  () => {
    renderMarkers();
    checkDistanceToDestination();
  },
  { deep: true },
);

watch(
  () => [props.destination?.id, props.destination?.lat, props.destination?.lng],
  ([destinationId, latitude, longitude]) => {
    const lat = Number(latitude);
    const lng = Number(longitude);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

    console.info('[Map] destination marker coordinate', {
      destinationId,
      latitude: lat,
      longitude: lng,
    });

    if (activeDestMarker && window.naver?.maps) {
      activeDestMarker.setPosition(new window.naver.maps.LatLng(lat, lng));
      activeDestMarker._key = `${lat}_${lng}_${props.destination.name || ''}`;
    }

    renderMarkers();
  },
  { immediate: true },
);

watch(
  () => props.selectedProperty?.propertyId,
  (currentPropertyId, previousPropertyId) => {
    if (
      currentPropertyId === previousPropertyId ||
      expandedAmenityMarkerKeys.value.size === 0
    )
      return;

    expandedAmenityMarkerKeys.value = new Set();
    refreshAmenityMarkerContents();
  },
);

watch(
  () => props.amenities,
  () => {
    renderAmenityMarkers();
    scheduleSelectedPropertyContextFit();
  },
  { deep: true },
);

watch(
  () => props.safetyRoute,
  () => {
    renderSafetyRoute();
    clearRouteFacilityOverlays();
    scheduleSelectedPropertyContextFit();
  },
  { deep: true },
);

watch(
  () => props.routeFacilities,
  () => renderRouteFacilities(),
  { deep: true },
);

watch(
  [() => props.selectedProperty, () => props.destination],
  () => scheduleSelectedPropertyContextFit(),
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
  if (!filter || filter.showIsochrone === false || props.selectedProperty)
    return;

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
  const centerLat = Number(
    props.destination.lat || props.destination.destLatitude,
  );
  const centerLng = Number(
    props.destination.lng || props.destination.destLongitude,
  );
  const latRad = (centerLat * Math.PI) / 180;

  // 15% 여유 공간 마진
  const latOffset = (radiusMeters / earthRadius) * (180 / Math.PI) * 1.15;
  const lngOffset =
    (((radiusMeters / (earthRadius * Math.cos(latRad))) * 180) / Math.PI) *
    1.15;

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

// 목적지 변경 시 줌 자동 조율 (매물 추가 로드 시에는 현재 사용자의 줌 레벨을 유지)
watch(
  () => props.destination,
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
  if (selectedContextFitFrame) cancelAnimationFrame(selectedContextFitFrame);
  activePropertyMarkersMap.forEach((marker) => marker.setMap(null));
  activePropertyMarkersMap.clear();
  clearDestinationMarkers();
  clearAmenityMarkers();
  clearSafetyRoutePolyline();
  clearRouteFacilityOverlays();
  clearPendingDestinationOverlay();
  clearUnsupportedDistrictGeoJson(mapInstance.value);
  if (debugViewportRectangleInstance) {
    debugViewportRectangleInstance.setMap(null);
    debugViewportRectangleInstance = null;
  }
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
});
const isFarFromDestination = ref(false);

const destinationName = computed(() => {
  const raw =
    props.destination?.name || props.destination?.destName || '내 목적지';
  return raw.replace(/\s*\(주 목적지\)$/, '');
});

// 🧪 디버그/시각화용: 카메라 뷰포트 사각형 (Bounding Box) 실시간 지도 가시화 ON/OFF 토글 (true: 켜기, false: 끄기)
const SHOW_DEBUG_VIEWPORT_RECTANGLE = ref(false);
let debugViewportRectangleInstance = null;

const renderDebugViewportRectangle = () => {
  if (!mapInstance.value || !window.naver || !window.naver.maps) return;

  if (!SHOW_DEBUG_VIEWPORT_RECTANGLE.value) {
    if (debugViewportRectangleInstance) {
      debugViewportRectangleInstance.setMap(null);
      debugViewportRectangleInstance = null;
    }
    return;
  }

  const bounds = mapInstance.value.getBounds();
  if (!bounds) return;

  const sw = bounds.getSW();
  const ne = bounds.getNE();

  // 화면 브라우저 가장자리에 겹쳐서 안 보이는 현상을 방지하기 위해 3% 내측 인셋(Inset) 사각형 선명 표기
  const latSpan = ne.lat() - sw.lat();
  const lngSpan = ne.lng() - sw.lng();
  const insetSw = new window.naver.maps.LatLng(
    sw.lat() + latSpan * 0.03,
    sw.lng() + lngSpan * 0.03,
  );
  const insetNe = new window.naver.maps.LatLng(
    ne.lat() - latSpan * 0.03,
    ne.lng() - lngSpan * 0.03,
  );
  const insetBounds = new window.naver.maps.LatLngBounds(insetSw, insetNe);

  if (!debugViewportRectangleInstance) {
    debugViewportRectangleInstance = new window.naver.maps.Rectangle({
      map: mapInstance.value,
      bounds: insetBounds,
      strokeColor: '#ef4444',
      strokeWeight: 3,
      strokeOpacity: 0.95,
      strokeStyle: 'dash',
      fillColor: '#ef4444',
      fillOpacity: 0.1,
      clickable: false,
      zIndex: 200,
    });
  } else {
    debugViewportRectangleInstance.setBounds(insetBounds);
    if (!debugViewportRectangleInstance.getMap()) {
      debugViewportRectangleInstance.setMap(mapInstance.value);
    }
  }
};

const checkDistanceToDestination = () => {
  if (
    !mapInstance.value ||
    !props.destination ||
    !window.naver ||
    !window.naver.maps
  )
    return;
  const targetLat =
    Number(props.destination.lat || props.destination.destLatitude) || 37.5502;
  const targetLng =
    Number(props.destination.lng || props.destination.destLongitude) ||
    127.0731;

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
      v-if="showIsochroneOverlay"
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
@keyframes fillLongPressRing {
  0% {
    stroke-dashoffset: 163.36;
  }
  100% {
    stroke-dashoffset: 0;
  }
}

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
