<template>
  <MainLayout>
    <div class="rh-container not-found">
      <div class="rh-card not-found__card">
        <h1>404</h1>
        <p>页面不存在或已被移除</p>
        <router-link to="/"><el-button type="primary">返回首页</el-button></router-link>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import MainLayout from '@/layout/MainLayout.vue'
</script>

<style scoped>
.not-found { padding: 80px 0; }
.not-found__card { text-align: center; padding: 64px 24px; }
.not-found__card h1 { margin: 0; font-size: 72px; color: var(--rh-primary); }
.not-found__card p { color: var(--rh-text-sub); margin: 16px 0 24px; }
</style>
