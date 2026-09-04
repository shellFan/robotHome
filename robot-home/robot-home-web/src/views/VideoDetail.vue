<template>
  <MainLayout>
    <div v-if="detail" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'videos' }">视频</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-grid rh-grid--2 detail-layout">
        <div>
          <section class="rh-card video-detail">
            <div class="video-detail__player">
              <video v-if="detail.url" :src="detail.url" :poster="imageOf(detail.cover)" controls />
              <img v-else :src="imageOf(detail.cover)" :alt="detail.title" />
            </div>
            <h1 class="video-detail__title">{{ detail.title }}</h1>
            <div class="video-detail__meta rh-text-light">
              <span v-if="detail.categoryName">{{ detail.categoryName }}</span>
              <span v-if="detail.author">{{ detail.author }}</span>
              <span>{{ formatDate(detail.publishTime, 'YYYY-MM-DD HH:mm') }}</span>
              <span>{{ formatCount(detail.viewCount) }} 次播放</span>
              <span v-if="detail.duration">时长 {{ formatDuration(detail.duration) }}</span>
            </div>
            <p v-if="detail.summary" class="video-detail__summary">{{ detail.summary }}</p>
            <div class="video-detail__actions">
              <el-button :type="detail.favorited ? 'primary' : 'default'" plain @click="toggleFavorite">
                {{ detail.favorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button plain @click="toggleLike">
                {{ liked ? '已点赞' : '点赞' }} {{ likeCount }}
              </el-button>
              <router-link v-if="detail.robotId" :to="'/robot/' + detail.robotId">
                <el-button plain>相关机器人</el-button>
              </router-link>
            </div>
          </section>

          <section class="rh-card rh-section">
            <CommentPanel biz-type="video" :biz-id="id" />
          </section>
        </div>

        <aside>
          <section v-if="related.length" class="rh-card rh-section">
            <h2 class="rh-section__title">相关推荐</h2>
            <router-link
              v-for="item in related"
              :key="item.id"
              :to="'/video/' + item.id"
              class="side-item"
            >
              <img :src="imageOf(item.cover)" :alt="item.title" />
              <span class="rh-clamp-2">{{ item.title }}</span>
            </router-link>
          </section>
        </aside>
      </div>
    </div>
    <div v-else-if="error" class="rh-container"><div class="rh-empty">{{ error }}</div></div>
    <div v-else class="rh-container"><div class="rh-empty">加载中…</div></div>
  </MainLayout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { videoApi, favoriteApi, likeApi } from '@/api'
import { useUserStore } from '@/store/user'
import { formatCount, formatDate, formatDuration, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))
const detail = ref(null)
const related = ref([])
const liked = ref(false)
const likeCount = ref(0)
const error = ref('')

async function load () {
  detail.value = null
  error.value = ''
  try {
    const data = await videoApi.detail(id.value)
    if (!data) {
      error.value = '视频不存在'
      return
    }
    detail.value = data
    related.value = data.related || []
    liked.value = !!data.liked
    likeCount.value = data.likeCount || 0
    setPageMeta({
      title: data.title + ' - 视频 - 机器人之家',
      description: data.summary || data.title
    })
  } catch (e) {
    error.value = '视频加载失败或不存在'
  }
}

function requireLogin () {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function toggleFavorite () {
  if (!requireLogin()) return
  const data = await favoriteApi.toggle('video', id.value)
  detail.value.favorited = data.favorited
  ElMessage.success(data.favorited ? '已收藏' : '已取消收藏')
}

async function toggleLike () {
  if (!requireLogin()) return
  const data = await likeApi.toggle('video', id.value)
  liked.value = data.liked
  likeCount.value = likeCount.value + (data.liked ? 1 : -1)
}

watch(id, load, { immediate: true })
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.detail-layout {
  align-items: start;
}

.video-detail__player {
  background: #000;
  border-radius: var(--rh-radius);
  overflow: hidden;
  margin-bottom: 16px;
}

.video-detail__player video,
.video-detail__player img {
  width: 100%;
  max-height: 480px;
  display: block;
  object-fit: contain;
  background: #000;
}

.video-detail__title {
  font-size: 22px;
  margin: 0 0 10px;
}

.video-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  margin-bottom: 12px;
}

.video-detail__summary {
  color: var(--rh-text-sub);
  margin: 0 0 16px;
}

.video-detail__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.side-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--rh-border-light);
  font-size: 13px;
}

.side-item:last-child {
  border-bottom: none;
}

.side-item img {
  width: 96px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.side-item:hover span {
  color: var(--rh-primary);
}
</style>
