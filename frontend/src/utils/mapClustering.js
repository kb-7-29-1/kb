/**
 * 초고속 격자 클러스터링 (Grid Marker Clustering) 및 바운즈 연산 유틸리티
 */

import { createApp } from 'vue';
import PropertyPin from '@/components/map/PropertyPin.vue';
import DestinationPin from '@/components/map/DestinationPin.vue';

// 매물 마커 핀 HTML 렌더러 (PropertyPin.vue 컴포넌트 실시간 마운트)
export const renderPropertyPinHTML = (prop, isSelected) => {
  const container = document.createElement('div');
  const pinApp = createApp(PropertyPin, { property: prop, isSelected });
  pinApp.mount(container);
  const content = container.innerHTML;
  pinApp.unmount();
  return content;
};

// 주 목적지 마커 핀 HTML 렌더러 (DestinationPin.vue 컴포넌트 실시간 마운트)
export const renderDestinationPinHTML = (destination) => {
  const container = document.createElement('div');
  const pinApp = createApp(DestinationPin, { destination });
  pinApp.mount(container);
  const content = container.innerHTML;
  pinApp.unmount();
  return content;
};

// 클러스터 마커 핀 HTML (축소 상태 시 매물 묶음 핀)
export const renderClusterPinHTML = (count) => {
  return `
    <div class="flex h-[58px] w-[58px] items-center justify-center rounded-full border border-[#aebcff] bg-[#dfe5ff] text-[#3852e8] shadow-lg transition-all transform -translate-x-1/2 -translate-y-full hover:-translate-y-[calc(100%+3px)] hover:bg-[#d2dcff] cursor-pointer z-20 select-none">
      <span class="text-[18px] font-extrabold leading-none">${count.toLocaleString()}<span class="ml-px text-[11px] font-semibold">개</span></span>
    </div>
  `;
};

/**
 * 사용자 세세 조절 커스텀 옵션 세팅
 */
export const DEFAULT_CLUSTER_OPTIONS = {
  minClusterZoom: 18, // 🔍 18 레벨 이상 극도로 확대했을 때만 클러스터 해제
  gridSizeFactor: 0.012, // 📐 클러스터 반경 범위 크기
  minClusterCount: 2, // 🔢 최소 묶음 개수 (2개부터 무조건 클러스터 핀으로 묶음)
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
