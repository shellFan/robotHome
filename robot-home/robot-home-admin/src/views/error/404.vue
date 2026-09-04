<template>
  <div class="not-found">
    <h2 class="not-found__code">404</h2>
    <p class="not-found__text">页面不存在或已被移除</p>
    <el-button type="primary" @click="$router.replace('/dashboard')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
}

.not-found__code {
  margin: 0;
  font-size: 56px;
  color: var(--rh-text-secondary);
}

.not-found__text {
  margin: 0 0 12px;
  color: var(--rh-text-weak);
}
</style>
