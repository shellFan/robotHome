<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>品牌库</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card">
        <div class="brand-toolbar">
          <el-input
            v-model="keyword"
            placeholder="搜索品牌"
            clearable
            style="width: 240px"
            @keyup.enter="load"
            @clear="load"
          />
          <div class="letters">
            <span class="chip" :class="{ 'chip--active': !activeLetter }" @click="setLetter(null)">全部</span>
            <span
              v-for="group in groups"
              :key="group.letter"
              class="chip"
              :class="{ 'chip--active': activeLetter === group.letter }"
              @click="setLetter(group.letter)"
            >
              {{ group.letter }}
            </span>
          </div>
        </div>

        <div v-if="loading" class="rh-empty">加载中…</div>
        <div v-else-if="!brands.length" class="rh-empty">没有找到相关品牌</div>
        <div v-else class="brand-cards">
          <router-link
            v-for="brand in brands"
            :key="brand.id"
            :to="'/brand/' + brand.id"
            class="brand-item"
          >
            <img :src="imageOf(brand.logo)" :alt="brand.name" />
            <div class="brand-item__body">
              <div class="brand-item__name rh-ellipsis">{{ brand.name }}</div>
              <div class="brand-item__meta rh-text-light">
                {{ brand.country }}<template v-if="brand.foundYear"> · {{ brand.foundYear }} 年</template>
                · {{ brand.robotCount || 0 }} 款产品
              </div>
              <div class="brand-item__intro rh-clamp-2">{{ brand.intro }}</div>
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
import { brandApi } from '@/api'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const groups = ref([])
const brands = ref([])
const keyword = ref('')
const activeLetter = ref(null)
const pageNum = ref(1)
const pageSize = ref(24)
const total = ref(0)
const loading = ref(false)

async function loadGroups () {
  groups.value = await brandApi.letters()
}

async function load () {
  loading.value = true
  try {
    const data = await brandApi.page({
      keyword: keyword.value || undefined,
      initial: activeLetter.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    brands.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function setLetter (letter) {
  activeLetter.value = letter
  pageNum.value = 1
  load()
}

function changePage (p) {
  pageNum.value = p
  load()
}

onMounted(async () => {
  setPageMeta({
    title: '机器人品牌库 - 品牌大全与产品列表 - 机器人之家',
    description: '机器人之家品牌库，按首字母检索宇树、优必选、波士顿动力、大疆、小米等机器人品牌及其产品。'
  })
  await loadGroups()
  await load()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.brand-toolbar {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.letters {
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

.brand-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.brand-item {
  display: flex;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
  background: #fff;
}

.brand-item:hover {
  border-color: var(--rh-primary);
}

.brand-item img {
  width: 64px;
  height: 64px;
  object-fit: contain;
  border-radius: 8px;
  flex-shrink: 0;
  background: var(--rh-surface-sub);
}

.brand-item__body {
  min-width: 0;
}

.brand-item__name {
  font-size: 15px;
  font-weight: 600;
}

.brand-item__meta {
  font-size: 12px;
  margin: 2px 0 6px;
}

.brand-item__intro {
  font-size: 12px;
  color: var(--rh-text-sub);
  height: 36px;
}
</style>
