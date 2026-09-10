<template>
  <MainLayout>
    <div v-if="detail" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'tutorials' }">教程</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-grid rh-grid--2 detail-layout">
        <div>
          <article class="rh-card tutorial-detail">
            <h1 class="tutorial-detail__title">{{ detail.title }}</h1>
            <div class="tutorial-detail__meta rh-text-light">
              <span v-if="detail.categoryName">{{ detail.categoryName }}</span>
              <span v-if="detail.author">{{ detail.author }}</span>
              <span>{{ formatDate(detail.publishTime, 'YYYY-MM-DD HH:mm') }}</span>
              <span>{{ formatCount(detail.viewCount) }} 阅读</span>
            </div>
            <p v-if="detail.summary" class="tutorial-detail__summary">{{ detail.summary }}</p>
            <div class="rich-text" v-html="contentHtml" />
            <div class="tutorial-detail__actions">
              <el-button :type="detail.favorited ? 'primary' : 'default'" plain @click="toggleFavorite">
                {{ detail.favorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button plain @click="toggleLike">
                {{ liked ? '已点赞' : '点赞' }} {{ likeCount }}
              </el-button>
            </div>
          </article>

          <section class="rh-card rh-section">
            <CommentPanel biz-type="tutorial" :biz-id="id" />
          </section>
        </div>

        <aside>
          <section v-if="related.length" class="rh-card rh-section">
            <h2 class="rh-section__title">相关教程</h2>
            <router-link
              v-for="item in related"
              :key="item.id"
              :to="'/tutorial/' + item.id"
              class="side-item"
            >
              <img :src="imageOf(item.cover)" :alt="item.title" loading="lazy" />
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
import MarkdownIt from 'markdown-it'
import MainLayout from '@/layout/MainLayout.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { tutorialApi, favoriteApi, likeApi } from '@/api'
import { useUserStore } from '@/store/user'
import { formatCount, formatDate, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'
import { XssUtil } from '@/utils/xss'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))
const detail = ref(null)
const related = ref([])
const liked = ref(false)
const likeCount = ref(0)
const error = ref('')

const contentHtml = computed(() => {
  const raw = (detail.value && detail.value.content) || ''
  if (raw.includes('<') && raw.includes('>')) {
    return XssUtil.clean(raw)
  }
  return XssUtil.clean(md.render(raw))
})

async function load () {
  detail.value = null
  error.value = ''
  try {
    const data = await tutorialApi.detail(id.value)
    if (!data) {
      error.value = '教程不存在'
      return
    }
    detail.value = data
    related.value = data.related || []
    liked.value = !!data.liked
    likeCount.value = data.likeCount || 0
    setPageMeta({
      title: data.title + ' - 教程 - 机器人之家',
      description: data.summary || data.title
    })
  } catch (e) {
    error.value = '教程加载失败或不存在'
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
  const data = await favoriteApi.toggle('tutorial', id.value)
  detail.value.favorited = data.favorited
  ElMessage.success(data.favorited ? '已收藏' : '已取消收藏')
}

async function toggleLike () {
  if (!requireLogin()) return
  const data = await likeApi.toggle('tutorial', id.value)
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

.tutorial-detail__title {
  font-size: 26px;
  margin: 0 0 12px;
  line-height: 1.4;
}

.tutorial-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  margin-bottom: 16px;
}

.tutorial-detail__summary {
  background: var(--rh-surface-sub);
  padding: 12px 14px;
  border-radius: var(--rh-radius);
  color: var(--rh-text-sub);
  margin: 0 0 20px;
}

.rich-text {
  font-size: 15px;
  line-height: 1.9;
}

.rich-text :deep(img) {
  max-width: 100%;
}

.tutorial-detail__actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--rh-border-light);
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
