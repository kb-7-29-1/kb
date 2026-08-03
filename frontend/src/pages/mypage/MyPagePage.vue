<script setup>
import OnboardingPanel from '@/components/mypage/OnboardingPanel.vue';
</script>

<template>
  <main>
    <h1>마이페이지</h1>
    <RouterLink to="/map"> 지도로 돌아가기 </RouterLink>

    <OnboardingPanel />
  </main>
</template>
