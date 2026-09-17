<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="质量评分" name="scores">
        <div style="margin-bottom: 16px; display: flex; justify-content: space-between;">
          <h3>机器人质量评分</h3>
          <el-button type="primary" :loading="computing" @click="handleCompute">重新计算评分</el-button>
        </div>
        <el-table :data="scores" v-loading="loading" border stripe>
          <el-table-column prop="robotId" label="机器人ID" width="100" />
          <el-table-column prop="robotName" label="机器人名称" min-width="150" />
          <el-table-column prop="totalScore" label="总分" width="80" />
          <el-table-column prop="basicInfoScore" label="基本信息" width="80" />
          <el-table-column prop="paramScore" label="参数" width="70" />
          <el-table-column prop="imageScore" label="图片" width="70" />
          <el-table-column prop="videoScore" label="视频" width="70" />
          <el-table-column prop="priceScore" label="价格" width="70" />
          <el-table-column prop="brandScore" label="品牌" width="70" />
          <el-table-column prop="categoryScore" label="分类" width="70" />
          <el-table-column prop="createTime" label="计算时间" width="160" />
        </el-table>
        <el-pagination background layout="total, prev, pager, next" :current-page="sPage" :page-size="pageSize" :total="sTotal" @current-change="(p) => { sPage = p; loadScores() }" />
      </el-tab-pane>

      <el-tab-pane label="质量问题" name="issues">
        <h3 style="margin-bottom: 16px;">质量问题列表</h3>
        <el-table :data="issues" v-loading="iLoading" border stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="robotId" label="机器人ID" width="100" />
          <el-table-column prop="robotName" label="机器人名称" min-width="150" />
          <el-table-column prop="issueType" label="问题类型" width="180" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="severity" label="严重度" width="80">
            <template #default="{ row }">
              <el-tag :type="row.severity === 1 ? 'danger' : row.severity === 2 ? 'warning' : 'info'">{{ row.severity === 1 ? '高' : row.severity === 2 ? '中' : '低' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{ row.status === 1 ? '已处理' : row.status === 2 ? '忽略' : '未处理' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 1" text size="small" type="primary" @click="handleResolve(row)">标记解决</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="total, prev, pager, next" :current-page="iPage" :page-size="pageSize" :total="iTotal" @current-change="(p) => { iPage = p; loadIssues() }" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { computeQuality, getScorePage, getIssuePage, updateIssueStatus } from '@/api/quality'

const activeTab = ref('scores')
const loading = ref(false)
const scores = ref([])
const sPage = ref(1)
const sTotal = ref(0)
const computing = ref(false)
const iLoading = ref(false)
const issues = ref([])
const iPage = ref(1)
const iTotal = ref(0)
const pageSize = ref(20)

async function loadScores() {
  loading.value = true
  try {
    const res = await getScorePage({ pageNum: sPage.value, pageSize: pageSize.value })
    if (res.data) { scores.value = res.data.list || []; sTotal.value = res.data.total || 0 }
  } finally { loading.value = false }
}

async function loadIssues() {
  iLoading.value = true
  try {
    const res = await getIssuePage({ pageNum: iPage.value, pageSize: pageSize.value })
    if (res.data) { issues.value = res.data.list || []; iTotal.value = res.data.total || 0 }
  } finally { iLoading.value = false }
}

async function handleCompute() {
  computing.value = true
  try {
    await computeQuality()
    ElMessage.success('评分计算完成')
    loadScores()
    loadIssues()
  } finally { computing.value = false }
}

async function handleResolve(row) {
  await updateIssueStatus(row.id, 1)
  ElMessage.success('已标记为解决')
  loadIssues()
}

onMounted(loadScores)
</script>