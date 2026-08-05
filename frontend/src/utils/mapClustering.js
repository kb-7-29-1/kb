/**
 * 초고속 격자 클러스터링 (Grid Marker Clustering) 및 바운즈 연산 유틸리티
 */

import { formatPropertyPrice } from '@/utils/priceFormatter';

// 매물 마커 핀 HTML 렌더러
export const renderPropertyPinHTML = (prop, isSelected) => {
  const priceText = formatPropertyPrice(prop.deposit, prop.monthlyRent);
  const safetyScore = prop.safetyScore || 85;
  const bgClass = isSelected
    ? 'bg-blue-600 text-white ring-4 ring-blue-500/30 border-blue-400 scale-110 z-30'
    : 'bg-slate-900/95 text-white hover:bg-blue-600 border-slate-700 backdrop-blur-md z-10';

  let icon = '🏠';
  if (isSelected) {
    icon = '📍';
  } else if (prop.buildingType === 3) {
    icon = '🏢';
  } else if (prop.buildingType === 1) {
    icon = '🏡';
  }

  return `
    <div class="px-2.5 py-1 rounded-xl text-xs font-black shadow-2xl border transition-all cursor-pointer flex flex-col items-center justify-center transform -translate-x-1/2 -translate-y-full select-none leading-tight ${bgClass}">
      <div class="flex items-center gap-1">
        <span class="text-xs">${icon}</span>
        <span class="text-emerald-400 font-extrabold flex items-center gap-0.5 text-[11px]"><i class="fa-solid fa-shield-halved text-[9px]"></i> ${safetyScore}점</span>
      </div>
      <div class="text-[11px] font-black text-white whitespace-nowrap mt-0.5">
        ${priceText}
      </div>
    </div>
  `;
};

// 주 목적지 마커 핀 HTML 렌더러
export const renderDestinationPinHTML = (destination) => {
  const rawName = destination?.name || destination?.destName || '주 목적지';
  const name = rawName.replace(/\s*\(주 목적지\)$/, '');
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

// 클러스터 마커 핀 HTML (축소 상태 시 매물 묶음 핀)
export const renderClusterPinHTML = (count, items = []) => {
  let icon = '🏘️'; // 2종류 이상 혼합 묶음
  if (items && items.length > 0) {
    const buildingTypes = new Set(items.map((item) => item.buildingType));
    if (buildingTypes.size === 1) {
      const singleType = Array.from(buildingTypes)[0];
      if (singleType === 3) icon = '🏢'; // 오피스텔 단일 묶음
      else if (singleType === 1) icon = '🏡'; // 빌라 단일 묶음
      else if (singleType === 2) icon = '🏠'; // 다가구 단일 묶음
    }
  }

  return `
    <div class="px-3 py-2 rounded-2xl text-xs font-black shadow-2xl border border-blue-400/50 bg-blue-600/90 text-white backdrop-blur-md transition-all transform -translate-x-1/2 -translate-y-full hover:scale-110 flex items-center gap-1.5 cursor-pointer z-20 select-none">
      <span class="text-sm">${icon}</span>
      <span class="text-sm tracking-tight font-extrabold">${count.toLocaleString()}개 매물</span>
    </div>
  `;
};

/**
 * 사용자 세세 조절 커스텀 옵션 세팅
 */
export const DEFAULT_CLUSTER_OPTIONS = {
  minClusterZoom: 18,    // 🔍 18 레벨 이상 극도로 확대했을 때만 클러스터 해제
  gridSizeFactor: 0.012, // 📐 클러스터 반경 범위 크기
  minClusterCount: 2,    // 🔢 최소 묶음 개수 (2개부터 무조건 클러스터 핀으로 묶음)
};

/**
 * 초고속 격자 클러스터링 알고리즘
 * @param {Array} properties - 매물 목록
 * @param {Number} currentZoom - 현재 네이버 지도 줌 레벨
 * @param {Object} bounds - 현재 화면 가시 영역 (naver.maps.LatLngBounds)
 * @param {Object} customOptions - 사용자 세부 조정 옵션 { minClusterZoom, gridSizeFactor, minClusterCount }
 * @returns {Array} 클러스터 노드 및 개별 매물 노드 리스트
 */
export const getClusteredMarkers = (properties, currentZoom, bounds, customOptions = {}) => {
  if (!properties || properties.length === 0) return [];

  const options = { ...DEFAULT_CLUSTER_OPTIONS, ...customOptions };

  // 1. 줌 레벨이 minClusterZoom 이상으로 확대되면 개별 매물 핀 표출
  if (currentZoom >= options.minClusterZoom) {
    return properties
      .filter((p) => p.latitude && p.longitude)
      .filter((p) =>
        bounds ? bounds.hasLatLng(new window.naver.maps.LatLng(p.latitude, p.longitude)) : true,
      )
      .map((p) => ({ isCluster: false, item: p }));
  }

  // 2. 줌 레벨이 낮을(축소) 때 격자 클러스터링 계산
  const gridSize = options.gridSizeFactor * Math.pow(2, 14 - currentZoom);
  const clusters = new Map();

  properties.forEach((p) => {
    if (!p.latitude || !p.longitude) return;
    const latLng = new window.naver.maps.LatLng(p.latitude, p.longitude);
    if (bounds && !bounds.hasLatLng(latLng)) return;

    const gridX = Math.floor(p.longitude / gridSize);
    const gridY = Math.floor(p.latitude / gridSize);
    const key = `${gridX}_${gridY}`;

    if (!clusters.has(key)) {
      clusters.set(key, {
        isCluster: true,
        count: 0,
        sumLat: 0,
        sumLng: 0,
        items: [],
      });
    }

    const c = clusters.get(key);
    c.count++;
    c.sumLat += p.latitude;
    c.sumLng += p.longitude;
    c.items.push(p);
  });

  const result = [];
  clusters.forEach((c) => {
    if (c.count < options.minClusterCount) {
      c.items.forEach((item) => result.push({ isCluster: false, item }));
    } else {
      result.push({
        isCluster: true,
        count: c.count,
        lat: c.sumLat / c.count,
        lng: c.sumLng / c.count,
        items: c.items,
      });
    }
  });

  return result;
};
