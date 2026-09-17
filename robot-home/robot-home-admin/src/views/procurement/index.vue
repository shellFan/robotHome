<template>
  <div class="app-container">
    <h3 style="margin-bottom: 16px;">采购需求管理</h3>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="联系人" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="companyName" label="公司" min-width="150" show-overflow-tooltip />
      <el-table-column prop="requirementType" label="需求类型" width="100" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="usageScene" label="应用场景" width="100" />
      <el-table-column prop="budgetRange" label="预算" width="100" />
      <el-table-column prop="leadScore" label="线索分" width="80">
        <template #default="{ row }">
          <el-input-number v-model="row.leadScore" :min="0" :max="100" size="small" controls-position="right" @change="handleLeadScore(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="assignedTo" label="负责人" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" type="primary" @click="handleAssign(row)">分配</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="(p) => { pageNum = p; loadList() }" />

    <el-dialog v-model="showAssign" title="分配负责人" width="400px">
      <el-form label-width="80px">
        <el-form-item label="负责人"><el-input v-model="assignName" placeholder="输入负责人姓名" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssign = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProcurementPage, assignProcurement, updateLeadScore } from '@/api/procurement'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showAssign = ref(false)
const assignRow = ref(null)
const assignName = ref('')

function statusType(s) {
  if (s === 'CLOSED') return 'info'
  if (s === 'ASSIGNED') return 'success'
  if (s === 'PROCESSING') return 'warning'
  return ''
}
function statusText(s) {
  const map = { OPEN: '待处理', ASSIGNED: '已分配', PROCESSING: '处理中', CLOSED: '已关闭' }
  return map[s] || s
}

async function loadList() {
  loading.value = true
  try {
    const res = await getProcurementPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.data) { list.value = res.data.list || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}

function handleAssign(row) {
  assignRow.value = row
  assignName.value = row.assignedTo || ''
  showAssign.value = true
}

async function confirmAssign() {
  if (!assignRow.value) return
  await assignProcurement(assignRow.value.id, assignName.value)
  ElMessage.success('分配成功')
  showAssign.value = false
  loadList()
}

async function handleLeadScore(row) {
  await updateLeadScore(row.id, row.leadScore)
}

onMounted(loadList)
</script>