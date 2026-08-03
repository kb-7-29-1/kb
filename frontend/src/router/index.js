import { createRouter, createWebHistory } from 'vue-router';
import AuthPage from '../pages/auth/AuthPage.vue';
import OnBoardingPage from '../pages/onboarding/OnBoardingPage.vue';
import MyPagePage from '../pages/mypage/MyPagePage.vue';
import HomePage from "@/pages/home/HomePage.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: AuthPage },
    { path: '/signup', name: 'signup', component: AuthPage },
    { path: '/onboarding', name: 'onboarding', component: OnBoardingPage },
    { path: '/home', name: 'home', component: HomePage, alias: '/map' },
    { path: '/map', name: 'map', component: HomePage },
    { path: '/mypage', name: 'mypage', component: MyPagePage },
  ],
});

export default router;
