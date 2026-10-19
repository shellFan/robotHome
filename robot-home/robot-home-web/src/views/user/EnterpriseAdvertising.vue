<template>
  <section class="advertising-page">
    <div class="rh-card ad-card">
      <div class="ad-heading">
        <div>
          <h1>广告管理</h1>
          <p>创建投放计划、管理素材、查看投放数据</p>
        </div>
        <div class="ad-heading__actions">
          <el-button @click="loadSlots">刷新广告位</el-button>
          <el-button type="primary" @click="showCreateDialog = true">创建投放</el-button>
        </div>
      </div>

      <div v-if="loading" class="rh-empty">正在加载…</div>
      <el-alert v-else-if="error" type="error" :closable="false" :title="error" />
      <el-alert v-else-if="!companyId" type="warning" :closable="false" title="请先认领企业后再使用广告管理" />

      <template v-else>
        <!-- 广告位列表 -->
        <section class="slots-section">
          <h2>可用广告位</h2>
          <el-table v-if="slots.length" :data="slots" size="small">
            <el-table-column prop="slotCode" label="广告位编码" width="160" />
            <el-table-column prop="slotName" label="名称" />
            <el-table-column prop="slotType" label="类型" width="100" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无可用广告位" :image-size="60" />
        </section>

        <!-- 我的投放计划 -->
        <section class="campaigns-section">
          <div class="campaigns-heading">
            <h2>我的投放计划</h2>
            <el-select v-model="campaignStatus" size="small" style="width: 140px" @change="loadCampaigns">
              <el-option label="全部状态" value="" />
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审核" value="PENDING_REVIEW" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="投放中" value="ONLINE" />
              <el-option label="已下线" value="OFFLINE" />
            </el-select>
          </div>
          <el-table v-if="campaigns.length" :data="campaigns" size="small">
            <el-table-column prop="name" label="计划名称" min-width="140" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="campaignStatusType(row.status)" size="small">{{ campaignStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="投放时间" min-width="180">
              <template #default="{ row }">
                {{ formatDate(row.startTime) }} ~ {{ formatDate(row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="dailyLimit" label="日限额" width="80">
              <template #default="{ row }">{{ row.dailyLimit || '-' }}</template>
            </el-table-column>
            <el-table-column prop="totalLimit" label="总限额" width="80">
              <template #default="{ row }">{{ row.totalLimit || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" size="small" @click="openCreativeDialog(row)">添加素材</el-button>
                <el-button v-if="canSubmit(row.status)" link type="primary" size="small" @click="submitCampaign(row)">提交审核</el-button>
                <el-button v-if="row.status === 'APPROVED'" link type="success" size="small" @click="onlineCampaign(row)">上线</el-button>
                <el-button v-if="row.status === 'ONLINE'" link type="warning" size="small" @click="offlineCampaign(row)">下线</el-button>
                <el-button link size="small" @click="viewStats(row)">数据</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无投放计划" :image-size="60" />
        </section>
      </template>
    </div>

    <!-- 创建投放对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建投放计划" width="520px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="广告位" required>
          <el-select v-model="createForm.slotId" placeholder="选择广告位" style="width: 100%">
            <el-option v-for="s in slots.filter(x => x.status === 'ACTIVE')" :key="s.id" :label="s.slotName + ' (' + s.slotCode + ')'" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划名称" required>
          <el-input v-model="createForm.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="createForm.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="createForm.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日限额">
          <el-input-number v-model="createForm.dailyLimit" :min="0" placeholder="0为不限" />
        </el-form-item>
        <el-form-item label="总限额">
          <el-input-number v-model="createForm.totalLimit" :min="0" placeholder="0为不限" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createCampaign">创建</el-button>
      </template>
    </el-dialog>

    <!-- 添加素材对话框 -->
    <el-dialog v-model="showCreativeDialog" title="添加广告素材" width="520px">
      <el-form :model="creativeForm" label-width="100px">
        <el-form-item label="素材类型" required>
          <el-select v-model="creativeForm.mediaType" style="width: 100%">
            <el-option label="图片" value="IMAGE" />
            <el-option label="视频" value="VIDEO" />
          </el-select>
        </el-form-item>
        <el-form-item label="素材链接" required>
          <el-input v-model="creativeForm.mediaUrl" maxlength="500" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="creativeForm.jumpUrl" maxlength="500" placeholder="https://... (仅支持http/https)" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="creativeForm.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="creativeForm.description" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreativeDialog = false">取消</el-button>
        <el-button type="primary" :loading="addingCreative" @click="addCreative">添加</el-button>
      </template>
    </el-dialog>

    <!-- 投放统计对话框 -->
    <el-dialog v-model="showStatsDialog" title="投放统计" width="480px">
      <div v-if="statsLoading" class="rh-empty">加载中…</div>
      <template v-else-if="stats">
        <div class="stats-grid">
          <div class="stats-item"><span class="stats-label">展示次数</span><span class="stats-value">{{ stats.impressions || 0 }}</span></div>
          <div class="stats-item"><span class="stats-label">点击次数</span><span class="stats-value">{{ stats.clicks || 0 }}</span></div>
          <div class="stats-item"><span class="stats-label">点击率</span><span class="stats-value">{{ stats.ctr || '0%' }}</span></div>
        </div>
      </template>
      <el-empty v-else description="暂无数据" :image-size="60" />
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { advertisingApi, enterpriseApi } from '@/api'

const loading = ref(true)
const error = ref('')
const companyId = ref(null)
const slots = ref([])
const campaigns = ref([])
const campaignStatus = ref('')
const showCreateDialog = ref(false)
const showCreativeDialog = ref(false)
const showStatsDialog = ref(false)
const creating = ref(false)
const addingCreative = ref(false)
const statsLoading = ref(false)
const stats = ref(null)
const currentCampaign = ref(null)

const createForm = reactive({ slotId: null, name: '', startTime: null, endTime: null, dailyLimit: 0, totalLimit: 0 })
const creativeForm = reactive({ mediaType: 'IMAGE', mediaUrl: '', jumpUrl: '', title: '', description: '' })

function campaignStatusText (status) {
  return { DRAFT: '草稿', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', ONLINE: '投放中', OFFLINE: '已下线' }[status] || status
}
function campaignStatusType (status) {
  return { DRAFT: 'info', PENDING_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger', ONLINE: 'success', OFFLINE: 'info' }[status] || 'info'
}
function canSubmit (status) {
  return status === 'DRAFT' || status === 'REJECTED'
}
function formatDate (val) {
  if (!val) return '-'
  return String(val).replace('T', ' ').substring(0, 16)
}
function formatDateTime (date) {
  if (!date) return ''
  const d = new Date(date)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function loadCompany () {
  try {
    const data = await enterpriseApi.myCompany()
    if (data && data.company) companyId.value = data.company.id
  } catch (e) { /* 未认领企业 */ }
}

async function loadSlots () {
  try {
    const data = await advertisingApi.slots()
    slots.value = data || []
  } catch (e) {
    ElMessage.error('广告位加载失败')
  }
}

async function loadCampaigns () {
  if (!companyId.value) return
  try {
    const params = { companyId: companyId.value, pageNum: 1, pageSize: 50 }
    if (campaignStatus.value) params.status = campaignStatus.value
    const data = await advertisingApi.myCampaigns(params)
    campaigns.value = (data && data.list) || []
  } catch (e) {
    ElMessage.error('投放计划加载失败')
  }
}

async function createCampaign () {
  if (creating.value) return
  if (!createForm.slotId || !createForm.name.trim() || !createForm.startTime || !createForm.endTime) {
    ElMessage.warning('请填写广告位、名称和投放时间')
    return
  }
  creating.value = true
  try {
    await advertisingApi.createCampaign({
      companyId: companyId.value,
      slotId: createForm.slotId,
      name: createForm.name.trim(),
      startTime: formatDateTime(createForm.startTime),
      endTime: formatDateTime(createForm.endTime),
      dailyLimit: createForm.dailyLimit || undefined,
      totalLimit: createForm.totalLimit || undefined
    })
    ElMessage.success('投放计划已创建')
    showCreateDialog.value = false
    createForm.name = ''
    createForm.slotId = null
    createForm.startTime = null
    createForm.endTime = null
    await loadCampaigns()
  } catch (e) {
    ElMessage.error('创建失败：' + (e.message || '请重试'))
  } finally {
    creating.value = false
  }
}

function openCreativeDialog (campaign) {
  currentCampaign.value = campaign
  creativeForm.mediaUrl = ''
  creativeForm.jumpUrl = ''
  creativeForm.title = ''
  creativeForm.description = ''
  showCreativeDialog.value = true
}

async function addCreative () {
  if (addingCreative.value || !currentCampaign.value) return
  if (!creativeForm.mediaUrl.trim() || !creativeForm.title.trim()) {
    ElMessage.warning('请填写素材链接和标题')
    return
  }
  addingCreative.value = true
  try {
    await advertisingApi.createCreative({
      campaignId: currentCampaign.value.id,
      companyId: companyId.value,
      mediaType: creativeForm.mediaType,
      mediaUrl: creativeForm.mediaUrl.trim(),
      jumpUrl: creativeForm.jumpUrl.trim() || undefined,
      title: creativeForm.title.trim(),
      description: creativeForm.description.trim() || undefined
    })
    ElMessage.success('素材已添加')
    showCreativeDialog.value = false
    await loadCampaigns()
  } catch (e) {
    ElMessage.error('添加失败：' + (e.message || '请重试'))
  } finally {
    addingCreative.value = false
  }
}

async function submitCampaign (campaign) {
  try {
    await ElMessageBox.confirm('提交后需等待管理员审核。', '提交审核')
    await advertisingApi.submitCampaign(campaign.id, companyId.value)
    ElMessage.success('已提交审核')
    await loadCampaigns()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('提交失败：' + (e.message || '请重试'))
  }
}

async function onlineCampaign (campaign) {
  try {
    await advertisingApi.goOnline(campaign.id, companyId.value)
    ElMessage.success('已上线')
    await loadCampaigns()
  } catch (e) {
    ElMessage.error('上线失败：' + (e.message || '请重试'))
  }
}

async function offlineCampaign (campaign) {
  try {
    await ElMessageBox.confirm('下线后广告将不再展示。', '下线投放')
    await advertisingApi.goOffline(campaign.id, companyId.value)
    ElMessage.success('已下线')
    await loadCampaigns()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('下线失败：' + (e.message || '请重试'))
  }
}

async function viewStats (campaign) {
  showStatsDialog.value = true
  statsLoading.value = true
  stats.value = null
  try {
    stats.value = await advertisingApi.campaignStats(campaign.id)
  } catch (e) {
    ElMessage.error('统计数据加载失败')
  } finally {
    statsLoading.value = false
  }
}

async function load () {
  loading.value = true
  error.value = ''
  try {
    await loadCompany()
    await Promise.all([loadSlots(), loadCampaigns()])
  } catch (e) {
    error.value = '数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.ad-card { padding: 24px; }
.ad-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.ad-heading h1 { margin: 0 0 6px; }
.ad-heading p { color: var(--rh-text-sub); margin: 0; }
.ad-heading__actions { display: flex; gap: 10px; }
.slots-section, .campaigns-section { margin-top: 24px; }
.slots-section h2, .campaigns-section h2 { margin-bottom: 14px; font-size: 17px; }
.campaigns-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.campaigns-heading h2 { margin: 0; font-size: 17px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.stats-item { display: flex; flex-direction: column; gap: 6px; padding: 16px; background: var(--rh-surface-sub); border-radius: 8px; text-align: center; }
.stats-label { font-size: 13px; color: var(--rh-text-sub); }
.stats-value { font-size: 22px; font-weight: 600; }
</style>