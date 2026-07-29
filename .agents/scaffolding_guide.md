# 🏗️ [살고싶오] 프로젝트 스캐폴딩(Scaffolding) & 보일러플레이트 가이드 (Markdown)

본 문서는 **살고싶오(29반 1팀) - 빌라촌 안심 주거 매칭 플랫폼** 프로젝트 개발 착수 시, 프론트엔드(Vue 3), 백엔드(Spring), DB, API 및 테스트(Playwright) 디렉토리 구조와 **초기 뼈대(Scaffolding)**를 한 번에 세우기 위한 표준 설치 및 아키텍처 명세서입니다.

---

## 📌 0. 스캐폴딩(Scaffolding) 핵심 목적

1. **디렉토리 체계 단일화**: 팀원 5명(구혜성, 안효선, 김민준, 김병승, 송은지) 간의 폴더 구조 및 파일 위치 혼선 방지.
2. **공통 보일러플레이트 초기 세팅**: Axios 인스턴스, JWT 인터셉터, Pinia 스토어, Spring 예외 처리기, MyBatis 맵퍼 초기 뼈대 구축.
3. **E2E 및 테스트 뼈대 세팅**: 개발 완료 전 미리 Playwright 테스트 스캐폴딩 환경 구성.

---

## 📱 1. Frontend (Vue 3 + Vite + Tailwind CSS) 스캐폴딩 뼈대

### ■ 디렉토리 구조 명세 (`salgosipo-fe/`)

```
salgosipo-fe/
├── public/                     # 정적 에셋 (favicon, map markers)
├── src/
│   ├── assets/                 # 공통 스타일, 이미지, 폰트
│   │   ├── main.css            # Tailwind CSS & Global Design Tokens
│   │   └── icons/              # SVG 아이콘
│   ├── components/             # 재사용 가능한 UI 컴포넌트
│   │   ├── common/             # 공통 버튼, 인풋, 모달, 토스트
│   │   │   ├── BaseButton.vue
│   │   │   ├── BaseInput.vue
│   │   │   └── BaseModal.vue
│   │   ├── auth/               # 로그인, 가입, 찾기 컴포넌트
│   │   ├── onboarding/         # 온보딩 Step 1~5 컴포넌트
│   │   ├── map/                # 지도 렌더링, 핀, 필터 바텀시트
│   │   │   ├── IsochroneMap.vue
│   │   │   ├── PropertyPin.vue
│   │   │   └── AmenityFilterSheet.vue
│   │   ├── detail/             # 우측 슬라이딩 도어 상세 컴포넌트
│   │   │   ├── SlidingDoorPanel.vue
│   │   │   ├── SafetyRadarChart.vue
│   │   │   └── CommentSection.vue
│   │   └── mypage/             # 마이페이지 & 3열 매물 비교 컴포넌트
│   ├── views/                  # 라우트 대표 페이지 뷰
│   │   ├── AuthView.vue
│   │   ├── OnboardingView.vue
│   │   ├── MapView.vue
│   │   └── MyPageView.vue
│   ├── stores/                 # Pinia 전역 상태 관리
│   │   ├── useAuthStore.js     # 사용자 로그인 토큰 상태
│   │   ├── useOnboardingStore.js # 온보딩 수집 정보 상태
│   │   └── useMapStore.js      # 선택 매물 및 필터 상태
│   ├── router/                 # Vue Router v4 라우팅 가드
│   │   └── index.js
│   ├── services/               # Axios API 통신 서비스 모듈
│   │   ├── api.js              # Axios Base Instance + JWT Header Interceptor
│   │   ├── authService.js
│   │   ├── onboardingService.js
│   │   ├── propertyService.js
│   │   └── safetyService.js
│   ├── types/                  # TypeScript 인터페이스 정의
│   │   ├── user.js
│   │   ├── onboarding.js
│   │   ├── property.js
│   │   └── safety.js
│   ├── utils/                  # 공통 유틸리티 (포맷터, 상수)
│   │   ├── formatters.js       # 통화/도보시간 포맷터
│   │   └── constants.js
│   ├── App.vue                 # Root Component
│   └── main.js                 # Vue App Entry Point
├── package.json
├── tailwind.config.js
└── vite.config.js
```

---

## ☕ 2. Backend (Java 17 + Spring + MyBatis) 스캐폴딩 뼈대

### ■ 디렉토리 구조 명세 (`salgosipo-be/`)

```
salgosipo-be/
├── src/
│   ├── main/
│   │   ├── java/com/salgosipo/
│   │   │   ├── config/                 # 공통 설비 클래스
│   │   │   │   ├── WebConfig.java      # CORS, Interceptor 설정
│   │   │   │   ├── SecurityConfig.java # JWT Filter, Security 설정
│   │   │   │   ├── MyBatisConfig.java  # DB Connection Pool 설정
│   │   │   │   └── SwaggerConfig.java  # API 문서화 설정
│   │   │   ├── controller/             # REST Controller
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── OnboardingController.java
│   │   │   │   ├── PropertyController.java
│   │   │   │   └── SafetyController.java
│   │   │   ├── service/                # Business Logic Services
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── OnboardingService.java
│   │   │   │   ├── PropertyService.java
│   │   │   │   └── SafetyService.java  # CPTED 감산 알고리즘 연산
│   │   │   ├── mapper/                 # MyBatis Mapper Interfaces
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── OnboardingMapper.java
│   │   │   │   ├── PropertyMapper.java
│   │   │   │   └── SafetyMapper.java
│   │   │   ├── dto/                    # Request / Response DTO
│   │   │   │   ├── req/
│   │   │   │   └── res/
│   │   │   ├── domain/                 # Entity Models
│   │   │   └── global/                 # 공통 처리기
│   │   │       ├── common/ApiResponse.java
│   │   │       ├── error/GlobalExceptionHandler.java
│   │   │       └── jwt/JwtTokenProvider.java
│   │   └── resources/
│   │       ├── mapper/                 # MyBatis XML Mappers
│   │       │   ├── UserMapper.xml
│   │       │   ├── OnboardingMapper.xml
│   │       │   ├── PropertyMapper.xml
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
# Vite 기반 Vue 3 + TypeScript 초기화
npm create vite@latest salgosipo-fe -- --template vue-ts

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
