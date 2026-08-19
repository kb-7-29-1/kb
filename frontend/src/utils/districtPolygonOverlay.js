import seoulGeoData from '@/assets/seoul_districts_geo.json';
export { UNSUPPORTED_SAFETY_DISTRICTS, SUPPORTED_SAFETY_DISTRICTS } from '@/utils/districtSupport.js';
import { UNSUPPORTED_SAFETY_DISTRICTS } from '@/utils/districtSupport.js';

/**
 * 서울시 25개 전체 자치구 명단 (살고싶오 전용 서비스 권역)
 */
export const SEOUL_25_GUS = [
  '강남구', '강동구', '강북구', '강서구', '관악구',
  '광진구', '구로구', '금천구', '노원구', '도봉구',
  '동대문구', '동작구', '마포구', '서대문구', '서초구',
  '성동구', '성북구', '송파구', '양천구', '영등포구',
  '용산구', '은평구', '종로구', '중구', '중랑구',
];

/**
 * 주소가 서울시 서비스 관할 권역인지 검증합니다.
 */
export function isSeoulServiceArea(address) {
  if (!address || typeof address !== 'string') return false;
  if (address.includes('서울') || address.includes('서울특별시')) return true;
  return SEOUL_25_GUS.some((gu) => address.includes(gu));
}

let isGeoJsonAdded = false;
let activeLabelMarkers = [];

// 미지원 11개 자치구 중심 좌표 정의 (어두운 마스크 위에서도 또렷이 보이도록 라벨 칩 핀 제공)
const UNSUPPORTED_DISTRICT_CENTERS = [
  { name: '강남구', lat: 37.4959, lng: 127.0664 },
  { name: '강북구', lat: 37.6396, lng: 127.0255 },
  { name: '강서구', lat: 37.5612, lng: 126.8228 },
  { name: '관악구', lat: 37.4653, lng: 126.9439 },
  { name: '마포구', lat: 37.5637, lng: 126.9087 },
  { name: '성동구', lat: 37.5634, lng: 127.0368 },
  { name: '성북구', lat: 37.5891, lng: 127.0182 },
  { name: '영등포구', lat: 37.5264, lng: 126.8962 },
  { name: '용산구', lat: 37.5326, lng: 126.9900 },
  { name: '중구', lat: 37.5579, lng: 126.9941 },
  { name: '중랑구', lat: 37.5953, lng: 127.0939 },
];

/**
 * 네이버 지도 공식 Data Layer(GeoJSON)를 사용하여 서울 25개 자치구 전체를 투톤(Green vs Coral)으로 렌더링합니다:
 * 1) 🟢 [안전점수 지원 자치구 14개]: 청량한 에메랄드 그린 실선 & 투명 (#059669)
 * 2) 🔒 [안전점수 미지원 자치구 11개]: 선명한 네온 코랄 점선 & 음영 (#fb7185) + 플로팅 칩 라벨
 * @param {object} mapInstance 네이버 지도 인스턴스
 */
export function renderUnsupportedDistrictGeoJson(mapInstance) {
  try {
    if (!mapInstance || !mapInstance.data) {
      return;
    }

    const unsupportedSet = new Set(UNSUPPORTED_SAFETY_DISTRICTS);

    // 1. 서울시 전체 25개 자치구 GeoJSON 로드
    if (!isGeoJsonAdded && seoulGeoData) {
      mapInstance.data.addGeoJson(seoulGeoData);
      isGeoJsonAdded = true;
    }

    // 2. Data Layer 지능형 2톤 스타일링 (🟢 지원 = 초록 vs 🔒 미지원 = 코랄)
    mapInstance.data.setStyle((feature) => {
      const guName = feature.getProperty('SIG_KOR_NM') || feature.getProperty('name') || '';
      const isUnsupported = unsupportedSet.has(guName);

      if (isUnsupported) {
        // 🔒 [미지원 자치구 (강남, 마포, 용산 등)]: 묵직하고 차분한 딥 다크 슬레이트 점선 테두리 + 어두운 딤드 마스크
        return {
          fillColor: '#0f172a',
          fillOpacity: 0.28,
          strokeColor: '#64748b',
          strokeOpacity: 0.85,
          strokeWeight: 2.0,
          strokeStyle: 'dash',
          clickable: false,
          zIndex: 20,
        };
      } else {
        // 🟢 [지원 자치구 (관악, 광진, 동작 등)]: 두껍고 또렷한 에메랄드 그린 외곽선 (내부는 100% 투명 유지)
        return {
          fillColor: 'transparent',
          fillOpacity: 0,
          strokeColor: '#059669',
          strokeOpacity: 0.9,
          strokeWeight: 2.2,
          strokeStyle: 'solid',
          clickable: false,
          zIndex: 18,
        };
      }
    });

    // 3. 서울시 미지원 자치구 중심에 모던한 글래스모피즘 슬레이트 그레이 칩 라벨 생성 (zIndex: 30)
    clearUnsupportedDistrictLabels();
    UNSUPPORTED_DISTRICT_CENTERS.forEach((center) => {
      if (unsupportedSet.has(center.name)) {
        const marker = new window.naver.maps.Marker({
          position: new window.naver.maps.LatLng(center.lat, center.lng),
          map: mapInstance,
          zIndex: 30,
          icon: {
            content: `
              <div style="transform: translate(-50%, -50%); pointer-events: none;">
                <div style="
                  display: flex;
                  align-items: center;
                  gap: 5px;
                  background: rgba(30, 41, 59, 0.88);
                  border: 1px solid rgba(148, 163, 184, 0.4);
                  color: #f1f5f9;
                  padding: 3.5px 9px;
                  border-radius: 9999px;
                  font-size: 11px;
                  font-weight: 600;
                  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25), inset 0 1px 1px rgba(255, 255, 255, 0.1);
                  white-space: nowrap;
                  backdrop-filter: blur(8px);
                  -webkit-backdrop-filter: blur(8px);
                ">
                  <span style="color: #94a3b8; font-size: 11px;">🔒</span>
                  <span style="color: #f8fafc; font-weight: 700;">${center.name}</span>
                  <span style="color: #94a3b8; font-size: 10px; font-weight: 500;">(보안등 데이터 미제공)</span>
                </div>
              </div>
            `,
          },
        });
        activeLabelMarkers.push(marker);
      }
    });
  } catch (err) {
    console.warn('[districtPolygonOverlay] GeoJSON Data Layer 로드 실패 방어:', err);
  }
}

/**
 * 라벨 칩 마커 제거
 */
function clearUnsupportedDistrictLabels() {
  activeLabelMarkers.forEach((m) => {
    if (m && m.setMap) m.setMap(null);
  });
  activeLabelMarkers = [];
}

/**
 * 지도 언마운트 시 GeoJSON 및 라벨 초기화
 */
export function clearUnsupportedDistrictGeoJson(mapInstance) {
  try {
    if (mapInstance && mapInstance.data) {
      mapInstance.data.forEach((feature) => {
        mapInstance.data.removeGeoJson(feature);
      });
    }
  } catch (ignored) { }
  clearUnsupportedDistrictLabels();
  isGeoJsonAdded = false;
}

/**
 * 2D Ray-Casting 알고리즘 기반 점의 폴리곤 링 내부 포함 여부 판별 (초경량 0.001ms)
 */
function isPointInsidePolygonRing(x, y, ring) {
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const xi = ring[i][0];
    const yi = ring[i][1];
    const xj = ring[j][0];
    const yj = ring[j][1];
    const intersect = yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
    if (intersect) inside = !inside;
  }
  return inside;
}

/**
 * 특정 위경도 좌표(lat, lng)가 안전데이터 미구축 8개 자치구 폴리곤 내부에 위치하는지 검사합니다.
 */
export function isPointInUnsupportedDistrict(lat, lng) {
  if (!seoulGeoData || !Array.isArray(seoulGeoData.features)) return false;
  const unsupportedSet = new Set(UNSUPPORTED_SAFETY_DISTRICTS);

  for (const feature of seoulGeoData.features) {
    const guName = feature.properties?.SIG_KOR_NM || feature.properties?.name;
    if (!unsupportedSet.has(guName)) continue;

    const geometry = feature.geometry;
    if (!geometry) continue;

    if (geometry.type === 'Polygon' && Array.isArray(geometry.coordinates)) {
      for (const ring of geometry.coordinates) {
        if (isPointInsidePolygonRing(lng, lat, ring)) return true;
      }
    } else if (geometry.type === 'MultiPolygon' && Array.isArray(geometry.coordinates)) {
      for (const poly of geometry.coordinates) {
        if (Array.isArray(poly)) {
          for (const ring of poly) {
            if (isPointInsidePolygonRing(lng, lat, ring)) return true;
          }
        }
      }
    }
  }
  return false;
}
