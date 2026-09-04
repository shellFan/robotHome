<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>企业库</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card">
        <div class="company-toolbar">
          <el-input
            v-model="keyword"
            placeholder="搜索企业名称或简介"
            clearable
            style="width: 260px"
            @keyup.enter="load"
            @clear="load"
          />
          <div class="regions">
            <span class="chip" :class="{ 'chip--active': !activeRegion }" @click="setRegion(null)">全部地区</span>
            <span
              v-for="region in regions"
              :key="region"
              class="chip"
              :class="{ 'chip--active': activeRegion === region }"
              @click="setRegion(region)"
            >
              {{ region }}
            </span>
          </div>
        </div>

        <div v-if="loading" class="rh-empty">加载中…</div>
        <div v-else-if="!list.length" class="rh-empty">没有找到相关企业</div>
        <div v-else class="company-cards">
          <router-link
            v-for="company in list"
            :key="company.id"
            :to="'/company/' + company.id"
            class="company-item"
          >
            <img :src="imageOf(company.logo)" :alt="company.name" />
            <div class="company-item__body">
              <div class="company-item__name rh-ellipsis">{{ company.name }}</div>
              <div class="company-item__meta rh-text-light">
                {{ company.region }}<template v-if="company.foundYear"> · {{ company.foundYear }} 年</template>
                · {{ company.brandCount || 0 }} 个品牌 · {{ company.productCount || 0 }} 款产品
              </div>
              <div class="company-item__intro rh-clamp-2">{{ company.intro }}</div>
              <div v-if="company.tags && company.tags.length" class="company-item__tags">
                <span v-for="tag in company.tags" :key="tag" class="rh-tag">{{ tag }}</span>
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
import { companyApi } from '@/api'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const list = ref([])
const regions = ref([])
const keyword = ref('')
const activeRegion = ref(null)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

async function loadRegions () {
  regions.value = await companyApi.regions()
}

async function load () {
  loading.value = true
  try {
    const data = await companyApi.page({
      keyword: keyword.value || undefined,
      region: activeRegion.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function setRegion (region) {
  activeRegion.value = region
  pageNum.value = 1
  load()
}

function changePage (p) {
  pageNum.value = p
  load()
}

onMounted(async () => {
  setPageMeta({
    title: '机器人企业库 - 机器人公司大全 - 机器人之家',
    description: '机器人之家企业库，收录宇树科技、优必选、新松、埃斯顿、科沃斯等机器人企业及其品牌与产品。'
  })
  await loadRegions()
  await load()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.company-toolbar {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.regions {
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

.company-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.company-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
  background: #fff;
}

.company-item:hover {
  border-color: var(--rh-primary);
}

.company-item img {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border-radius: 8px;
  background: var(--rh-surface-sub);
  flex-shrink: 0;
}

.company-item__body {
  min-width: 0;
}

.company-item__name {
  font-size: 16px;
  font-weight: 600;
}

.company-item__meta {
  font-size: 12px;
  margin: 2px 0 6px;
}

.company-item__intro {
  font-size: 12px;
  color: var(--rh-text-sub);
  height: 36px;
}

.company-item__tags {
  margin-top: 6px;
}
</style>
