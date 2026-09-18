<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户主页</el-breadcrumb-item>
      </el-breadcrumb>

      <div v-if="loading" class="rh-empty">加载中…</div>
      <div v-else-if="!profile" class="rh-empty">用户不存在</div>
      <div v-else class="rh-card profile-card">
        <div class="profile-header">
          <img :src="imageOf(profile.avatar)" :alt="profile.nickname" class="profile-avatar" />
          <div class="profile-info">
            <h2>{{ profile.nickname || '匿名用户' }}</h2>
            <p v-if="profile.intro" class="profile-intro">{{ profile.intro }}</p>
            <div class="profile-meta">
              <span v-if="profile.province || profile.city">{{ profile.province }}{{ profile.city }}</span>
              <span v-if="profile.contributorLevel" class="profile-level">{{ profile.contributorLevel }}</span>
            </div>
          </div>
          <el-button
            v-if="!isSelf"
            :type="profile.followed ? 'default' : 'primary'"
            size="small"
            @click="toggleFollow"
          >
            {{ profile.followed ? '已关注' : '+ 关注' }}
          </el-button>
        </div>

        <div class="profile-stats">
          <div class="stat-item">
            <span class="stat-value">{{ profile.fansCount || 0 }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ profile.followCount || 0 }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ profile.postCount || 0 }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ profile.reviewCount || 0 }}</span>
            <span class="stat-label">评测</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ profile.questionCount || 0 }}</span>
            <span class="stat-label">提问</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ profile.answerCount || 0 }}</span>
            <span class="stat-label">回答</span>
          </div>
          <div v-if="profile.contributionScore" class="stat-item">
            <span class="stat-value">{{ profile.contributionScore }}</span>
            <span class="stat-label">贡献分</span>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import { userApi, followApi } from '@/api'
import { useUserStore } from '@/store/user'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const userStore = useUserStore()
const profile = ref(null)
const loading = ref(true)

const isSelf = computed(() => {
  const me = userStore.user
  return me && me.id === Number(route.params.id)
})

async function loadProfile () {
  loading.value = true
  try {
    profile.value = await userApi.profile(route.params.id)
  } finally {
    loading.value = false
  }
}

async function toggleFollow () {
  if (!profile.value) return
  try {
    await followApi.toggle('user', profile.value.id)
    profile.value.followed = !profile.value.followed
    profile.value.fansCount = (profile.value.fansCount || 0) + (profile.value.followed ? 1 : -1)
  } catch (e) {
    // handled by interceptor
  }
}

onMounted(() => {
  setPageMeta({ title: '用户主页 - 机器人之家' })
  loadProfile()
})
</script>

<style scoped lang="scss">
.profile-card { padding: 24px; }
.profile-header { display: flex; align-items: center; gap: 20px; margin-bottom: 24px; }
.profile-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; background: var(--rh-surface-sub); }
.profile-info { flex: 1; }
.profile-info h2 { margin: 0 0 4px; font-size: 20px; }
.profile-intro { margin: 4px 0; color: var(--rh-text-light); font-size: 14px; }
.profile-meta { display: flex; gap: 8px; font-size: 13px; color: var(--rh-text-light); }
.profile-level { padding: 2px 8px; border-radius: 3px; background: var(--rh-primary-light); color: var(--rh-primary); font-size: 12px; font-weight: 500; }
.profile-stats { display: flex; gap: 24px; padding-top: 16px; border-top: 1px solid var(--rh-border-light); }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-value { font-size: 18px; font-weight: 600; }
.stat-label { font-size: 12px; color: var(--rh-text-light); margin-top: 2px; }
</style>