<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="用户名/内容" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="(item, key) in REVIEW_STATUS_MAP" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
        <el-input v-model="query.robotName" placeholder="机器人名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">口碑管理</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="robotName" label="机器人" min-width="140" show-overflow-tooltip />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column label="综合评分" width="90" align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.overallScore" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="质量" width="70" align="center">
          <template #default="{ row }">{{ row.qualityScore }}</template>
        </el-table-column>
        <el-table-column label="服务" width="70" align="center">
          <template #default="{ row }">{{ row.serviceScore }}</template>
        </el-table-column>
        <el-table-column label="性价比" width="70" align="center">
          <template #default="{ row }">{{ row.costScore }}</template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="helpfulCount" label="有用" width="60" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(REVIEW_STATUS_MAP[row.status] || {}).type || 'info'" size="small">
              {{ (REVIEW_STATUS_MAP[row.status] || {}).label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" @click="handleAudit(row, 1)">通过</el-button>
              <el-button link type="danger" @click="handleAudit(row, 2)">拒绝</el-button>
            </template>
            <el-button link type="primary" @click="openReply(row)">回复</el-button>
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
          :total="page.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="官方回复" width="500px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="评价内容">
          <div class="reply-preview">{{ currentRow?.content }}</div>
        </el-form-item>
        <el-form-item label="回复内容" required>
          <el-input v-model="replyText" type="textarea" :rows="4" placeholder="请输入官方回复内容" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replyLoading" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewPage, auditReview, replyReview, deleteReview, REVIEW_STATUS_MAP } from '@/api/review'

const loading = ref(false)
const list = ref([])
const query = reactive({ keyword: '', status: null, robotName: '' })
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })

// Reply dialog
const replyVisible = ref(false)
const replyLoading = ref(false)
const replyText = ref('')
const currentRow = ref(null)

async function loadData () {
  loading.value = true
  try {
    const params = {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      ...query
    }
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await getReviewPage(params)
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
  query.robotName = ''
  handleSearch()
}

async function handleAudit (row, status) {
  const action = status === 1 ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${action}该评价？`, '审核确认', { type: 'warning' })
    await auditReview(row.id, status)
    ElMessage.success(`评价已${action}`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

function openReply (row) {
  currentRow.value = row
  replyText.value = row.reply || ''
  replyVisible.value = true
}

async function submitReply () {
  if (!replyText.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replyLoading.value = true
  try {
    await replyReview(currentRow.value.id, replyText.value.trim())
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '回复失败')
  } finally {
    replyLoading.value = false
  }
}

async function handleDelete (row) {
  try {
    await ElMessageBox.confirm('确定删除该评价？删除后不可恢复', '删除确认', { type: 'danger' })
    await deleteReview(row.id)
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
.reply-preview {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  max-height: 120px;
  overflow-y: auto;
}
</style>