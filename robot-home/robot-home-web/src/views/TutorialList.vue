<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>教程</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card">
        <div class="tutorial-toolbar">
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
            placeholder="搜索教程"
            clearable
            style="width: 220px"
            @keyup.enter="reload"
            @clear="reload"
          />
        </div>

        <div v-if="loading" class="rh-empty">加载中…</div>
        <div v-else-if="!list.length" class="rh-empty">暂无教程</div>
        <div v-else class="tutorial-list">
          <router-link
            v-for="item in list"
            :key="item.id"
            :to="'/tutorial/' + item.id"
            class="tutorial-item"
          >
            <img :src="imageOf(item.cover)" :alt="item.title" />
            <div class="tutorial-item__body">
              <div class="tutorial-item__title rh-clamp-2">{{ item.title }}</div>
              <div class="tutorial-item__summary rh-clamp-2 rh-text-sub">{{ item.summary }}</div>
              <div class="tutorial-item__meta rh-text-light">
                <span v-if="item.categoryName">{{ item.categoryName }} · </span>
                {{ formatDate(item.publishTime) }} · {{ formatCount(item.viewCount) }} 阅读
              </div>
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
import { tutorialApi } from '@/api'
import { formatCount, formatDate, imageOf } from '@/utils/format'
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
  categories.value = (await tutorialApi.categories()) || []
}

async function load () {
  loading.value = true
  try {
    const data = await tutorialApi.page({
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
    title: '机器人教程 - 机器人之家',
    description: '机器人之家教程中心，开发、选型与应用指南。'
  })
  await loadCategories()
  await load()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.tutorial-toolbar {
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

.tutorial-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tutorial-item {
  display: flex;
  gap: 16px;
  padding: 12px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
}

.tutorial-item:hover {
  border-color: var(--rh-primary);
}

.tutorial-item img {
  width: 180px;
  height: 108px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
  background: var(--rh-surface-sub);
}

.tutorial-item__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.tutorial-item__title {
  font-size: 16px;
  font-weight: 600;
}

.tutorial-item__summary {
  font-size: 13px;
  margin: 8px 0 auto;
}

.tutorial-item__meta {
  font-size: 12px;
}
</style>
