<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="标题 / 内容关键词"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.circleId" placeholder="圈子" clearable filterable style="width: 160px">
          <el-option v-for="c in circles" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="待审" :value="0" />
          <el-option label="正常" :value="1" />
          <el-option label="下架" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">帖子审核</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="圈子" width="120">
          <template #default="{ row }">{{ circleName(row.circleId) }}</template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" link type="success" @click="handleAudit(row, 1)">通过</el-button>
            <el-button v-if="row.status !== 2" link type="warning" @click="handleAudit(row, 2)">下架</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
          :page-sizes="[10, 20, 50]"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCircles, getPostPage, auditPost, deletePost } from '@/api/community'
import { cleanParams, formatTime } from '@/utils'

const loading = ref(false)
const list = ref([])
const circles = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', circleId: null, status: '' })

function circleName(id) {
  const hit = circles.value.find((c) => String(c.id) === String(id))
  return hit ? hit.name : '-'
}

function statusLabel(s) {
  if (s === 0) return '待审'
  if (s === 1) return '正常'
  if (s === 2) return '下架'
  return '-'
}

function statusType(s) {
  if (s === 0) return 'warning'
  if (s === 1) return 'success'
  if (s === 2) return 'info'
  return 'info'
}

async function loadList() {
  loading.value = true
  try {
    const data = await getPostPage({
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      ...cleanParams(query)
    })
    list.value = (data && data.list) || []
    page.total = (data && data.total) || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.circleId = null
  query.status = ''
  handleSearch()
}

async function handleAudit(row, status) {
  const action = status === 1 ? '通过' : '下架'
  try {
    await ElMessageBox.confirm(`确认${action}该帖子？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await auditPost(row.id, status)
  ElMessage.success(`${action}成功`)
  loadList()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除帖子「${row.title || row.id}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deletePost(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  const data = await getCircles()
  circles.value = Array.isArray(data) ? data : []
  await loadList()
})
</script>
