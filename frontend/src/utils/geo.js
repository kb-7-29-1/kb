/**
 * 하버사인 공식 (두 위경도 좌표 간의 대권 거리 km 계산)
 * @param {number} lat1 
 * @param {number} lon1 
 * @param {number} lat2 
 * @param {number} lon2 
 * @returns {number} 거리 (km)
 */
export const getHaversineDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371; // 지구 반지름 (km)
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) *
      Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // 거리 (km)
};

export const calculateDistanceKm = getHaversineDistance;

/**
 * 네이버 지도 Reverse Geocode API를 호출하여 위경도 좌표를 장소/주소명으로 변환합니다.
 * 1순위: 건물명/장소명 (land.buildingName)
 * 2순위: 도로명 주소 (roadAddress)
 * 3순위: 지번 주소 (jibunAddress)
 * 
 * @param {number} lat 위도
 * @param {number} lng 경도
 * @returns {Promise<{ name: string, buildingName: string, roadAddress: string, jibunAddress: string }>}
 */
export const reverseGeocodeCoord = (lat, lng) => {
  return new Promise((resolve, reject) => {
    if (!window.naver || !window.naver.maps || !window.naver.maps.Service) {
      return reject(new Error('Naver Map Service is not available'));
    }

    const coords = new window.naver.maps.LatLng(lat, lng);
    window.naver.maps.Service.reverseGeocode(
      {
        coords,
        orders: [
          window.naver.maps.Service.OrderType.ADDR,
          window.naver.maps.Service.OrderType.ROAD_ADDR,
        ].join(','),
      },
      (status, response) => {
        if (status !== window.naver.maps.Service.Status.OK || !response?.v2) {
          return reject(new Error('Reverse geocode failed'));
        }

        const v2 = response.v2;
        let buildingName = '';
        let roadAddress = v2.address?.roadAddress || '';
        let jibunAddress = v2.address?.jibunAddress || '';
        let detailedAddress = '';

        if (v2.results && Array.isArray(v2.results)) {
          for (const item of v2.results) {
            const area1 = item.region?.area1?.name || '';
            const area2 = item.region?.area2?.name || '';
            const area3 = item.region?.area3?.name || '';
            const landNumber = item.land?.number1
              ? item.land.number2
                ? `${item.land.number1}-${item.land.number2}`
                : item.land.number1
              : '';
            const roadName = item.land?.name || '';
            const bName =
              item.land?.addition0?.value ||
              item.land?.buildingName ||
              '';

            if (bName && !buildingName) {
              buildingName = bName.trim();
            }

            if (item.name === 'roadaddr' && area2 && roadName) {
              roadAddress = `${area1} ${area2} ${roadName} ${landNumber}`.replace(/\s+/g, ' ').trim();
            } else if (item.name === 'addr' && area2 && area3) {
              jibunAddress = `${area1} ${area2} ${area3} ${landNumber}`.replace(/\s+/g, ' ').trim();
            }

            if (!detailedAddress && (area2 || area3)) {
              detailedAddress = [area2, area3, roadName || landNumber]
                .filter(Boolean)
                .join(' ')
                .trim();
            }
          }
        }

        let displayName = buildingName;
        if (!displayName) {
          if (roadAddress && roadAddress !== '서울특별시') {
            displayName = roadAddress;
          } else if (jibunAddress && jibunAddress !== '서울특별시') {
            displayName = jibunAddress;
          } else if (detailedAddress) {
            displayName = detailedAddress;
          } else {
            displayName = `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
          }
        }

        if (displayName === '서울특별시' && detailedAddress) {
          displayName = detailedAddress;
        }

        const finalRoad = roadAddress && roadAddress !== '서울특별시' ? roadAddress : (jibunAddress || displayName);
        const finalJibun = jibunAddress && jibunAddress !== '서울특별시' ? jibunAddress : (roadAddress || displayName);

        resolve({
          name: displayName,
          buildingName,
          roadAddress: finalRoad,
          jibunAddress: finalJibun,
        });
      },
    );
  });
};
