<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="审核状态" clearable style="width: 160px">
          <el-option
            v-for="(item, key) in COMPANY_REVIEW_STATUS_MAP"
            :key="key"
            :label="item.label"
            :value="key"
          />
        </el-select>
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button :disabled="loading" @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card">
      <div class="table-toolbar">
        <span class="page-title">企业资料变更申请</span>
        <el-button :icon="'Refresh'" :loading="loading" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="申请ID" width="90" />
        <el-table-column prop="companyId" label="企业ID" width="90" />
        <el-table-column label="变更字段" min-width="150">
          <template #default="{ row }">
            <div>{{ row.fieldLabel || row.fieldName || '-' }}</div>
            <div class="cell-secondary">{{ row.fieldName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="提交人" min-width="130">
          <template #default="{ row }">
            <div>{{ row.applicantName || '-' }}</div>
            <div class="cell-secondary">用户ID：{{ row.applicantUserId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="变更内容" min-width="220">
          <template #default="{ row }">
            <div class="change-summary">
              <span class="change-summary__old">{{ compactValue(row.oldValue) }}</span>
              <el-icon><Right /></el-icon>
              <span class="change-summary__new">{{ compactValue(row.newValue) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING' && canReview">
              <el-button link type="success" @click="openDetail(row, true)">通过</el-button>
              <el-button link type="danger" @click="openDetail(row, false)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无资料变更申请" /></template>
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

    <el-dialog v-model="dialogVisible" title="企业资料变更详情" width="820px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="申请ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="企业ID">{{ detail.companyId }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ detail.applicantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.applicantUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="变更类型">{{ detail.changeType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="变更字段">
          {{ detail.fieldLabel || detail.fieldName || '-' }}
          <span v-if="detail.fieldName" class="field-name">（{{ detail.fieldName }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="变更理由" :span="2">
          <div class="long-text">{{ detail.changeReason || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMeta(detail.status).type" size="small">
            {{ statusMeta(detail.status).label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.status !== 'PENDING'" label="审核人">
          {{ detail.reviewerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.status !== 'PENDING'" label="审核时间">
          {{ formatTime(detail.reviewTime) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.status !== 'PENDING'" label="审核意见" :span="2">
          <div class="long-text">{{ detail.reviewComment || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">变更内容对比</el-divider>
      <div v-if="detail" class="compare-grid">
        <section class="compare-card compare-card--old">
          <div class="compare-card__title">原值</div>
          <el-image
            v-if="isImageChange && isSafeUrl(detail.oldValue)"
            :src="detail.oldValue"
            :preview-src-list="[detail.oldValue]"
            fit="contain"
            class="compare-image"
          />
          <a v-else-if="isWebsiteChange && isSafeUrl(detail.oldValue)" :href="detail.oldValue" target="_blank" rel="noopener noreferrer">
            {{ detail.oldValue }}
          </a>
          <div v-else class="compare-value">{{ detail.oldValue || '（空）' }}</div>
        </section>
        <section class="compare-card compare-card--new">
          <div class="compare-card__title">新值</div>
          <el-image
            v-if="isImageChange && isSafeUrl(detail.newValue)"
            :src="detail.newValue"
            :preview-src-list="[detail.newValue]"
            fit="contain"
            class="compare-image"
          />
          <a v-else-if="isWebsiteChange && isSafeUrl(detail.newValue)" :href="detail.newValue" target="_blank" rel="noopener noreferrer">
            {{ detail.newValue }}
          </a>
          <div v-else class="compare-value">{{ detail.newValue || '（空）' }}</div>
        </section>
      </div>

      <template v-if="detail && detail.status === 'PENDING' && canReview">
        <el-divider content-position="left">审核处理</el-divider>
        <el-form label-width="86px">
          <el-form-item :label="reviewAction === false ? '拒绝原因' : '审核意见'" :required="reviewAction === false">
            <el-input
              v-model="reviewComment"
              type="textarea"
              :rows="3"
              maxlength="512"
              show-word-limit
              :placeholder="reviewAction === false ? '请输入拒绝原因' : '可填写审核意见'"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button :disabled="reviewing" @click="dialogVisible = false">关闭</el-button>
        <template v-if="detail && detail.status === 'PENDING' && canReview">
          <el-button
            type="danger"
            :loading="reviewing && reviewAction === false"
            :disabled="reviewing"
            @click="submitReview(false)"
          >拒绝</el-button>
          <el-button
            type="success"
            :loading="reviewing && reviewAction === true"
            :disabled="reviewing"
            @click="submitReview(true)"
          >通过</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  COMPANY_REVIEW_STATUS_MAP,
  getCompanyProfileChangePage,
  reviewCompanyProfileChange
} from '@/api/company'
import { useUserStore } from '@/store/user'
import { cleanParams, formatTime } from '@/utils'

const userStore = useUserStore()
const canReview = computed(() => userStore.hasPermission('company:profile:review'))
const loading = ref(false)
const reviewing = ref(false)
const list = ref([])
const query = reactive({ status: 'PENDING' })
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const dialogVisible = ref(false)
const detail = ref(null)
const reviewAction = ref(null)
const reviewComment = ref('')
const isImageChange = computed(() => detail.value && detail.value.fieldName === 'logo')
const isWebsiteChange = computed(() => detail.value && detail.value.fieldName === 'website')

function statusMeta(status) {
  return COMPANY_REVIEW_STATUS_MAP[status] || { label: status || '未知', type: 'info' }
}

function compactValue(value) {
  if (value === null || value === undefined || value === '') return '空'
  const text = String(value).replace(/\s+/g, ' ').trim()
  return text.length > 34 ? `${text.slice(0, 34)}…` : text
}

function isSafeUrl(value) {
  return typeof value === 'string' && (/^https?:\/\//i.test(value) || value.startsWith('/'))
}

async function loadList() {
  loading.value = true
  try {
    const data = await getCompanyProfileChangePage({
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      ...cleanParams(query)
    })
    list.value = (data && data.list) || []
    page.total = (data && data.total) || 0
  } catch (e) {
    list.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.pageNum = 1
  loadList()
}

function handleReset() {
  query.status = 'PENDING'
  handleSearch()
}

function openDetail(row, action = null) {
  detail.value = { ...row }
  reviewAction.value = action
  reviewComment.value = ''
  dialogVisible.value = true
}

async function submitReview(approved) {
  if (!detail.value || reviewing.value) return
  const comment = reviewComment.value.trim()
  if (!approved && !comment) {
    reviewAction.value = false
    ElMessage.warning('请输入拒绝原因')
    return
  }

  reviewAction.value = approved
  try {
    await ElMessageBox.confirm(
      approved ? '确认通过该资料变更申请并应用新值？' : '确认拒绝该资料变更申请？',
      '审核确认',
      { type: approved ? 'success' : 'warning' }
    )
  } catch (e) {
    return
  }

  reviewing.value = true
  try {
    const updated = await reviewCompanyProfileChange(detail.value.id, approved, comment || undefined)
    detail.value = updated || { ...detail.value, status: approved ? 'APPROVED' : 'REJECTED', reviewComment: comment }
    ElMessage.success(approved ? '资料变更已通过并应用' : '资料变更已拒绝')
    await loadList()
  } finally {
    reviewing.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.cell-secondary,
.field-name {
  color: var(--rh-text-weak);
  font-size: 12px;
}
.cell-secondary {
  margin-top: 3px;
}
.change-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.change-summary__old,
.change-summary__new {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.change-summary__old {
  color: var(--rh-text-weak);
  text-decoration: line-through;
}
.change-summary__new {
  color: var(--rh-text);
}
.long-text,
.compare-value {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.65;
}
.compare-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}
.compare-card {
  min-height: 130px;
  padding: 14px;
  border: 1px solid var(--rh-border);
  border-radius: 6px;
  overflow-wrap: anywhere;
}
.compare-card--old {
  background: #fafafa;
}
.compare-card--new {
  background: #f0f9eb;
  border-color: #c2e7b0;
}
.compare-card__title {
  margin-bottom: 10px;
  color: var(--rh-text-secondary);
  font-weight: 600;
}
.compare-image {
  width: 100%;
  height: 170px;
}
@media (max-width: 720px) {
  .compare-grid {
    grid-template-columns: 1fr;
  }
}
</style>
