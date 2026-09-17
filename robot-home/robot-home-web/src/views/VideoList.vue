<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>视频</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card">
        <div class="video-toolbar">
          <div class="cats">
            <span class="chip" :class="{ 'chip--active': !categoryId }" @click="setCategory(null)">全部</span>
            <span
              v-for="cat in categories"
              :key="cat.id"
              class="chip"
              :class="{ 'chip--active': categoryId === cat.id }"
              @click="setCategory(cat.id)"
            >
              {{ cat.name }}
            </span>
          </div>
          <el-input
            v-model="keyword"
            placeholder="搜索视频"
            clearable
            style="width: 220px"
            @keyup.enter="reload"
            @clear="reload"
          />
        </div>

        <div v-if="loading" class="rh-empty">加载中…</div>
        <div v-else-if="!list.length" class="rh-empty">暂无视频</div>
        <div v-else class="video-grid">
          <router-link
            v-for="item in list"
            :key="item.id"
            :to="'/video/' + item.id"
            class="video-item"
          >
            <div class="video-item__cover">
              <img :src="imageOf(item.cover)" :alt="item.title" loading="lazy" />
              <span v-if="item.duration" class="video-item__duration">{{ formatDuration(item.duration) }}</span>
            </div>
            <div class="video-item__title rh-clamp-2">{{ item.title }}</div>
            <div class="video-item__meta rh-text-light">
              {{ formatCount(item.viewCount) }} 次播放 · {{ formatDate(item.publishTime) }}
            </div>
          </router-link>
        </div>

        <el-pagination
          v-if="total > pageSize"
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
import { onMounted, ref } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import { videoApi } from '@/api'
import { formatCount, formatDate, formatDuration, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const categories = ref([])
const list = ref([])
const categoryId = ref(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

async function loadCategories () {
  categories.value = (await videoApi.categories()) || []
}

async function load () {
  loading.value = true
  try {
    const data = await videoApi.page({
      categoryId: categoryId.value || undefined,
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function setCategory (id) {
  categoryId.value = id
  reload()
}

function reload () {
  pageNum.value = 1
  load()
}

function changePage (p) {
  pageNum.value = p
  load()
}

onMounted(async () => {
  setPageMeta({
    title: '机器人视频 - 机器人之家',
    description: '机器人之家视频频道，产品演示、评测与技术讲解。'
  })
  await loadCategories()
  await load()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.video-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.cats {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  flex: 1;
}

.chip {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: var(--rh-text-sub);
}

.chip:hover {
  color: var(--rh-primary);
}

.chip--active {
  background: var(--rh-primary);
  color: #fff;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.video-item__cover {
  position: relative;
  border-radius: var(--rh-radius);
  overflow: hidden;
  background: var(--rh-surface-sub);
}

.video-item__cover img {
  width: 100%;
  height: 150px;
  object-fit: cover;
}

.video-item__duration {
  position: absolute;
  right: 8px;
  bottom: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 3px;
}

.video-item__title {
  font-size: 14px;
  font-weight: 600;
  margin: 8px 0 4px;
  min-height: 40px;
}

.video-item__meta {
  font-size: 12px;
}

.video-item:hover .video-item__title {
  color: var(--rh-primary);
}
</style>
