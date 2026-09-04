<template>
  <MainLayout>
    <div class="rh-container user-layout">
      <aside class="user-side rh-card">
        <div class="user-side__profile">
          <el-avatar :size="56" :src="userStore.profile && userStore.profile.avatar">
            {{ (userStore.nickname || '我').charAt(0) }}
          </el-avatar>
          <div class="user-side__name">{{ userStore.nickname || '用户' }}</div>
        </div>
        <nav class="user-side__nav">
          <router-link
            v-for="item in menus"
            :key="item.path"
            :to="item.path"
            class="user-side__link"
            active-class="user-side__link--active"
          >
            {{ item.label }}
          </router-link>
        </nav>
      </aside>
      <div class="user-main">
        <router-view />
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const menus = [
  { path: '/user/profile', label: '个人资料' },
  { path: '/user/favorites', label: '我的收藏' },
  { path: '/user/history', label: '浏览历史' },
  { path: '/user/posts', label: '我的帖子' },
  { path: '/user/comments', label: '我的评论' },
  { path: '/user/follows', label: '我的关注' },
  { path: '/user/inquiries', label: '我的询价' },
  { path: '/user/messages', label: '我的消息' }
]

onMounted(() => {
  if (!userStore.profile) {
    userStore.loadProfile()
  }
  userStore.loadStats()
})
</script>

<style scoped lang="scss">
.user-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  align-items: start;
}

.user-side__profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--rh-border-light);
  margin-bottom: 12px;
}

.user-side__name {
  font-weight: 600;
  font-size: 15px;
}

.user-side__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-side__link {
  padding: 10px 12px;
  border-radius: 6px;
  color: var(--rh-text-sub);
  font-size: 14px;
}

.user-side__link:hover {
  background: var(--rh-surface-sub);
  color: var(--rh-primary);
}

.user-side__link--active {
  background: var(--rh-primary-light);
  color: var(--rh-primary);
  font-weight: 600;
}
</style>
