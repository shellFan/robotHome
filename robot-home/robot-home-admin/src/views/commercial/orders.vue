<template>
  <div class="app-container">
    <div class="page-header">
      <h3>订单管理</h3>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width: 130px;" @change="resetLoad">
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>
        <el-input v-model="filterCompanyId" placeholder="企业ID" clearable size="small" style="width: 120px;" @change="resetLoad" />
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNo" label="订单号" width="200" show-overflow-tooltip />
      <el-table-column prop="companyId" label="企业ID" width="90" />
      <el-table-column prop="planName" label="套餐" min-width="120" show-overflow-tooltip />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ formatAmount(row.amountCents) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentChannel" label="渠道" width="110" />
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column prop="paidTime" label="支付时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" type="primary" @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="(p) => { pageNum = p; loadList() }" />

    <el-dialog v-model="detailVisible" title="订单详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="企业ID">{{ detail.companyId }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="套餐">{{ detail.planName }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ formatAmount(detail.amountCents) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ detail.paymentChannel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.paidTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="幂等键" :span="2">{{ detail.idemKey }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listOrders, getOrder } from '@/api/commercial'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterStatus = ref('')
const filterCompanyId = ref('')

const detailVisible = ref(false)
const detail = ref(null)

function orderStatusType(s) { return { PAID: 'success', PENDING_PAYMENT: 'warning', CANCELLED: 'info', REFUNDED: 'danger', CLOSED: 'info' }[s] || '' }
function orderStatusText(s) { return { PENDING_PAYMENT: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款', CLOSED: '已关闭' }[s] || s }
function formatAmount(cents) { return ((cents || 0) / 100).toFixed(2) }

function resetLoad() { pageNum.value = 1; loadList() }

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterCompanyId.value) params.companyId = filterCompanyId.value
    const data = await listOrders(params)
    if (data) { list.value = data.list || []; total.value = data.total || 0 }
  } finally { loading.value = false }
}

async function showDetail(row) {
  const data = await getOrder(row.id)
  detail.value = data
  detailVisible.value = true
}

onMounted(loadList)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.filter-bar { display: flex; gap: 10px; }
</style>