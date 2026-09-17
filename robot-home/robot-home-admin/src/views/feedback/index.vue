<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="关键词搜索"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option
            v-for="(item, key) in FEEDBACK_STATUS_MAP"
            :key="key"
            :label="item.label"
            :value="Number(key)"
          />
        </el-select>
        <el-select v-model="query.feedbackType" placeholder="类型" clearable style="width: 130px">
          <el-option
            v-for="(item, key) in FEEDBACK_TYPE_MAP"
            :key="key"
            :label="item.label"
            :value="key"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">反馈列表</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag
              :type="(FEEDBACK_TYPE_MAP[row.feedbackType] || {}).type || 'info'"
              size="small"
            >
              {{ (FEEDBACK_TYPE_MAP[row.feedbackType] || {}).label || row.feedbackType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contact" label="联系方式" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag
              :type="(FEEDBACK_STATUS_MAP[row.status] || {}).type || 'info'"
              size="small"
            >
              {{ (FEEDBACK_STATUS_MAP[row.status] || {}).label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="openHandle(row)">处理</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="600px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="(FEEDBACK_TYPE_MAP[detail.feedbackType] || {}).type || 'info'" size="small">
            {{ (FEEDBACK_TYPE_MAP[detail.feedbackType] || {}).label || detail.feedbackType }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="内容">{{ detail.content }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ detail.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="页面URL">{{ detail.pageUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(FEEDBACK_STATUS_MAP[detail.status] || {}).type || 'info'" size="small">
            {{ (FEEDBACK_STATUS_MAP[detail.status] || {}).label || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.handleNote" label="处理备注">{{ detail.handleNote }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.handleTime" label="处理时间">{{ formatTime(detail.handleTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理反馈" width="500px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="处理备注">
          <el-input
            v-model="handleNote"
            type="textarea"
            :rows="4"
            placeholder="请输入处理说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFeedbackPage,
  getFeedbackDetail,
  handleFeedback,
  deleteFeedback,
  FEEDBACK_TYPE_MAP,
  FEEDBACK_STATUS_MAP
} from '@/api/feedback'
import { cleanParams, formatTime } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', status: '', feedbackType: '' })

const detailVisible = ref(false)
const detail = ref(null)
const handleVisible = ref(false)
const handleId = ref(null)
const handleNote = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await getFeedbackPage({
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
  query.status = ''
  query.feedbackType = ''
  handleSearch()
}

async function openDetail(row) {
  const data = await getFeedbackDetail(row.id)
  detail.value = data || row
  detailVisible.value = true
}

function openHandle(row) {
  handleId.value = row.id
  handleNote.value = ''
  handleVisible.value = true
}

async function submitHandle() {
  if (!handleNote.value.trim()) {
    ElMessage.warning('请输入处理备注')
    return
  }
  saving.value = true
  try {
    await handleFeedback(handleId.value, handleNote.value.trim())
    ElMessage.success('处理成功')
    handleVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除反馈 #${row.id}？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteFeedback(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => {
  loadList()
})
</script>