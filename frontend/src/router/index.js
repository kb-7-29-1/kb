import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '@/pages/home/HomePage.vue';
import OnBoardingPage from '@/pages/onboarding/OnBoardingPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/onboarding', name: 'onboarding', component: OnBoardingPage },
  ],
});

export default router;
