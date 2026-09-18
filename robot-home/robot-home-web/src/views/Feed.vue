<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>关注动态</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card">
        <h2 class="rh-section__title" style="margin-bottom: 20px">关注动态</h2>

        <div v-if="loading" class="rh-empty">加载中…</div>
        <div v-else-if="!feedItems.length" class="rh-empty">
          暂无动态，去<a href="/robots" style="color: var(--rh-primary)">发现机器人</a>关注一下吧
        </div>
        <div v-else class="feed-list">
          <router-link
            v-for="item in feedItems"
            :key="item.id"
            :to="feedLink(item)"
            class="feed-item"
          >
            <div class="feed-item__meta">
              <span class="feed-item__type">{{ feedTypeLabel(item.feedType) }}</span>
              <span class="feed-item__source">{{ item.sourceName }}</span>
              <span class="rh-text-light">{{ formatDate(item.createTime) }}</span>
            </div>
            <div class="feed-item__title rh-clamp-2">{{ item.title }}</div>
            <div v-if="item.summary" class="feed-item__summary rh-clamp-2 rh-text-light">{{ item.summary }}</div>
          </router-link>
        </div>

        <div v-if="hasMore" style="text-align: center; margin-top: 20px">
          <el-button text type="primary" @click="loadMore" :loading="loadingMore">加载更多</el-button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import { feedApi } from '@/api'
import { formatDate } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const feedItems = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const lastId = ref(null)

function feedTypeLabel (type) {
  const map = { POST: '帖子', ARTICLE: '文章', QUESTION: '问答' }
  return map[type] || type
}

function feedLink (item) {
  if (item.feedType === 'ARTICLE') return '/article/' + item.targetId
  if (item.feedType === 'QUESTION') return '/qa/' + item.targetId
  return '/community/' + item.targetId
}

async function load (append = false) {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await feedApi.mine(lastId.value, 20)
    const items = res.list || []
    if (append) {
      feedItems.value = [...feedItems.value, ...items]
    } else {
      feedItems.value = items
    }
    hasMore.value = items.length >= 20
    if (items.length > 0) {
      lastId.value = items[items.length - 1].id
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore () {
  load(true)
}

onMounted(() => {
  setPageMeta({ title: '关注动态 - 机器人之家' })
  load()
})
</script>

<style scoped lang="scss">
.feed-list { display: flex; flex-direction: column; gap: 12px; }
.feed-item {
  display: block; padding: 16px; border-radius: var(--rh-radius);
  background: var(--rh-surface-sub); transition: background 0.18s;
}
.feed-item:hover { background: var(--rh-primary-light); }
.feed-item__meta { display: flex; gap: 8px; align-items: center; margin-bottom: 6px; font-size: 13px; }
.feed-item__type { padding: 2px 8px; border-radius: 3px; background: var(--rh-primary); color: #fff; font-size: 12px; }
.feed-item__source { color: var(--rh-primary); font-weight: 500; }
.feed-item__title { font-size: 15px; font-weight: 500; line-height: 1.5; }
.feed-item__summary { font-size: 13px; margin-top: 4px; }
</style>