<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>机器人库</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="filter-card">
        <!-- 分类 -->
        <div class="filter-row">
          <span class="filter-row__label">分类</span>
          <div class="filter-row__values">
            <span class="chip" :class="{ 'chip--active': !query.categoryId }" @click="setCategory(null)">
              不限
            </span>
            <template v-for="cat in filters.categories" :key="cat.id">
              <span
                class="chip"
                :class="{ 'chip--active': query.categoryId === cat.id }"
                @click="setCategory(cat.id)"
              >
                {{ cat.name }}
              </span>
              <span
                v-for="child in cat.children || []"
                :key="child.id"
                class="chip chip--sub"
                :class="{ 'chip--active': query.categoryId === child.id }"
                @click="setCategory(child.id)"
              >
                {{ child.name }}
              </span>
            </template>
          </div>
        </div>

        <!-- 品牌 -->
        <div v-if="filters.brands && filters.brands.length" class="filter-row">
          <span class="filter-row__label">品牌</span>
          <div class="filter-row__values">
            <span class="chip" :class="{ 'chip--active': !query.brandId }" @click="setBrand(null)">不限</span>
            <span
              v-for="brand in filters.brands"
              :key="brand.id"
              class="chip"
              :class="{ 'chip--active': query.brandId === brand.id }"
              @click="setBrand(brand.id)"
            >
              {{ brand.name }}
            </span>
          </div>
        </div>

        <!-- 价格 -->
        <div v-if="filters.priceRanges && filters.priceRanges.length" class="filter-row">
          <span class="filter-row__label">价格</span>
          <div class="filter-row__values">
            <span class="chip" :class="{ 'chip--active': !activeRange }" @click="setRange(null)">不限</span>
            <span
              v-for="(range, idx) in filters.priceRanges"
              :key="range.label"
              class="chip"
              :class="{ 'chip--active': activeRange === idx }"
              @click="setRange(idx)"
            >
              {{ range.label }}
            </span>
          </div>
        </div>

        <!-- 使用场景 -->
        <div v-if="filters.scenes && filters.scenes.length" class="filter-row">
          <span class="filter-row__label">使用场景</span>
          <div class="filter-row__values">
            <span
              v-for="scene in filters.scenes"
              :key="scene"
              class="chip"
              :class="{ 'chip--active': selected.scenes.includes(scene) }"
              @click="toggle('scenes', scene)"
            >
              {{ scene }}
            </span>
          </div>
        </div>

        <!-- 开发能力 -->
        <div v-if="filters.devs && filters.devs.length" class="filter-row">
          <span class="filter-row__label">开发能力</span>
          <div class="filter-row__values">
            <span
              v-for="dev in filters.devs"
              :key="dev"
              class="chip"
              :class="{ 'chip--active': selected.devs.includes(dev) }"
              @click="toggle('devs', dev)"
            >
              {{ dev }}
            </span>
          </div>
        </div>

        <!-- AI 能力 -->
        <div v-if="filters.ais && filters.ais.length" class="filter-row">
          <span class="filter-row__label">
            人工智能
            <el-tooltip content="是否支持二次开发、开源 SDK、是否接入大模型与语音交互" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </span>
          <div class="filter-row__values">
            <span
              v-for="ai in filters.ais"
              :key="ai"
              class="chip"
              :class="{ 'chip--active': selected.ais.includes(ai) }"
              @click="toggle('ais', ai)"
            >
              {{ ai }}
            </span>
          </div>
        </div>

        <div class="filter-card__foot">
          <el-button size="small" @click="resetFilter">重置</el-button>
          <span class="rh-text-light">共 {{ page.total }} 个结果</span>
        </div>
      </div>

      <div class="list-toolbar">
        <div class="list-toolbar__sorts">
          <span
            v-for="s in sortList"
            :key="s.value"
            class="sort-chip"
            :class="{ 'sort-chip--active': query.sort === s.value }"
            @click="setSort(s.value)"
          >
            {{ s.label }}
          </span>
        </div>
        <el-checkbox v-model="onlyCompared" class="list-toolbar__compare" @change="load">
          只看可比参数
        </el-checkbox>
      </div>

      <div v-if="loading" class="rh-empty">加载中…</div>
      <div v-else-if="!page.list.length" class="rh-empty">没有符合条件的机器人，试试调整筛选条件</div>
      <div v-else class="rh-grid rh-grid--5">
        <RobotCard v-for="robot in page.list" :key="robot.id" :robot="robot" />
      </div>

      <el-pagination
        v-if="page.total > page.pageSize"
        background
        layout="total, prev, pager, next, jumper"
        :current-page="page.pageNum"
        :page-size="page.pageSize"
        :total="page.total"
        @current-change="changePage"
      />
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { QuestionFilled } from '@element-plus/icons-vue'
import MainLayout from '@/layout/MainLayout.vue'
import RobotCard from '@/components/RobotCard.vue'
import { robotApi } from '@/api'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const onlyCompared = ref(false)
const activeRange = ref(null)

const filters = reactive({ categories: [], brands: [], scenes: [], devs: [], ais: [], priceRanges: [] })
const selected = reactive({ scenes: [], devs: [], ais: [] })
const query = reactive({
  categoryId: null,
  brandId: null,
  minPrice: null,
  maxPrice: null,
  sort: 'comprehensive'
})
const page = reactive({ list: [], total: 0, pageNum: 1, pageSize: 20, pages: 0 })

const sortList = [
  { label: '综合', value: 'comprehensive' },
  { label: '热度', value: 'hot' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' },
  { label: '最新', value: 'new' },
  { label: '评分', value: 'score' }
]

async function loadFilters () {
  const data = await robotApi.filters()
  filters.categories = data.categories || []
  filters.brands = data.brands || []
  filters.scenes = data.scenes || []
  filters.devs = data.devs || []
  filters.ais = data.ais || []
  filters.priceRanges = data.priceRanges || []
}

function buildParams () {
  return {
    categoryId: query.categoryId || undefined,
    brandId: query.brandId || undefined,
    minPrice: query.minPrice !== null ? query.minPrice : undefined,
    maxPrice: query.maxPrice !== null ? query.maxPrice : undefined,
    scenes: selected.scenes.length ? selected.scenes.join(',') : undefined,
    devs: selected.devs.length ? selected.devs.join(',') : undefined,
    ais: selected.ais.length ? selected.ais.join(',') : undefined,
    sort: query.sort,
    pageNum: page.pageNum,
    pageSize: page.pageSize
  }
}

async function load () {
  loading.value = true
  try {
    const data = await robotApi.page(buildParams())
    page.list = data.list || []
    page.total = data.total || 0
    page.pages = data.pages || 0
  } finally {
    loading.value = false
  }
}

function setCategory (id) {
  query.categoryId = id
  page.pageNum = 1
  load()
}

function setBrand (id) {
  query.brandId = id
  page.pageNum = 1
  load()
}

function setRange (idx) {
  if (idx === null || activeRange.value === idx) {
    activeRange.value = null
    query.minPrice = null
    query.maxPrice = null
  } else {
    const range = filters.priceRanges[idx]
    activeRange.value = idx
    query.minPrice = range.min
    query.maxPrice = range.max
  }
  page.pageNum = 1
  load()
}

function toggle (key, value) {
  const list = selected[key]
  const idx = list.indexOf(value)
  if (idx >= 0) {
    list.splice(idx, 1)
  } else {
    list.push(value)
  }
  page.pageNum = 1
  load()
}

function setSort (value) {
  query.sort = value
  page.pageNum = 1
  load()
}

function resetFilter () {
  query.categoryId = null
  query.brandId = null
  query.minPrice = null
  query.maxPrice = null
  query.sort = 'comprehensive'
  activeRange.value = null
  selected.scenes = []
  selected.devs = []
  selected.ais = []
  page.pageNum = 1
  load()
}

function changePage (p) {
  page.pageNum = p
  load()
}

/** 从 URL 同步筛选条件，支持 /robots?categoryId=1 直接访问 */
function syncFromRoute () {
  const q = route.query
  query.categoryId = q.categoryId ? Number(q.categoryId) : null
  query.brandId = q.brandId ? Number(q.brandId) : null
  query.sort = q.sort || 'comprehensive'
  page.pageNum = Number(q.pageNum) || 1
}

onMounted(async () => {
  setPageMeta({
    title: '机器人库 - 按分类、品牌、价格、场景筛选机器人 - 机器人之家',
    description: '机器人之家产品库，支持按人形机器人、机器狗、服务机器人、工业机器人等分类，结合价格、使用场景、开发能力与 AI 能力筛选。'
  })
  await loadFilters()
  syncFromRoute()
  await load()
})

watch(
  () => route.query,
  () => {
    syncFromRoute()
    load()
  }
)

watch(onlyCompared, () => {
  // 可比参数筛选由后台参数模板保证，这里仅作为前端提示
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.filter-card {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 4px 18px 14px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--rh-border-light);
  align-items: flex-start;
}

.filter-row:last-of-type {
  border-bottom: none;
}

.filter-row__label {
  width: 92px;
  flex-shrink: 0;
  color: var(--rh-text-sub);
  font-size: 13px;
  padding-top: 2px;
}

.filter-row__values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  flex: 1;
}

.chip {
  padding: 3px 12px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: var(--rh-text-sub);
  transition: all 0.15s ease;
}

.chip:hover {
  color: var(--rh-primary);
}

.chip--active {
  background: var(--rh-primary);
  color: #fff;
}

.chip--sub {
  color: var(--rh-text-light);
  font-size: 12px;
}

.filter-card__foot {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 10px;
  border-top: 1px solid var(--rh-border-light);
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 10px 18px;
  margin-bottom: 16px;
}

.list-toolbar__sorts {
  display: flex;
  gap: 6px;
}

.sort-chip {
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: var(--rh-text-sub);
}

.sort-chip:hover {
  color: var(--rh-primary);
}

.sort-chip--active {
  background: var(--rh-primary-light);
  color: var(--rh-primary);
  font-weight: 600;
}
</style>
