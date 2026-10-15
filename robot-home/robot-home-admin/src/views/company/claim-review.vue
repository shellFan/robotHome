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
        <span class="page-title">企业认领申请</span>
        <el-button :icon="'Refresh'" :loading="loading" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="申请ID" width="90" />
        <el-table-column prop="companyId" label="企业ID" width="90" />
        <el-table-column label="申请人" min-width="150">
          <template #default="{ row }">
            <div>{{ row.applicantName || '-' }}</div>
            <div class="cell-secondary">用户ID：{{ row.applicantUserId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="applicantPosition" label="职位" width="120" show-overflow-tooltip />
        <el-table-column label="联系方式" min-width="180">
          <template #default="{ row }">
            <div>{{ row.applicantPhone || '-' }}</div>
            <div class="cell-secondary">{{ row.applicantEmail || '-' }}</div>
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
        <template #empty><el-empty description="暂无认领申请" /></template>
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

    <el-dialog v-model="dialogVisible" title="企业认领申请详情" width="760px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="申请ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="企业ID">{{ detail.companyId }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.applicantUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ detail.applicantPosition || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.applicantPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱" :span="2">{{ detail.applicantEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认领理由" :span="2">
          <div class="long-text">{{ detail.claimReason || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="证明材料" :span="2">
          <div v-if="proofMaterials.length" class="proof-list">
            <div v-for="(item, index) in proofMaterials" :key="`${item.url}-${index}`">
              <a v-if="isSafeUrl(item.url)" :href="item.url" target="_blank" rel="noopener noreferrer">
                {{ item.label }}
              </a>
              <span v-else>{{ item.label }}</span>
            </div>
          </div>
          <span v-else>-</span>
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
  getCompanyClaimPage,
  reviewCompanyClaim
} from '@/api/company'
import { useUserStore } from '@/store/user'
import { cleanParams, formatTime, safeParse } from '@/utils'

const userStore = useUserStore()
const canReview = computed(() => userStore.hasPermission('company:claim:review'))
const loading = ref(false)
const reviewing = ref(false)
const list = ref([])
const query = reactive({ status: 'PENDING' })
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const dialogVisible = ref(false)
const detail = ref(null)
const reviewAction = ref(null)
const reviewComment = ref('')

const proofMaterials = computed(() => {
  const parsed = safeParse(detail.value && detail.value.proofMaterials, [])
  const values = Array.isArray(parsed) ? parsed : [parsed]
  return values
    .map((item, index) => {
      if (typeof item === 'string') return { url: item, label: item || `材料 ${index + 1}` }
      if (item && typeof item === 'object') {
        const url = item.url || item.path || ''
        return { url, label: item.name || item.fileName || url || `材料 ${index + 1}` }
      }
      return null
    })
    .filter(Boolean)
})

function statusMeta(status) {
  return COMPANY_REVIEW_STATUS_MAP[status] || { label: status || '未知', type: 'info' }
}

function isSafeUrl(value) {
  return typeof value === 'string' && (/^https?:\/\//i.test(value) || value.startsWith('/'))
}

async function loadList() {
  loading.value = true
  try {
    const data = await getCompanyClaimPage({
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
      approved ? '确认通过该企业认领申请？' : '确认拒绝该企业认领申请？',
      '审核确认',
      { type: approved ? 'success' : 'warning' }
    )
  } catch (e) {
    return
  }

  reviewing.value = true
  try {
    const updated = await reviewCompanyClaim(detail.value.id, approved, comment || undefined)
    detail.value = updated || { ...detail.value, status: approved ? 'APPROVED' : 'REJECTED', reviewComment: comment }
    ElMessage.success(approved ? '认领申请已通过' : '认领申请已拒绝')
    await loadList()
  } finally {
    reviewing.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.cell-secondary {
  margin-top: 3px;
  color: var(--rh-text-weak);
  font-size: 12px;
}
.long-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.65;
}
.proof-list {
  display: grid;
  gap: 6px;
  overflow-wrap: anywhere;
}
</style>
