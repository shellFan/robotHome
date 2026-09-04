<template>
  <div class="rh-card">
    <h1 class="page-title">我的询价</h1>

    <div v-if="loading" class="rh-empty">加载中…</div>
    <div v-else-if="!list.length" class="rh-empty">暂无询价记录</div>
    <div v-else class="inquiry-list">
      <div v-for="item in list" :key="item.id" class="inquiry-row" @click="openDetail(item)">
        <div class="inquiry-row__main">
          <div class="inquiry-row__title">
            <router-link v-if="item.robotId" :to="'/robot/' + item.robotId" @click.stop>
              {{ item.robotName || ('机器人 #' + item.robotId) }}
            </router-link>
            <span v-else>{{ item.robotName || '询价' }}</span>
          </div>
          <div class="inquiry-row__meta rh-text-light">
            {{ item.name }} · {{ item.phone }} · 数量 {{ item.quantity }}
            <template v-if="item.budget"> · 预算 {{ item.budget }}</template>
          </div>
          <div class="inquiry-row__time rh-text-light">
            {{ formatDate(item.createTime, 'YYYY-MM-DD HH:mm') }}
          </div>
        </div>
        <el-tag :type="statusType(item.status)" size="small">{{ item.statusName || statusText(item.status) }}</el-tag>
      </div>
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

    <el-dialog v-model="dialog.visible" title="询价详情" width="520px">
      <template v-if="dialog.data">
        <p><b>产品：</b>{{ dialog.data.robotName }}</p>
        <p><b>联系人：</b>{{ dialog.data.name }} / {{ dialog.data.phone }}</p>
        <p><b>地区：</b>{{ dialog.data.region || '-' }}</p>
        <p><b>类型：</b>{{ dialog.data.customerTypeName || '-' }} {{ dialog.data.companyName || '' }}</p>
        <p><b>数量：</b>{{ dialog.data.quantity }} · <b>预算：</b>{{ dialog.data.budget || '-' }}</p>
        <p><b>备注：</b>{{ dialog.data.remark || '-' }}</p>
        <p><b>状态：</b>{{ dialog.data.statusName || statusText(dialog.data.status) }}</p>
        <p v-if="dialog.data.handleNote"><b>处理备注：</b>{{ dialog.data.handleNote }}</p>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { inquiryApi } from '@/api'
import { formatDate } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const dialog = reactive({ visible: false, data: null })

function statusText (s) {
  const map = { 0: '待处理', 1: '处理中', 2: '已完成', 3: '已关闭' }
  return map[s] || String(s ?? '-')
}

function statusType (s) {
  if (s === 2) return 'success'
  if (s === 3) return 'info'
  if (s === 1) return 'warning'
  return ''
}

async function load () {
  loading.value = true
  try {
    const data = await inquiryApi.my({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function changePage (p) {
  pageNum.value = p
  load()
}

async function openDetail (item) {
  try {
    dialog.data = await inquiryApi.detail(item.id)
    dialog.visible = true
  } catch (e) {
    dialog.data = item
    dialog.visible = true
  }
}

onMounted(() => {
  setPageMeta({ title: '我的询价 - 机器人之家' })
  load()
})
</script>

<style scoped lang="scss">
.page-title {
  margin: 0 0 16px;
  font-size: 18px;
}

.inquiry-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.inquiry-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
  cursor: pointer;
}

.inquiry-row:hover {
  border-color: var(--rh-primary);
}

.inquiry-row__main {
  flex: 1;
  min-width: 0;
}

.inquiry-row__title {
  font-weight: 600;
}

.inquiry-row__title a:hover {
  color: var(--rh-primary);
}

.inquiry-row__meta,
.inquiry-row__time {
  font-size: 12px;
  margin-top: 4px;
}
</style>
