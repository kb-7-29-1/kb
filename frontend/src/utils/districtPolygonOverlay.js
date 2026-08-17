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

// 미지원 8개 자치구 중심 좌표 정의 (어두운 마스크 위에서도 또렷이 보이도록 라벨 칩 핀 제공)
const UNSUPPORTED_DISTRICT_CENTERS = [
  { name: '강남구', lat: 37.4959, lng: 127.0664 },
  { name: '강북구', lat: 37.6396, lng: 127.0255 },
  { name: '금천구', lat: 37.4568, lng: 126.8955 },
  { name: '마포구', lat: 37.5637, lng: 126.9087 },
  { name: '성동구', lat: 37.5634, lng: 127.0368 },
  { name: '성북구', lat: 37.5891, lng: 127.0182 },
  { name: '영등포구', lat: 37.5264, lng: 126.8962 },
  { name: '용산구', lat: 37.5326, lng: 126.9900 },
];

/**
 * 네이버 지도 공식 Data Layer(GeoJSON)를 사용하여 서울 25개 자치구 전체를 투톤(Green vs Coral)으로 렌더링합니다:
 * 1) 🟢 [안전점수 지원 자치구 17개]: 청량한 에메랄드 그린 실선 & 은은한 틴트 (#10b981)
 * 2) 🔒 [안전점수 미지원 자치구 8개]: 선명한 네온 코랄 점선 & 음영 (#fb7185) + 플로팅 칩 라벨
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
        // 🔒 [미지원 자치구 (강남, 마포, 용산 등)]: 선명한 네온 코랄 점선 테두리 + 음영
        return {
          fillColor: '#f43f5e',
          fillOpacity: 0.13,
          strokeColor: '#fb7185',
          strokeOpacity: 0.95,
          strokeWeight: 2.0,
          strokeStyle: 'dash',
          clickable: false,
          zIndex: 25,
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

    // 3. 서울시 미지원 자치구 중심에 선명한 플로팅 칩 라벨 생성 (zIndex: 30)
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
                  gap: 4px;
                  background: rgba(15, 23, 42, 0.92);
                  border: 1.5px solid #fb7185;
                  color: #ffe4e6;
                  padding: 3px 8px;
                  border-radius: 9999px;
                  font-size: 10.5px;
                  font-weight: 700;
                  box-shadow: 0 4px 12px rgba(0,0,0,0.5);
                  white-space: nowrap;
                  backdrop-filter: blur(4px);
                ">
                  <span style="color: #fb7185; font-size: 11px;">🔒</span>
                  <span>${center.name}</span>
                  <span style="color: #fda4af; font-size: 9.5px; font-weight: 600;">(보안등 데이터 부족)</span>
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
  } catch (ignored) {}
  clearUnsupportedDistrictLabels();
  isGeoJsonAdded = false;
}
