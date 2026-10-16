<template>
  <div class="app-container">
    <div class="page-header">
      <h3>广告审核</h3>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width: 140px;" @change="resetLoad">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已上线" value="ONLINE" />
          <el-option label="已下线" value="OFFLINE" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
        <el-input v-model="filterCompanyId" placeholder="企业ID" clearable size="small" style="width: 120px;" @change="resetLoad" />
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="campaignName" label="投放名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="companyId" label="企业ID" width="90" />
      <el-table-column prop="slotId" label="广告位ID" width="90" />
      <el-table-column label="投放时间" width="200">
        <template #default="{ row }">{{ formatDate(row.startTime) }} ~ {{ formatDate(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="campaignStatusType(row.status)" size="small">{{ campaignStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING_REVIEW'">
            <el-button text size="small" type="success" @click="approve(row)">通过</el-button>
            <el-button text size="small" type="danger" @click="reject(row)">拒绝</el-button>
          </template>
          <span v-else class="done-text">--</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="(p) => { pageNum = p; loadList() }" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCampaigns, approveCampaign, rejectCampaign } from '@/api/advertising'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterStatus = ref('')
const filterCompanyId = ref('')

function campaignStatusType(s) {
  return { PENDING_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger', ONLINE: 'success', OFFLINE: 'info', DRAFT: 'info' }[s] || ''
}
function campaignStatusText(s) {
  return { DRAFT: '草稿', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', ONLINE: '已上线', OFFLINE: '已下线' }[s] || s
}
function formatDate(val) {
  if (!val) return '-'
  return String(val).replace('T', ' ').substring(0, 10)
}

function resetLoad() { pageNum.value = 1; loadList() }

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterCompanyId.value) params.companyId = filterCompanyId.value
    const data = await listCampaigns(params)
    if (data) { list.value = data.list || []; total.value = data.total || 0 }
  } finally { loading.value = false }
}

async function approve(row) {
  try {
    const { value } = await ElMessageBox.prompt('审核备注（可留空）', '通过审核', {
      inputPlaceholder: '选填',
      confirmButtonText: '通过',
      inputValue: ''
    })
    await approveCampaign(row.id, value || '')
    ElMessage.success('已通过')
    loadList()
  } catch (e) { /* cancel */ }
}

async function reject(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空',
      confirmButtonText: '拒绝'
    })
    await rejectCampaign(row.id, value)
    ElMessage.success('已拒绝')
    loadList()
  } catch (e) { /* cancel */ }
}

onMounted(loadList)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.filter-bar { display: flex; gap: 10px; }
.done-text { color: #999; }
</style>