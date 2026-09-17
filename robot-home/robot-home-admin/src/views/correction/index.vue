<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="机器人名/纠错值" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="(item, key) in CORRECTION_STATUS_MAP" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">参数纠错管理</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="robotName" label="机器人" min-width="140" show-overflow-tooltip />
        <el-table-column prop="defName" label="参数项" width="120" show-overflow-tooltip />
        <el-table-column prop="oldValue" label="当前值" width="120" show-overflow-tooltip />
        <el-table-column prop="newValue" label="纠错值" width="120" show-overflow-tooltip />
        <el-table-column prop="reason" label="纠错理由" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userName" label="提交人" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(CORRECTION_STATUS_MAP[row.status] || {}).type || 'info'" size="small">
              {{ (CORRECTION_STATUS_MAP[row.status] || {}).label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" @click="handleAudit(row, 1)">采纳</el-button>
              <el-button link type="danger" @click="handleAudit(row, 2)">拒绝</el-button>
            </template>
            <el-button v-else link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page.pageNum"
          v-model:page-size="page.pageSize"
          :total="page.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCorrectionPage, auditCorrection, deleteCorrection, CORRECTION_STATUS_MAP } from '@/api/correction'

const loading = ref(false)
const list = ref([])
const query = reactive({ keyword: '', status: null })
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })

async function loadData () {
  loading.value = true
  try {
    const params = {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      ...query
    }
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await getCorrectionPage(params)
    list.value = data.records || data.list || []
    page.total = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch () {
  page.pageNum = 1
  loadData()
}

function handleReset () {
  query.keyword = ''
  query.status = null
  handleSearch()
}

async function handleAudit (row, status) {
  const action = status === 1 ? '采纳' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${action}该纠错？${status === 1 ? '采纳后将自动更新参数值' : ''}`, '审核确认', { type: 'warning' })
    await auditCorrection(row.id, status)
    ElMessage.success(`纠错已${action}`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete (row) {
  try {
    await ElMessageBox.confirm('确定删除该纠错记录？', '删除确认', { type: 'danger' })
    await deleteCorrection(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function formatTime (t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
</style>