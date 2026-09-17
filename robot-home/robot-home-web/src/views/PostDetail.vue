<template>
  <MainLayout>
    <div v-if="detail" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'community' }">社区</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.title || '帖子详情' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card post-detail">
        <div class="post-detail__head">
          <el-avatar :size="44" :src="detail.author && detail.author.avatar">
            {{ ((detail.author && detail.author.nickname) || '?').charAt(0) }}
          </el-avatar>
          <div>
            <div class="post-detail__author">{{ detail.author && detail.author.nickname }}</div>
            <div class="rh-text-light">
              {{ fromNow(detail.createTime) }}
              <template v-if="detail.circleName"> · {{ detail.circleName }}</template>
            </div>
          </div>
          <el-button
            v-if="detail.canDelete"
            class="post-detail__delete"
            type="danger"
            text
            @click="removePost"
          >
            删除
          </el-button>
        </div>

        <h1 class="post-detail__title">{{ detail.title }}</h1>
        <div v-if="detail.topic" class="post-detail__topic">#{{ detail.topic }}</div>
        <div class="post-detail__content">{{ detail.content }}</div>

        <div v-if="detail.images && detail.images.length" class="post-detail__images">
          <img v-for="(img, idx) in detail.images" :key="idx" :src="imageOf(img)" alt="" loading="lazy" />
        </div>

        <div v-if="detail.robotId" class="post-detail__robot">
          <router-link :to="'/robot/' + detail.robotId" class="post-detail__robot-link">
            <img :src="imageOf(detail.robotCover)" :alt="detail.robotName" loading="lazy" />
            <span>关联机器人：{{ detail.robotName }}</span>
          </router-link>
        </div>

        <div class="post-detail__actions">
          <el-button plain @click="toggleLike">
            {{ detail.liked ? '已点赞' : '点赞' }} {{ detail.likeCount || 0 }}
          </el-button>
          <el-button :type="detail.favorited ? 'primary' : 'default'" plain @click="toggleFavorite">
            {{ detail.favorited ? '已收藏' : '收藏' }}
          </el-button>
          <span class="rh-text-light">{{ formatCount(detail.viewCount) }} 浏览 · {{ formatCount(detail.commentCount) }} 评论</span>
        </div>
      </div>

      <section class="rh-card rh-section">
        <CommentPanel biz-type="post" :biz-id="id" />
      </section>
    </div>
    <div v-else-if="error" class="rh-container"><div class="rh-empty">{{ error }}</div></div>
    <div v-else class="rh-container"><div class="rh-empty">加载中…</div></div>
  </MainLayout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { communityApi, favoriteApi, likeApi } from '@/api'
import { useUserStore } from '@/store/user'
import { formatCount, fromNow, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))
const detail = ref(null)
const error = ref('')

async function load () {
  detail.value = null
  error.value = ''
  try {
    const data = await communityApi.detail(id.value)
    if (!data) {
      error.value = '帖子不存在'
      return
    }
    detail.value = data
    setPageMeta({
      title: (data.title || '帖子详情') + ' - 社区 - 机器人之家',
      description: (data.content || '').slice(0, 120)
    })
  } catch (e) {
    error.value = '帖子加载失败或不存在'
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

async function toggleLike () {
  if (!requireLogin()) return
  const data = await likeApi.toggle('post', id.value)
  detail.value.liked = data.liked
  detail.value.likeCount = (detail.value.likeCount || 0) + (data.liked ? 1 : -1)
}

async function toggleFavorite () {
  if (!requireLogin()) return
  const data = await favoriteApi.toggle('post', id.value)
  detail.value.favorited = data.favorited
  ElMessage.success(data.favorited ? '已收藏' : '已取消收藏')
}

async function removePost () {
  await ElMessageBox.confirm('确定删除该帖子吗？', '提示', { type: 'warning' })
  await communityApi.remove(id.value)
  ElMessage.success('已删除')
  router.replace('/community')
}

watch(id, load, { immediate: true })
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.post-detail__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.post-detail__author {
  font-weight: 600;
}

.post-detail__delete {
  margin-left: auto;
}

.post-detail__title {
  font-size: 24px;
  margin: 0 0 8px;
}

.post-detail__topic {
  color: var(--rh-primary);
  margin-bottom: 12px;
}

.post-detail__content {
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-detail__images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.post-detail__images img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 6px;
  background: var(--rh-surface-sub);
}

.post-detail__robot {
  margin-top: 16px;
}

.post-detail__robot-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
}

.post-detail__robot-link img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}

.post-detail__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--rh-border-light);
}
</style>
