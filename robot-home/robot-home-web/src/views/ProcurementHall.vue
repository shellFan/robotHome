<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>采购大厅</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="hall-layout">
        <div class="hall-main">
          <div class="rh-card">
            <h2 class="hall-title">采购大厅</h2>
            <p class="hall-desc">发布采购需求，获取供应商报价</p>

            <div class="hall-toolbar">
              <div class="sorts">
                <span class="chip" :class="{ 'chip--active': sort === 'latest' }" @click="setSort('latest')">最新</span>
                <span class="chip" :class="{ 'chip--active': sort === 'hot' }" @click="setSort('hot')">热门</span>
              </div>
            </div>

            <div v-if="loading" class="rh-empty">加载中…</div>
            <div v-else-if="!list.length" class="rh-empty">暂无采购需求</div>
            <div v-else class="hall-list">
              <router-link
                v-for="item in list"
                :key="item.id"
                :to="'/procurement/' + item.id"
                class="hall-item"
              >
                <div class="hall-item__header">
                  <span class="hall-item__type">{{ item.requirementType || '采购需求' }}</span>
                  <span class="hall-item__time rh-text-light">{{ fromNow(item.createTime) }}</span>
                </div>
                <div class="hall-item__title">{{ item.title }}</div>
                <div class="hall-item__desc rh-clamp-2">{{ item.description }}</div>
                <div class="hall-item__meta rh-text-light">
                  <span v-if="item.category">{{ item.category }}</span>
                  <span v-if="item.usageScene">{{ item.usageScene }}</span>
                  <span v-if="item.budgetRange">{{ item.budgetRange }}</span>
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
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import { procurementApi } from '@/api'
import { fromNow } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const sort = ref('latest')

function setSort(s) {
  sort.value = s
  pageNum.value = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value, sort: sort.value }
    const res = await procurementApi.hallList(params)
    if (res.data) {
      list.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  pageNum.value = p
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.hall-layout { display: flex; gap: 20px; }
.hall-main { flex: 1; min-width: 0; }
.hall-title { font-size: 22px; font-weight: 600; margin-bottom: 8px; }
.hall-desc { color: #666; margin-bottom: 16px; }
.hall-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.hall-toolbar .sorts { display: flex; gap: 8px; }
.hall-list { display: flex; flex-direction: column; gap: 12px; }
.hall-item { padding: 16px; border-radius: 8px; background: #fafafa; text-decoration: none; color: inherit; transition: background .2s; }
.hall-item:hover { background: #f0f0f0; }
.hall-item__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.hall-item__type { font-size: 12px; padding: 2px 8px; background: #409eff; color: #fff; border-radius: 4px; }
.hall-item__title { font-size: 16px; font-weight: 500; margin-bottom: 6px; }
.hall-item__desc { font-size: 14px; color: #666; line-height: 1.5; margin-bottom: 8px; }
.hall-item__meta { display: flex; gap: 12px; font-size: 12px; }
</style>