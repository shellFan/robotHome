<template>
  <div class="app-container">
    <div class="page-header">
      <h3>退款管理</h3>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width: 130px;" @change="resetLoad">
          <el-option label="申请中" value="APPLYING" />
          <el-option label="已成功" value="SUCCESS" />
          <el-option label="已失败" value="FAIL" />
        </el-select>
        <el-button type="primary" size="small" @click="showApply = true">发起退款</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderId" label="订单ID" width="90" />
      <el-table-column prop="orderNo" label="订单号" width="200" show-overflow-tooltip />
      <el-table-column prop="companyId" label="企业ID" width="90" />
      <el-table-column label="退款金额" width="110">
        <template #default="{ row }">¥{{ formatAmount(row.amountCents) }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="refundStatusType(row.status)" size="small">{{ refundStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="申请时间" width="170" />
      <el-table-column prop="operatorId" label="操作人" width="90" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'APPLYING'">
            <el-button text size="small" type="success" @click="confirm(row)">确认退款</el-button>
            <el-button text size="small" type="danger" @click="fail(row)">标记失败</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="(p) => { pageNum = p; loadList() }" />

    <!-- 发起退款 -->
    <el-dialog v-model="showApply" title="发起退款" width="440px">
      <el-form :model="applyForm" label-width="90px">
        <el-form-item label="订单ID"><el-input-number v-model="applyForm.orderId" :min="1" controls-position="right" style="width: 100%;" /></el-form-item>
        <el-form-item label="退款原因"><el-input v-model="applyForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApply = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="apply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRefunds, applyRefund, confirmRefund, failRefund } from '@/api/commercial'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterStatus = ref('')

const showApply = ref(false)
const applying = ref(false)
const applyForm = ref({ orderId: 0, reason: '' })

function refundStatusType(s) { return { SUCCESS: 'success', APPLYING: 'warning', FAIL: 'danger' }[s] || '' }
function refundStatusText(s) { return { APPLYING: '申请中', SUCCESS: '已成功', FAIL: '已失败' }[s] || s }
function formatAmount(cents) { return ((cents || 0) / 100).toFixed(2) }

function resetLoad() { pageNum.value = 1; loadList() }

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    const data = await listRefunds(params)
    if (data) { list.value = data.list || []; total.value = data.total || 0 }
  } finally { loading.value = false }
}

async function apply() {
  if (!applyForm.value.orderId) { ElMessage.warning('请输入订单ID'); return }
  if (!applyForm.value.reason) { ElMessage.warning('请输入退款原因'); return }
  applying.value = true
  try {
    await applyRefund(applyForm.value.orderId, applyForm.value.reason)
    ElMessage.success('退款申请已提交')
    showApply.value = false
    applyForm.value = { orderId: 0, reason: '' }
    loadList()
  } finally { applying.value = false }
}

async function confirm(row) {
  try {
    await ElMessageBox.confirm(`确认退款 ¥${formatAmount(row.amountCents)}？确认后权益将被撤销、订阅降级。`, '确认退款')
    await confirmRefund(row.id)
    ElMessage.success('退款已确认')
    loadList()
  } catch (e) { /* cancel */ }
}

async function fail(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入失败原因', '标记退款失败', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
    await failRefund(row.id, value)
    ElMessage.success('已标记为失败')
    loadList()
  } catch (e) { /* cancel */ }
}

onMounted(loadList)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.filter-bar { display: flex; gap: 10px; }
</style>