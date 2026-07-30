backend/
└─ src/main/
├─ java/com/salgosipo/
│ ├─ global/ # 공통: 담당자 공동, 최초 1회 설정
│ │ ├─ config/ # RootConfig, ServletConfig, Swagger, Web
│ │ ├─ security/ # JWT, 인증 필터
│ │ ├─ response/ # ApiResponse, ErrorResponse
│ │ ├─ exception/ # GlobalExceptionHandler
│ │ └─ util/
│ │
│ ├─ auth/ # 로그인·로그아웃·ID/PW 찾기
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ └─ dto/
│ │
│ ├─ user/ # 회원가입·프로필·회원 탈퇴
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ ├─ domain/ # UserVO
│ │ └─ dto/
│ │
│ ├─ bookmark/ # 관심 매물만 별도 소유
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ ├─ domain/ # BookmarkVO
│ │ └─ dto/
│ │
│ ├─ onboarding/ # 조건 등록·수정·초기화·조회
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ ├─ domain/ # OnboardingVO
│ │ └─ dto/
│ │
│ ├─ destination/ # 목적지 키워드·주소·좌표
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ ├─ client/ # 주소/좌표 변환 API 호출
│ │ ├─ domain/ # DestinationVO
│ │ └─ dto/
│ │
│ ├─ property/ # 매물·이미지·목록·지도 마커·기본 상세
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ ├─ domain/ # PropertyVO, PropertyImageVO
│ │ └─ dto/ # PropertySearchCond, List/Detail 응답
│ │
│ ├─ amenity/ # 편의시설 필터·도보 시간
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ ├─ client/ # 도보 시간/거리 외부 API
│ │ ├─ domain/ # PropertyAmenityVO
│ │ └─ dto/
│ │
│ ├─ comment/ # 댓글 CRUD·태그 집계
│ │ ├─ controller/
│ │ ├─ service/
│ │ ├─ mapper/
│ │ ├─ domain/ # PropertyCommentVO, PropertyTagVO
│ │ └─ dto/
│ │
│ └─ safety/ # 안전점수·경로·투표
│ ├─ controller/
│ ├─ service/
│ ├─ mapper/
│ ├─ client/ # ODsay, 공공 CCTV/가로등 등
│ ├─ domain/ # PropertySafetyVO, RouteVoteVO
│ └─ dto/

frontend/src/
├── api/ -- 백엔드 REST API 통신 모듈 (Axios)
│ ├── axios.js -- Axios 인스턴스 생성 (Interceptors, BaseURL 설정)
│ ├── userApi.js -- 회원/온보딩 관련 API
│ ├── propertyApi.js -- 매물/댓글/태그 API
│ └── safetyApi.js -- 안전 점수/투표 API
├── assets/ -- 이미지, 폰트, CSS 등 정적 자원
│ ├── styles/ -- global.css, reset.css, tailwind/bootstrap
│ └── images/ -- 로고, 아이콘 등
│
├── components/ -- 화면 조각 / 재사용 UI 컴포넌트
│ ├── common/ -- AppHeader, AppFooter, BaseButton, BaseModal
│ ├── property/ -- PropertyCard, PropertyImageList, TagBadge, CommentList
│ ├── map/ -- NaverMap, MarkerOverlay, RoutePolyline
│ └── user/ -- BookmarkList, ProfileCard
│
├── pages/ -- 기능별 폴더로 분리된 페이지 라우팅 단위
│ ├── home/
│ │ └── HomePage.vue -- 메인 지도 & 검색 페이지
│ │
│ ├── auth/
│ │ ├── LoginPage.vue -- 로그인 페이지
│ │ └── SignupPage.vue -- 회원가입 페이지
│ │
│ ├── onboarding/
│ │ └── OnboardingPage.vue -- 온보딩 (출퇴근/예산/안전 선호도 설정)
│ │
│ ├── property/
│ │ └── PropertyDetailPage.vue -- 매물 상세 정보 페이지
│ │

│ │
│ └── mypage/
│ └── MyMainPage.vue -- 마이페이지 메인 (내 정보 관리)
│
├── router/ -- Vue Router 설정
│ └── index.js -- 페이지 경로 정의
│
├── stores/ -- Pinia 상태 관리
│ ├── auth.js -- 로그인 유저 정보 및 토큰
│ ├── property.js -- 현재 선택된 매물, 검색 필터 조건
│ └── map.js -- 지도 중심 좌표, 레벨 상태
│
│
├── utils/ -- 공통 유틸리티 함수
│ ├── formatter.js -- 가격(만원 -> 억/만원 변환), 날짜 포맷팅
│ └── mapUtils.js -- 위경도 좌표 계산, 반경 계산 유틸
│
├── App.vue -- 루트 컴포넌트
└── main.js -- 앱 진입점 (Pinia, Router 등록)
