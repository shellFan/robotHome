<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-select v-model="query.sourceId" placeholder="数据源" clearable style="width: 180px">
          <el-option v-for="s in sources" :key="s.id" :label="s.sourceName" :value="s.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="排队中" value="QUEUED" />
          <el-option label="运行中" value="RUNNING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已停止" value="STOPPED" />
        </el-select>
        <el-select v-model="query.taskType" placeholder="任务类型" clearable style="width: 130px">
          <el-option label="全站采集" value="FULL" />
          <el-option label="增量采集" value="INCREMENTAL" />
          <el-option label="单页采集" value="SINGLE" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">采集任务</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">创建任务</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="sourceName" label="数据源" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sourceName || row.sourceId }}</template>
        </el-table-column>
        <el-table-column prop="taskType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发现/成功" width="110">
          <template #default="{ row }">{{ row.urlsDiscovered || 0 }} / {{ row.urlsSuccess || 0 }}</template>
        </el-table-column>
        <el-table-column label="新文章/产品" width="120">
          <template #default="{ row }">{{ row.articlesNew || 0 }} / {{ row.productsNew || 0 }}</template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'QUEUED'" link type="success" @click="handleStart(row)">启动</el-button>
            <el-button v-if="row.status === 'RUNNING'" link type="warning" @click="handleStop(row)">停止</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无任务" />
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

    <el-dialog v-model="dialogVisible" title="创建采集任务" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="数据源" prop="sourceId">
          <el-select v-model="form.sourceId" placeholder="选择数据源" style="width: 100%">
            <el-option v-for="s in sources" :key="s.id" :label="s.sourceName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="form.taskType" style="width: 100%">
            <el-option label="全站采集" value="FULL" />
            <el-option label="增量采集" value="INCREMENTAL" />
            <el-option label="单页采集" value="SINGLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="种子URL">
          <el-input v-model="form.seedUrls" type="textarea" :rows="3" placeholder="每行一个URL（可选，覆盖数据源默认种子）" />
        </el-form-item>
        <el-form-item label="最大深度">
          <el-input-number v-model="form.maxDepth" :min="1" :max="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大页数">
          <el-input-number v-model="form.maxPages" :min="1" :max="100000" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">创 建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCrawlerTaskPage,
  createCrawlerTask,
  startCrawlerTask,
  stopCrawlerTask,
  deleteCrawlerTask,
  getActiveCrawlerSources
} from '@/api/crawler'
import { cleanParams } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const sources = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ sourceId: '', status: '', taskType: '' })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ sourceId: null, taskType: 'FULL', seedUrls: '', maxDepth: 3, maxPages: 500 })
const rules = {
  sourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }]
}

function typeLabel(t) {
  const map = { FULL: '全站采集', INCREMENTAL: '增量采集', SINGLE: '单页采集' }
  return map[t] || t
}

function statusLabel(s) {
  const map = { QUEUED: '排队中', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败', STOPPED: '已停止' }
  return map[s] || s
}

function statusType(s) {
  const map = { QUEUED: 'info', RUNNING: 'success', COMPLETED: 'success', FAILED: 'danger', STOPPED: 'warning' }
  return map[s] || 'info'
}

async function loadSources() {
  try { sources.value = await getActiveCrawlerSources() || [] } catch { sources.value = [] }
}

async function loadList() {
  loading.value = true
  try {
    const data = await getCrawlerTaskPage({
      page: page.pageNum, size: page.pageSize, ...cleanParams(query)
    })
    list.value = (data && data.records) || []
    page.total = (data && data.total) || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.pageNum = 1; loadList() }
function handleReset() { query.sourceId = ''; query.status = ''; query.taskType = ''; handleSearch() }

function openCreate() {
  form.sourceId = null; form.taskType = 'FULL'; form.seedUrls = ''; form.maxDepth = 3; form.maxPages = 500
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createCrawlerTask({ ...form })
    ElMessage.success('任务创建成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleStart(row) {
  await startCrawlerTask(row.id)
  ElMessage.success('任务已启动')
  loadList()
}

async function handleStop(row) {
  try { await ElMessageBox.confirm('确认停止运行中的任务？', '提示', { type: 'warning' }) } catch { return }
  await stopCrawlerTask(row.id)
  ElMessage.success('任务已停止')
  loadList()
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm(`确认删除任务 #${row.id}？`, '提示', { type: 'warning' }) } catch { return }
  await deleteCrawlerTask(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => { loadSources(); loadList() })
</script>