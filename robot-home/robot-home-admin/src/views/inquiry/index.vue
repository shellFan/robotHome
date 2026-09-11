<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 手机号"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option
            v-for="(item, key) in INQUIRY_STATUS_MAP"
            :key="key"
            :label="item.label"
            :value="Number(key)"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div v-if="statusCount" class="status-tags">
        <el-tag
          v-for="(item, key) in INQUIRY_STATUS_MAP"
          :key="key"
          :type="item.type"
          effect="plain"
          class="status-tag"
          @click="filterByStatus(Number(key))"
        >
          {{ item.label }} {{ statusCount[key] ?? 0 }}
        </el-tag>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">询价列表</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="联系人" width="100" />
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            <span>{{ maskPhone(row.phone) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="客户类型" width="100">
          <template #default="{ row }">{{ CUSTOMER_TYPE_MAP[row.customerType] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="robotName" label="意向产品" min-width="140" show-overflow-tooltip />
        <el-table-column prop="companyName" label="公司" min-width="140" show-overflow-tooltip />
        <el-table-column prop="budget" label="预算" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="(INQUIRY_STATUS_MAP[row.status] || {}).type || 'info'" size="small">
              {{ (INQUIRY_STATUS_MAP[row.status] || {}).label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">处理</el-button>
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

    <el-dialog v-model="dialogVisible" title="询价处理" width="680px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="联系人">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ maskPhone(detail.phone) }}</el-descriptions-item>
        <el-descriptions-item label="客户类型">
          {{ CUSTOMER_TYPE_MAP[detail.customerType] || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="地区">{{ detail.region || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公司">{{ detail.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="意向产品">{{ detail.robotName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预算">{{ detail.budget || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detail.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">更新状态</el-divider>
      <el-form label-width="90px">
        <el-form-item label="状态">
          <el-select v-model="handleForm.status" style="width: 200px">
            <el-option
              v-for="(item, key) in INQUIRY_STATUS_MAP"
              :key="key"
              :label="item.label"
              :value="Number(key)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleForm.handleNote" type="textarea" :rows="2" placeholder="状态变更备注" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveStatus">更新状态</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">跟进记录</el-divider>
      <el-timeline v-if="records.length">
        <el-timeline-item v-for="(r, i) in records" :key="i" :timestamp="r.time || r.createTime || ''">
          <div>{{ r.content }}</div>
          <div v-if="r.operator" class="text-weak" style="font-size: 12px">操作人：{{ r.operator }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无跟进记录" :image-size="60" />
      <div class="follow-form">
        <el-input v-model="followContent" type="textarea" :rows="2" placeholder="添加跟进备注" />
        <el-button type="primary" :loading="saving" style="margin-top: 8px" @click="addRecord">添加跟进</el-button>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInquiryPage,
  getInquiryDetail,
  updateInquiryStatus,
  addInquiryRecord,
  deleteInquiry,
  getInquiryStatusCount,
  INQUIRY_STATUS_MAP,
  CUSTOMER_TYPE_MAP
} from '@/api/inquiry'
import { cleanParams, formatTime, safeParse } from '@/utils'

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const statusCount = ref(null)
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', status: '' })

const dialogVisible = ref(false)
const detail = ref(null)
const records = ref([])
const handleForm = reactive({ status: 1, handleNote: '' })
const followContent = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await getInquiryPage({
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

async function loadStatusCount() {
  try {
    statusCount.value = (await getInquiryStatusCount()) || {}
  } catch (e) {
    statusCount.value = null
  }
}

function handleSearch() {
  page.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.status = ''
  handleSearch()
}

function filterByStatus(status) {
  query.status = status
  handleSearch()
}

async function openDetail(row) {
  const data = await getInquiryDetail(row.id)
  detail.value = data || row
  handleForm.status = (data && data.status) || row.status || 1
  handleForm.handleNote = (data && data.handleNote) || ''
  const raw = (data && data.handleRecords) || []
  records.value = Array.isArray(raw) ? raw : safeParse(raw, [])
  followContent.value = ''
  dialogVisible.value = true
}

async function saveStatus() {
  if (!detail.value) return
  saving.value = true
  try {
    await updateInquiryStatus(detail.value.id, handleForm.status, handleForm.handleNote)
    ElMessage.success('状态已更新')
    await openDetail(detail.value)
    loadList()
    loadStatusCount()
  } finally {
    saving.value = false
  }
}

async function addRecord() {
  if (!detail.value || !followContent.value.trim()) {
    ElMessage.warning('请输入跟进内容')
    return
  }
  saving.value = true
  try {
    await addInquiryRecord(detail.value.id, followContent.value.trim(), 'admin')
    ElMessage.success('跟进已添加')
    followContent.value = ''
    await openDetail(detail.value)
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除询价「${row.name || row.id}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteInquiry(row.id)
  ElMessage.success('删除成功')
  loadList()
  loadStatusCount()
}

onMounted(async () => {
  await Promise.all([loadList(), loadStatusCount()])
})
</script>

<style scoped>
.status-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.status-tag {
  cursor: pointer;
}
.follow-form {
  margin-top: 12px;
}
</style>
