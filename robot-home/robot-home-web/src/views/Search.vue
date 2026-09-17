<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>搜索</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card search-box-card">
        <el-input
          v-model="keyword"
          size="large"
          clearable
          placeholder="搜索机器人、品牌、企业、资讯、视频、教程、帖子…"
          @keyup.enter="doSearch"
          @clear="onClear"
        >
          <template #append>
            <el-button type="primary" @click="doSearch">搜索</el-button>
          </template>
        </el-input>

        <div v-if="!keyword && (hotList.length || historyList.length)" class="search-hints">
          <div v-if="hotList.length" class="search-hints__block">
            <div class="search-hints__title">热门搜索</div>
            <div class="search-hints__tags">
              <span
                v-for="item in hotList"
                :key="item.keyword"
                class="chip"
                @click="goKeyword(item.keyword)"
              >
                {{ item.keyword }}
              </span>
            </div>
          </div>
          <div v-if="historyList.length" class="search-hints__block">
            <div class="search-hints__head">
              <span class="search-hints__title">搜索历史</span>
              <el-button v-if="userStore.isLogin" text size="small" @click="clearHistory">清空</el-button>
            </div>
            <div class="search-hints__tags">
              <span
                v-for="item in historyList"
                :key="item"
                class="chip"
                @click="goKeyword(item)"
              >
                {{ item }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="keyword" class="rh-card search-result">
        <el-tabs v-model="activeType" @tab-change="onTypeChange">
          <el-tab-pane
            v-for="t in typeTabs"
            :key="t.value"
            :label="tabLabel(t)"
            :name="t.value"
          />
        </el-tabs>

        <div v-if="loading" class="rh-empty">搜索中…</div>
        <div v-else-if="!items.length" class="rh-empty">没有找到「{{ keyword }}」相关结果</div>
        <div v-else class="result-list">
          <router-link
            v-for="item in items"
            :key="item.type + '-' + item.id"
            :to="item.url || typePath(item)"
            class="result-item"
          >
            <img :src="imageOf(item.image)" :alt="item.title" loading="lazy" />
            <div class="result-item__body">
              <div class="result-item__type">{{ typeName(item.type) }}</div>
              <div class="result-item__title rh-ellipsis">{{ item.title }}</div>
              <div class="result-item__summary rh-clamp-2 rh-text-sub">{{ item.summary || item.extra }}</div>
            </div>
          </router-link>
        </div>

        <el-pagination
          v-if="activeType !== 'all' && total > pageSize"
          background
          layout="total, prev, pager, next"
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          @current-change="changePage"
        />
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { searchApi } from '@/api'
import { useUserStore } from '@/store/user'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const typeTabs = [
  { value: 'all', label: '全部' },
  { value: 'robot', label: '机器人' },
  { value: 'brand', label: '品牌' },
  { value: 'company', label: '企业' },
  { value: 'article', label: '资讯' },
  { value: 'video', label: '视频' },
  { value: 'tutorial', label: '教程' },
  { value: 'post', label: '帖子' }
]

const keyword = ref('')
const activeType = ref('all')
const loading = ref(false)
const items = ref([])
const counts = ref({})
const hotList = ref([])
const historyList = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

function resolveKeywordFromRoute () {
  return String(route.query.q || route.query.keyword || '').trim()
}

function tabLabel (t) {
  if (t.value === 'all') return t.label
  const n = counts.value[t.value]
  return n ? `${t.label}(${n})` : t.label
}

function typeName (type) {
  const map = {
    robot: '机器人',
    brand: '品牌',
    company: '企业',
    article: '资讯',
    video: '视频',
    tutorial: '教程',
    post: '帖子'
  }
  return map[type] || type
}

function typePath (item) {
  const map = {
    robot: '/robot/',
    brand: '/brand/',
    company: '/company/',
    article: '/article/',
    video: '/video/',
    tutorial: '/tutorial/',
    post: '/community/'
  }
  return (map[item.type] || '/') + item.id
}

async function loadHints () {
  try {
    const [hot, history] = await Promise.all([
      searchApi.hot(10),
      userStore.isLogin ? searchApi.history(10).catch(() => []) : Promise.resolve([])
    ])
    hotList.value = hot || []
    historyList.value = history || []
  } catch (e) {
    hotList.value = []
    historyList.value = []
  }
}

async function load () {
  const kw = keyword.value.trim()
  if (!kw) {
    items.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    if (activeType.value === 'all') {
      const data = await searchApi.search(kw, 8)
      counts.value = (data && data.counts) || {}
      const merged = []
      ;['robots', 'brands', 'companies', 'articles', 'videos', 'tutorials', 'posts'].forEach((key) => {
        ;(data[key] || []).forEach((item) => merged.push(item))
      })
      items.value = merged
      total.value = merged.length
    } else {
      const data = await searchApi.byType(activeType.value, {
        keyword: kw,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      items.value = data.list || []
      total.value = data.total || 0
    }
  } catch (e) {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function syncRoute () {
  const q = keyword.value.trim()
  router.replace({
    name: 'search',
    query: {
      keyword: q || undefined,
      type: activeType.value !== 'all' ? activeType.value : undefined
    }
  })
}

function doSearch () {
  pageNum.value = 1
  syncRoute()
  load()
  setPageMeta({
    title: (keyword.value ? keyword.value + ' - ' : '') + '搜索 - 机器人之家',
    description: '在机器人之家搜索机器人、品牌、企业与内容。'
  })
}

function onClear () {
  items.value = []
  syncRoute()
}

function goKeyword (kw) {
  keyword.value = kw
  activeType.value = 'all'
  doSearch()
}

function onTypeChange () {
  pageNum.value = 1
  syncRoute()
  load()
}

function changePage (p) {
  pageNum.value = p
  load()
}

async function clearHistory () {
  await searchApi.clearHistory()
  historyList.value = []
  ElMessage.success('已清空搜索历史')
}

watch(
  () => [route.query.q, route.query.keyword, route.query.type],
  () => {
    keyword.value = resolveKeywordFromRoute()
    activeType.value = String(route.query.type || 'all')
    if (keyword.value) load()
  }
)

onMounted(async () => {
  setPageMeta({ title: '搜索 - 机器人之家', description: '在机器人之家搜索机器人、品牌、企业与内容。' })
  keyword.value = resolveKeywordFromRoute()
  activeType.value = String(route.query.type || 'all')
  await loadHints()
  if (keyword.value) await load()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.search-box-card {
  margin-bottom: 16px;
}

.search-hints {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-hints__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-hints__title {
  font-weight: 600;
  margin-bottom: 8px;
}

.search-hints__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 4px 12px;
  border-radius: 14px;
  background: var(--rh-surface-sub);
  color: var(--rh-text-sub);
  font-size: 13px;
  cursor: pointer;
}

.chip:hover {
  color: var(--rh-primary);
  background: var(--rh-primary-light);
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  display: flex;
  gap: 14px;
  padding: 12px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
}

.result-item:hover {
  border-color: var(--rh-primary);
}

.result-item img {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
  background: var(--rh-surface-sub);
}

.result-item__body {
  min-width: 0;
}

.result-item__type {
  font-size: 12px;
  color: var(--rh-primary);
  margin-bottom: 2px;
}

.result-item__title {
  font-size: 15px;
  font-weight: 600;
}

.result-item__summary {
  font-size: 13px;
  margin-top: 4px;
}
</style>
