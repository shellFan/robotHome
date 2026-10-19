<template>
  <div class="app-container">
    <div class="page-header">
      <h3>套餐管理</h3>
      <el-button type="primary" @click="openCreate">新建套餐</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="code" label="编码" width="110" />
      <el-table-column prop="name" label="名称" min-width="130" />
      <el-table-column prop="level" label="等级" width="70" />
      <el-table-column label="展示价" width="100">
        <template #default="{ row }">¥{{ formatPrice(row.displayPrice) }}</template>
      </el-table-column>
      <el-table-column prop="durationDays" label="周期(天)" width="90" />
      <el-table-column prop="currentVersion" label="版本" width="70" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="planStatusType(row.status)" size="small">{{ planStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="可售" width="70">
        <template #default="{ row }">
          <el-tag :type="row.saleable === 1 ? 'success' : 'info'" size="small">{{ row.saleable === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" type="primary" @click="openVersions(row)">版本</el-button>
          <el-button v-if="row.status === 'DRAFT'" text size="small" type="success" @click="changeStatus(row, 'ON_SHELF')">上架</el-button>
          <el-button v-if="row.status === 'ON_SHELF'" text size="small" type="warning" @click="changeStatus(row, 'OFF_SHELF')">下架</el-button>
          <el-button v-if="row.status === 'OFF_SHELF'" text size="small" type="success" @click="changeStatus(row, 'ON_SHELF')">重新上架</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="(p) => { pageNum = p; loadList() }" />

    <!-- 新建/编辑套餐 -->
    <el-dialog v-model="showForm" :title="form.id ? '编辑套餐' : '新建套餐'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编码"><el-input v-model="form.code" placeholder="FREE/BASIC/PRO/ENTERPRISE" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="等级"><el-input-number v-model="form.level" :min="0" :max="100" /></el-form-item>
        <el-form-item label="展示价"><el-input-number v-model="form.displayPrice" :min="0" :precision="2" :step="100" /></el-form-item>
        <el-form-item label="周期(天)"><el-input-number v-model="form.durationDays" :min="1" :max="3650" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="999" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 版本管理 -->
    <el-dialog v-model="showVersions" title="套餐版本管理" width="720px">
      <div v-if="currentPlan" class="version-plan-name">套餐：{{ currentPlan.name }}（当前版本 v{{ currentPlan.currentVersion }}）</div>
      <div style="margin-bottom: 12px;">
        <el-button type="primary" size="small" @click="showVersionForm = true">新建版本</el-button>
      </div>
      <el-table :data="versions" border size="small">
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="价格(分)" width="100"><template #default="{ row }">{{ row.priceCents }}</template></el-table-column>
        <el-table-column prop="durationDays" label="周期(天)" width="90" />
        <el-table-column prop="entitlementConfig" label="权益配置" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新建版本表单 -->
      <el-divider v-if="showVersionForm" />
      <el-form v-if="showVersionForm" :model="versionForm" label-width="110px" style="margin-top: 12px;">
        <el-form-item label="价格(分)"><el-input-number v-model="versionForm.priceCents" :min="0" /></el-form-item>
        <el-form-item label="周期(天)"><el-input-number v-model="versionForm.durationDays" :min="1" :max="3650" /></el-form-item>
        <el-form-item label="权益配置">
          <el-input v-model="versionForm.entitlementConfig" type="textarea" :rows="4" placeholder='[{"code":"LEAD_VIEW","quotaTotal":50},{"code":"LEAD_RESPONSE","quotaTotal":20}]' />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingVersion" @click="saveVersion">创建版本</el-button>
          <el-button @click="showVersionForm = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPlans, savePlan, updatePlanStatus, listPlanVersions, createPlanVersion } from '@/api/commercial'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showForm = ref(false)
const saving = ref(false)
const form = ref({ code: '', name: '', description: '', level: 0, displayPrice: 0, durationDays: 365, sort: 0 })

const showVersions = ref(false)
const currentPlan = ref(null)
const versions = ref([])
const showVersionForm = ref(false)
const savingVersion = ref(false)
const versionForm = ref({ priceCents: 0, durationDays: 365, entitlementConfig: '[]' })

function planStatusType(s) { return { ON_SHELF: 'success', DRAFT: 'info', OFF_SHELF: 'warning' }[s] || '' }
function planStatusText(s) { return { ON_SHELF: '已上架', DRAFT: '草稿', OFF_SHELF: '已下架' }[s] || s }
function formatPrice(val) { return val != null ? Number(val).toFixed(2) : '0.00' }

async function loadList() {
  loading.value = true
  try {
    const data = await listPlans({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (data) { list.value = data.list || []; total.value = data.total || 0 }
  } finally { loading.value = false }
}

function openCreate() {
  form.value = { code: '', name: '', description: '', level: 0, displayPrice: 0, durationDays: 365, sort: 0 }
  showForm.value = true
}

async function save() {
  saving.value = true
  try {
    await savePlan(form.value)
    ElMessage.success('保存成功')
    showForm.value = false
    loadList()
  } finally { saving.value = false }
}

async function changeStatus(row, status) {
  try {
    await ElMessageBox.confirm(`确认将套餐「${row.name}」${status === 'ON_SHELF' ? '上架' : '下架'}？`, '状态变更')
    await updatePlanStatus(row.id, status)
    ElMessage.success('状态已更新')
    loadList()
  } catch (e) { /* cancel */ }
}

async function openVersions(row) {
  currentPlan.value = row
  showVersions.value = true
  showVersionForm.value = false
  versionForm.value = { priceCents: 0, durationDays: row.durationDays || 365, entitlementConfig: '[]' }
  const data = await listPlanVersions(row.id)
  versions.value = data || []
}

async function saveVersion() {
  if (!currentPlan.value) return
  try { JSON.parse(versionForm.value.entitlementConfig) } catch (e) {
    ElMessage.error('权益配置必须是合法JSON数组'); return
  }
  savingVersion.value = true
  try {
    await createPlanVersion(currentPlan.value.id, versionForm.value)
    ElMessage.success('版本已创建')
    showVersionForm.value = false
    const data = await listPlanVersions(currentPlan.value.id)
    versions.value = data || []
    loadList()
  } finally { savingVersion.value = false }
}

onMounted(loadList)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.version-plan-name { margin-bottom: 10px; font-weight: 600; }
</style>