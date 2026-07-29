# 🏗️ [살고싶오] 프로젝트 스캐폴딩(Scaffolding) & 보일러플레이트 가이드 (Markdown)

본 문서는 **살고싶오(29반 1팀) - 빌라촌 안심 주거 매칭 플랫폼** 프로젝트 개발 착수 시, 프론트엔드(Vue 3 + Vite + Tailwind CSS), 백엔드(Java 17 + Spring Legacy + MyBatis), DB, API 및 E2E 테스트(Playwright) 디렉토리 구조와 **초기 뼈대(Scaffolding)**를 한 번에 세우기 위한 표준 설치 및 아키텍처 명세서입니다.

---

## 📌 0. 스캐폴딩(Scaffolding) 핵심 목적 및 팀 체계

1. **디렉토리 체계 단일화**: 팀원 5명(구혜성, 안효선, 김민준, 김병승, 송은지) 간의 폴더 구조 및 파일 위치 혼선 완전 방지.
2. **도메인 중심(Domain-driven) 설계로 개선**: 계층별(Controller/Service/Mapper) 단순 분리를 넘어 도메인(Feature) 단위의 응집도 높은 패키지 구조로 발전시켜, 기능 추가 및 유지보수 효율성 극대화.
3. **공통 보일러플레이트 초기 세팅**: Axios 인스턴스, JWT 인터셉터, Pinia 전역 상태, Spring 예외 처리기(`GlobalExceptionHandler`), `ApiResponse` 래퍼, MyBatis 맵퍼 초기 뼈대 구축.
4. **E2E 및 테스트 뼈대 세팅**: 개발 완료 전 미리 Playwright 테스트 스캐폴딩 환경 구성.

---

## 📱 1. Frontend (Vue 3 + Vite + Tailwind CSS) 스캐폴딩 뼈대

### ■ 디렉토리 구조 명세 (`salgosipo-fe/`)

```
salgosipo-fe/
├── public/                     # 정적 에셋 (favicon, map markers)
├── src/
│   ├── api/ (또는 services/)   # Axios API 통신 서비스 모듈
│   │   ├── api.js (axios.js)   # Axios Base Instance + JWT Header Interceptor
│   │   ├── authService.js (userApi.js)     # 로그인, 회원가입, ID/PW 찾기 API
│   │   ├── onboardingService.js            # 온보딩 조건 등록 및 조회 API
│   │   ├── propertyService.js (propertyApi.js) # 매물/목록/상세/댓글/태그 API
│   │   └── safetyService.js (safetyApi.js)     # CPTED 안전 점수/안심 경로/투표 API
│   ├── assets/                 # 공통 스타일, 이미지, 폰트 정적 자원
│   │   ├── main.css            # Tailwind CSS & Global Design Tokens (styles/global.css, reset.css)
│   │   ├── icons/              # SVG 아이콘
│   │   └── images/             # 로고 및 매물 대표 이미지
│   ├── components/             # 재사용 가능한 UI 컴포넌트
│   │   ├── common/             # 공통 버튼, 인풋, 모달, 토스트, 헤더/푸터
│   │   │   ├── BaseButton.vue
│   │   │   ├── BaseInput.vue
│   │   │   ├── BaseModal.vue
│   │   │   ├── AppHeader.vue
│   │   │   └── AppFooter.vue
│   │   ├── auth/               # 로그인, 회원가입, 찾기 모달/폼 컴포넌트
│   │   ├── onboarding/         # 온보딩 Step 1~5 (출퇴근/예산/안전 선호도) 단계별 컴포넌트
│   │   ├── map/                # 지도 렌더링, 핀, 오버레이, 안심 경로, 필터 바텀시트
│   │   │   ├── NaverMap.vue
│   │   │   ├── IsochroneMap.vue
│   │   │   ├── PropertyPin.vue
│   │   │   ├── MarkerOverlay.vue
│   │   │   ├── RoutePolyline.vue
│   │   │   └── AmenityFilterSheet.vue
│   │   ├── property/           # 매물 카드, 이미지 리스트, 태그 배지, 댓글 리스트
│   │   │   ├── PropertyCard.vue
│   │   │   ├── PropertyImageList.vue
│   │   │   ├── TagBadge.vue
│   │   │   └── CommentList.vue
│   │   ├── detail/             # 우측 슬라이딩 도어 상세 컴포넌트
│   │   │   ├── SlidingDoorPanel.vue
│   │   │   ├── SafetyRadarChart.vue
│   │   │   └── CommentSection.vue
│   │   └── mypage/ (user/)     # 마이페이지, 프로필 카드, 관심 매물 목록 & 3열 매물 비교 컴포넌트
│   │       ├── ProfileCard.vue
│   │       └── BookmarkList.vue
│   ├── views/ (pages/)         # 라우트 대표 페이지 뷰
│   │   ├── AuthView.vue (LoginPage.vue, SignupPage.vue)
│   │   ├── OnboardingView.vue (OnboardingPage.vue)
│   │   ├── MapView.vue (HomePage.vue - 메인 지도 & 검색 페이지)
│   │   ├── PropertyDetailPage.vue (매물 상세 정보 페이지)
│   │   └── MyPageView.vue (MyMainPage.vue - 내 정보 관리 및 관심 매물)
│   ├── stores/                 # Pinia 전역 상태 관리
│   │   ├── useAuthStore.js     # 사용자 로그인 유저 정보 및 토큰 상태
│   │   ├── useOnboardingStore.js # 온보딩 수집 정보 및 단계 상태
│   │   ├── usePropertyStore.js # 현재 선택된 매물 및 검색 필터 조건
│   │   └── useMapStore.js      # 지도 중심 좌표, 레벨, 핀 선택 상태
│   ├── router/                 # Vue Router v4 라우팅 가드 및 경로 정의
│   │   └── index.js
│   ├── utils/                  # 공통 유틸리티 (포맷터, 좌표 연산, 상수)
│   │   ├── formatters.js       # 통화/가격(만원->억/만원), 도보시간 포맷터
│   │   ├── mapUtils.js         # 위경도 좌표 연산, 반경/거리 계산 유틸
│   │   └── constants.js        # 시스템 공통 상수 명세
│   ├── App.vue                 # Root Component
│   └── main.js                 # Vue App Entry Point (Pinia, Router 등록)
├── package.json
├── tailwind.config.js
└── vite.config.js
```

---

## ☕ 2. Backend (Java 17 + Spring Legacy + MyBatis) 스캐폴딩 뼈대

### ■ 디렉토리 구조 명세 (`salgosipo-be/`)

> **[개선 사항]**: 기존 계층(Layer) 중심 구조에서 기능 확장성이 뛰어난 **도메인(Feature/Domain-driven) 패키지 구조**로 통합 전환하여 각 기능 패키지 내부에 Controller, Service, Mapper, VO, DTO를 함께 배치했습니다.

```
salgosipo-be/
├── src/
│   ├── main/
│   │   ├── java/com/salgosipo/
│   │   │   ├── global/                 # 공통/인프라: 담당자 공동, 최초 1회 설정
│   │   │   │   ├── config/             # RootConfig, ServletConfig, WebConfig, SecurityConfig, MyBatisConfig, SwaggerConfig
│   │   │   │   ├── security/           # JwtTokenProvider, JWT 인증 필터
│   │   │   │   ├── response/           # ApiResponse.java, ErrorResponse.java
│   │   │   │   ├── exception/          # GlobalExceptionHandler.java (전역 예외 처리기)
│   │   │   │   └── util/               # 공통 유틸리티
│   │   │   │
│   │   │   ├── auth/                   # [도메인] 로그인·로그아웃·ID/PW 찾기
│   │   │   │   ├── controller/         # AuthController.java
│   │   │   │   ├── service/            # AuthService.java
│   │   │   │   ├── mapper/             # AuthMapper.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── user/                   # [도메인] 회원가입·프로필·회원 탈퇴
│   │   │   │   ├── controller/         # UserController.java
│   │   │   │   ├── service/            # UserService.java
│   │   │   │   ├── mapper/             # UserMapper.java
│   │   │   │   ├── domain/             # UserVO.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── bookmark/               # [도메인] 관심 매물 (즐겨찾기 목록 소유 및 관리)
│   │   │   │   ├── controller/         # BookmarkController.java
│   │   │   │   ├── service/            # BookmarkService.java
│   │   │   │   ├── mapper/             # BookmarkMapper.java
│   │   │   │   ├── domain/             # BookmarkVO.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── onboarding/             # [도메인] 조건 등록·수정·초기화·조회
│   │   │   │   ├── controller/         # OnboardingController.java
│   │   │   │   ├── service/            # OnboardingService.java
│   │   │   │   ├── mapper/             # OnboardingMapper.java
│   │   │   │   ├── domain/             # OnboardingVO.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── destination/            # [도메인] 목적지 키워드·주소·좌표
│   │   │   │   ├── controller/         # DestinationController.java
│   │   │   │   ├── service/            # DestinationService.java
│   │   │   │   ├── mapper/             # DestinationMapper.java
│   │   │   │   ├── client/             # 주소/좌표 변환 외부 API 호출 Client
│   │   │   │   ├── domain/             # DestinationVO.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── property/               # [도메인] 매물·이미지·목록·지도 마커·상세
│   │   │   │   ├── controller/         # PropertyController.java
│   │   │   │   ├── service/            # PropertyService.java
│   │   │   │   ├── mapper/             # PropertyMapper.java
│   │   │   │   ├── domain/             # PropertyVO.java, PropertyImageVO.java
│   │   │   │   └── dto/                # PropertySearchCond, List/Detail 응답 DTO
│   │   │   │
│   │   │   ├── amenity/                # [도메인] 편의시설 필터·도보 시간
│   │   │   │   ├── controller/         # AmenityController.java
│   │   │   │   ├── service/            # AmenityService.java
│   │   │   │   ├── mapper/             # AmenityMapper.java
│   │   │   │   ├── client/             # 도보 시간/거리 외부 API 호출 Client
│   │   │   │   ├── domain/             # PropertyAmenityVO.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── comment/                # [도메인] 댓글 CRUD·태그 집계
│   │   │   │   ├── controller/         # CommentController.java
│   │   │   │   ├── service/            # CommentService.java
│   │   │   │   ├── mapper/             # CommentMapper.java
│   │   │   │   ├── domain/             # PropertyCommentVO.java, PropertyTagVO.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   └── safety/                 # [도메인] 안전점수·경로·투표
│   │   │       ├── controller/         # SafetyController.java
│   │   │       ├── service/            # SafetyService.java (CPTED 감산 알고리즘 연산)
│   │   │       ├── mapper/             # SafetyMapper.java
│   │   │       ├── client/             # ODsay, 공공 CCTV/가로등 외부 API Client
│   │   │       ├── domain/             # PropertySafetyVO.java, RouteVoteVO.java
│   │   │       └── dto/
│   │   │
│   │   └── resources/
│   │       ├── mapper/                 # MyBatis XML Mappers (SQL 쿼리 매핑)
│   │       │   ├── AuthMapper.xml
│   │       │   ├── UserMapper.xml
│   │       │   ├── BookmarkMapper.xml
│   │       │   ├── OnboardingMapper.xml
│   │       │   ├── DestinationMapper.xml
│   │       │   ├── PropertyMapper.xml
│   │       │   ├── AmenityMapper.xml
│   │       │   ├── CommentMapper.xml
│   │       │   └── SafetyMapper.xml
│   │       ├── application.properties  # DB 접속 및 외부 API 키
│   │       └── log4j2.xml
│   └── test/                           # JUnit 5 테스트
├── pom.xml (또는 build.gradle)
└── Dockerfile                          # 배포용 Docker 컨테이너 설정
```

---

## 🧪 3. E2E & 테스트 스캐폴딩 뼈대 (Playwright Setup)

```
salgosipo-fe/
├── tests/
│   ├── e2e/                           # Playwright 시나리오 테스트 스펙
│   │   ├── auth.spec.js               # 가입 & 로그인 테스트
│   │   ├── onboarding.spec.js         # 온보딩 Step 1~5 테스트
│   │   ├── map-filter.spec.js         # 지도 & 편의시설 필터 테스트
│   │   └── property-detail.spec.js    # 매물 상세 슬라이딩 도어 테스트
│   └── fixtures/                      # Mock Data
└── playwright.config.js               # E2E 뷰포트 및 BaseURL 설정
```

---

## 🚀 4. 초기 스캐폴딩 자동 생성을 위한 명령어 (CLI Setup)

### 1) Vue 3 프론트엔드 스캐폴딩 초기화

```bash
# Vite 기반 Vue 3 + JavaScript 초기화
npm create vite@latest salgosipo-fe -- --template vue

# 필수 패키지 설치 (Pinia, Router, Axios, Tailwind)
cd salgosipo-fe
npm install pinia vue-router axios
npm install -D tailwindcss postcss autoprefixer @playwright/test
npx tailwindcss init -p
```

### 2) Playwright E2E 테스트 스캐폴딩 초기화

```bash
# Playwright 테스트 아키텍처 세팅
npx playwright install
```
