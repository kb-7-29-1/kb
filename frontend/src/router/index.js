import { createRouter, createWebHistory } from 'vue-router';
import AuthPage from '../pages/auth/AuthPage.vue';
import OnBoardingPage from '../pages/onboarding/OnBoardingPage.vue';
import MapView from '../pages/home/MapView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: AuthPage },
    { path: '/onboarding', name: 'onboarding', component: OnBoardingPage },
    { path: '/map', name: 'map', component: MapView },
  ],
});

export default router;
