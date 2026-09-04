<template>
  <div class="navbar">
    <breadcrumb />
    <div class="navbar__right">
      <el-tag v-for="role in userStore.roles" :key="role" size="small" type="info" effect="plain">
        {{ role }}
      </el-tag>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="navbar__user">
          <el-avatar :size="28" :src="userStore.avatar || defaultAvatar" class="navbar__avatar">
            {{ (userStore.displayName || 'A').charAt(0) }}
          </el-avatar>
          <span class="navbar__name">{{ userStore.displayName }}</span>
          <el-icon><arrow-down /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="info" disabled>
              {{ userStore.username || '-' }}
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import Breadcrumb from './Breadcrumb.vue'

const defaultAvatar = ''

const userStore = useUserStore()
const router = useRouter()

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
      .then(async () => {
        await userStore.logout()
        router.replace('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid var(--rh-border);
}

.navbar__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.navbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
  color: var(--rh-text);
}

.navbar__avatar {
  background: var(--rh-primary);
  font-size: 13px;
}

.navbar__name {
  font-size: 13px;
}
</style>
