import { createRouter, createWebHistory } from 'vue-router';
import AuthPage from '../pages/auth/AuthPage.vue';
import OnBoardingPage from '../pages/onboarding/OnBoardingPage.vue';
import MyPagePage from '../pages/mypage/MyPagePage.vue';
import HomePage from "@/pages/home/HomePage.vue";
import NotFoundPage from '@/pages/error/NotFoundPage.vue';
import SafetyDebugMapPage from '@/pages/devtools/SafetyDebugMapPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: AuthPage },
    { path: '/signup', name: 'signup', component: AuthPage },
    { path: '/onboarding', name: 'onboarding', component: OnBoardingPage },
    { path: '/home', name: 'home', component: HomePage, alias: '/map' },
    { path: '/map', name: 'map', component: HomePage },
    { path: '/mypage', name: 'mypage', component: MyPagePage },
    { path: '/dev/safety-debug', name: 'safety-debug', component: SafetyDebugMapPage },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundPage },
  ],
});

export default router;
