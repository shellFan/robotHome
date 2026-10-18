<template>
  <section class="enterprise-workspace">
    <div class="rh-card workspace-card">
      <div class="workspace-heading">
        <div>
          <h1>企业工作台</h1>
          <p>查看认领进度，并维护已认证企业资料。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>

      <div v-if="loading" class="rh-empty">正在加载企业信息…</div>
      <el-alert v-else-if="error" type="error" :closable="false" :title="error" />

      <template v-else>
        <section v-if="company" class="company-summary">
          <img :src="imageOf(company.logo)" :alt="company.name" />
          <div>
            <h2>{{ company.name }}</h2>
            <el-tag :type="isVerified ? 'success' : 'warning'">
              {{ isVerified ? '已认证' : '待认证' }}
            </el-tag>
            <span class="member-role">你的角色：{{ roleText(myRole) }}</span>
          </div>
        </section>

        <el-empty v-else-if="!claimCompanyId" description="当前账号还没有企业工作台。可从企业详情页发起认领。" />

        <section v-if="claimCompanyId" class="claim-panel">
          <h2>认领 {{ claimCompanyName || ('企业 #' + claimCompanyId) }}</h2>
          <el-alert v-if="claimStatus === 'PENDING'" type="info" :closable="false" title="认领申请正在审核中" />
          <el-alert v-else-if="claimStatus === 'APPROVED'" type="success" :closable="false" title="该企业已完成认证" />
          <el-form v-else :model="claimForm" label-width="110px" @submit.prevent="submitClaim">
            <el-form-item label="申请人姓名" required><el-input v-model="claimForm.applicantName" maxlength="64" /></el-form-item>
            <el-form-item label="联系电话" required><el-input v-model="claimForm.applicantPhone" maxlength="32" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="claimForm.applicantEmail" maxlength="128" /></el-form-item>
            <el-form-item label="企业职位"><el-input v-model="claimForm.applicantPosition" maxlength="64" /></el-form-item>
            <el-form-item label="认领理由" required><el-input v-model="claimForm.claimReason" type="textarea" maxlength="512" :rows="3" /></el-form-item>
            <el-form-item label="证明材料链接"><el-input v-model="claimForm.proofMaterials" type="textarea" maxlength="2000" :rows="2" placeholder="可选，每行填写一个可访问的证明材料链接" /></el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitClaim">提交认领申请</el-button>
          </el-form>
        </section>

        <!-- 商业概览 -->
        <section v-if="company && isVerified" class="commercial-panel">
          <div class="commercial-heading">
            <h2>商业概览</h2>
            <el-button size="small" @click="loadCommercial">刷新</el-button>
          </div>
          <div v-if="commercialLoading" class="rh-empty">正在加载商业数据…</div>
          <el-alert v-else-if="commercialError" type="warning" :closable="false" :title="commercialError" />
          <template v-else-if="commercial">
            <div class="commercial-grid">
              <div class="commercial-card">
                <span class="commercial-card__label">当前套餐</span>
                <span class="commercial-card__value">{{ commercial.subscription ? commercial.subscription.planName : '免费版' }}</span>
                <el-tag v-if="commercial.subscription" :type="subStatusType(commercial.subscription.status)" size="small">
                  {{ subStatusText(commercial.subscription.status) }}
                </el-tag>
                <el-tag v-else type="info" size="small">免费版</el-tag>
              </div>
              <div class="commercial-card">
                <span class="commercial-card__label">到期时间</span>
                <span class="commercial-card__value">{{ commercial.subscription && commercial.subscription.endTime ? formatDate(commercial.subscription.endTime) : '-' }}</span>
              </div>
              <div class="commercial-card">
                <span class="commercial-card__label">商业订单</span>
                <span class="commercial-card__value">{{ commercial.orderStats != null ? commercial.orderStats : 0 }} 笔</span>
              </div>
            </div>
            <div v-if="commercial.entitlements && commercial.entitlements.length" class="ent-quick">
              <h3>权益使用</h3>
              <div class="ent-quick__list">
                <div v-for="ent in commercial.entitlements" :key="ent.entCode" class="ent-quick__item">
                  <span class="ent-quick__name">{{ entLabel(ent.entCode) }}</span>
                  <span v-if="ent.quotaTotal === 0" class="ent-quick__quota">不限量</span>
                  <span v-else class="ent-quick__quota" :class="{ 'ent-quick__quota--full': ent.quotaUsed >= ent.quotaTotal }">
                    {{ ent.quotaUsed }} / {{ ent.quotaTotal }}
                  </span>
                </div>
              </div>
            </div>
            <div class="commercial-actions">
              <el-button type="primary" size="small" @click="$router.push('/plans')">升级套餐</el-button>
              <el-button size="small" @click="$router.push('/user/subscription')">订阅详情</el-button>
              <el-button size="small" @click="$router.push('/user/orders')">商业订单</el-button>
              <el-button size="small" @click="$router.push('/user/advertising')">广告管理</el-button>
            </div>
          </template>
          <el-empty v-else description="暂无商业数据" :image-size="60" />
        </section>

        <section v-if="company && isVerified" class="profile-panel">
          <h2>企业资料维护</h2>
          <el-form :model="profileForm" label-width="110px" @submit.prevent="saveProfile">
            <el-form-item label="维护字段">
              <el-select v-model="profileForm.fieldName" @change="syncProfileValue">
                <el-option label="企业简介（即时更新）" value="intro" />
                <el-option label="官网（审核后更新）" value="website" />
                <el-option label="联系电话（审核后更新）" value="contactPhone" />
              </el-select>
            </el-form-item>
            <el-form-item label="资料内容">
              <el-input v-model="profileForm.newValue" :type="profileForm.fieldName === 'intro' ? 'textarea' : 'text'" maxlength="2000" :rows="3" />
            </el-form-item>
            <el-form-item v-if="profileForm.fieldName !== 'intro'" label="修改原因">
              <el-input v-model="profileForm.changeReason" maxlength="512" />
            </el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">{{ profileForm.fieldName === 'intro' ? '保存资料' : '提交审核' }}</el-button>
          </el-form>
          <h3>最近资料变更</h3>
          <el-table v-if="profileChanges.length" :data="profileChanges" size="small">
            <el-table-column prop="fieldLabel" label="字段" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="reviewComment" label="审核意见" />
            <el-table-column prop="createTime" label="提交时间" />
          </el-table>
          <el-empty v-else description="暂无资料变更记录" :image-size="60" />
        </section>

        <section class="claims-panel">
          <h2>我的认领记录</h2>
          <el-table v-if="claims.length" :data="claims" size="small">
            <el-table-column prop="companyId" label="企业 ID" width="90" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="createTime" label="提交时间" />
            <el-table-column prop="reviewComment" label="审核意见" />
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button v-if="scope.row.status === 'PENDING'" link type="danger" :loading="cancellingId === scope.row.id" @click="cancelClaim(scope.row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无认领记录" :image-size="60" />
        </section>
      </template>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { commercialApi, enterpriseApi } from '@/api'
import { imageOf } from '@/utils/format'

const commercial = ref(null)
const commercialLoading = ref(false)
const commercialError = ref('')

const ENT_LABELS = {
  LEAD_VIEW: '商机查看',
  LEAD_RESPONSE: '商机响应',
  PRODUCT_MANAGEMENT: '产品管理',
  PRODUCT_FEATURED: '精选推荐位',
  AD_CAMPAIGN: '广告投放',
  ANALYTICS_ADVANCED: '高级数据分析',
  COMPANY_PROFILE_ENHANCED: '企业主页增强'
}
function entLabel (code) { return ENT_LABELS[code] || code }
function subStatusText (status) {
  return { FREE: '免费版', ACTIVE: '使用中', EXPIRING: '即将到期', EXPIRED: '已过期', SUSPENDED: '已暂停', CANCELLED: '已取消' }[status] || status
}
function subStatusType (status) {
  return { ACTIVE: 'success', FREE: 'info', EXPIRING: 'warning', EXPIRED: 'danger', SUSPENDED: 'danger' }[status] || 'info'
}
function formatDate (val) {
  if (!val) return '-'
  return String(val).replace('T', ' ').substring(0, 10)
}

async function loadCommercial () {
  if (!company.value || !isVerified.value) return
  commercialLoading.value = true
  commercialError.value = ''
  try {
    commercial.value = await commercialApi.commercialOverview(company.value.id)
  } catch (e) {
    commercialError.value = '商业数据加载失败，请稍后重试'
  } finally {
    commercialLoading.value = false
  }
}

const route = useRoute()
const loading = ref(true)
const error = ref('')
const company = ref(null)
const myRole = ref('')
const isVerified = ref(false)
const claims = ref([])
const profileChanges = ref([])
const claimStatus = ref('')
const submitting = ref(false)
const savingProfile = ref(false)
const cancellingId = ref(null)
const claimCompanyId = computed(() => /^\d+$/.test(String(route.query.companyId || '')) ? String(route.query.companyId) : '')
const claimCompanyName = computed(() => String(route.query.companyName || ''))

const claimForm = reactive({ applicantName: '', applicantPhone: '', applicantEmail: '', applicantPosition: '', claimReason: '', proofMaterials: '' })
const profileForm = reactive({ fieldName: 'intro', newValue: '', changeReason: '' })

const roleText = (role) => ({ OWNER: '企业所有者', ADMIN: '企业管理员', MEMBER: '企业成员' }[role] || '企业成员')

async function load () {
  loading.value = true
  error.value = ''
  try {
    const [workspace, claimPage] = await Promise.all([
      enterpriseApi.myCompany(),
      enterpriseApi.myClaims({ pageNum: 1, pageSize: 10 })
    ])
    company.value = workspace && workspace.company ? workspace.company : null
    myRole.value = workspace && workspace.myRole
    isVerified.value = !!(workspace && workspace.isVerified)
    claims.value = (claimPage && claimPage.list) || []
    if (company.value) {
      profileForm.newValue = company.value.intro || ''
      const changePage = await enterpriseApi.myProfileChanges({ pageNum: 1, pageSize: 10 })
      profileChanges.value = (changePage && changePage.list) || []
    }
    if (claimCompanyId.value) {
      const latest = await enterpriseApi.latestClaim(claimCompanyId.value)
      claimStatus.value = latest && latest.status ? latest.status : ''
    }
    await loadCommercial()
  } catch (e) {
    error.value = '企业信息加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function submitClaim () {
  if (submitting.value) return
  if (!claimForm.applicantName.trim() || !claimForm.applicantPhone.trim() || !claimForm.claimReason.trim()) {
    ElMessage.warning('请填写申请人、联系电话和认领理由')
    return
  }
  submitting.value = true
  try {
    const payload = {
      companyId: claimCompanyId.value,
      applicantName: claimForm.applicantName.trim(),
      applicantPhone: claimForm.applicantPhone.trim(),
      claimReason: claimForm.claimReason.trim()
    }
    if (claimForm.applicantEmail.trim()) payload.applicantEmail = claimForm.applicantEmail.trim()
    if (claimForm.applicantPosition.trim()) payload.applicantPosition = claimForm.applicantPosition.trim()
    const links = claimForm.proofMaterials.split(/\r?\n/).map((v) => v.trim()).filter(Boolean)
    if (links.length) payload.proofMaterials = JSON.stringify(links)
    await enterpriseApi.submitClaim(payload)
    claimStatus.value = 'PENDING'
    ElMessage.success('认领申请已提交')
    await load()
  } catch (e) {
    ElMessage.error('提交失败，请确认申请状态后重试')
  } finally {
    submitting.value = false
  }
}

function syncProfileValue () {
  const fields = { intro: 'intro', website: 'website', contactPhone: 'contactPhone' }
  profileForm.newValue = company.value ? (company.value[fields[profileForm.fieldName]] || '') : ''
}

async function saveProfile () {
  if (savingProfile.value || !company.value || !isVerified.value) return
  savingProfile.value = true
  try {
    if (profileForm.fieldName === 'intro') {
      await enterpriseApi.directUpdate({ companyId: company.value.id, fieldName: 'intro', newValue: profileForm.newValue })
      company.value.intro = profileForm.newValue
      ElMessage.success('企业简介已更新')
    } else {
      await enterpriseApi.submitProfileChange({
        companyId: company.value.id,
        fieldName: profileForm.fieldName,
        newValue: profileForm.newValue,
        changeReason: profileForm.changeReason
      })
      const page = await enterpriseApi.myProfileChanges({ pageNum: 1, pageSize: 10 })
      profileChanges.value = (page && page.list) || []
      ElMessage.success('资料变更已提交审核')
    }
  } catch (e) {
    ElMessage.error('资料保存失败，请检查认证状态后重试')
  } finally {
    savingProfile.value = false
  }
}

async function cancelClaim (claim) {
  if (cancellingId.value) return
  try {
    await ElMessageBox.confirm('取消后该申请将不再进入审核流程。', '取消认领申请')
    cancellingId.value = claim.id
    await enterpriseApi.cancelClaim(claim.id)
    claim.status = 'CANCELLED'
    ElMessage.success('申请已取消')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('取消失败，请刷新后重试')
  } finally {
    cancellingId.value = null
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.workspace-card { padding: 24px; }
.workspace-heading, .company-summary { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.workspace-heading h1, .company-summary h2 { margin: 0 0 8px; }
.workspace-heading p, .member-role { color: var(--rh-text-sub); }
.company-summary { justify-content: flex-start; padding: 18px 0; border-bottom: 1px solid var(--rh-border-light); }
.company-summary img { width: 72px; height: 72px; object-fit: contain; border-radius: 8px; background: var(--rh-surface-sub); }
.member-role { margin-left: 12px; font-size: 13px; }
.claim-panel, .profile-panel, .claims-panel { margin-top: 24px; }
.claim-panel h2, .profile-panel h2, .claims-panel h2 { margin-bottom: 16px; font-size: 18px; }
.profile-panel h3 { margin: 24px 0 12px; font-size: 15px; }
.claim-panel .el-form, .profile-panel .el-form { max-width: 760px; }
.commercial-panel { margin-top: 24px; padding: 20px; background: var(--rh-surface-sub); border-radius: 10px; }
.commercial-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.commercial-heading h2 { margin: 0; font-size: 18px; }
.commercial-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 18px; }
.commercial-card { display: flex; flex-direction: column; gap: 6px; padding: 14px; background: var(--rh-surface, #fff); border-radius: 8px; border: 1px solid var(--rh-border-light); }
.commercial-card__label { font-size: 13px; color: var(--rh-text-sub); }
.commercial-card__value { font-size: 18px; font-weight: 600; }
.ent-quick h3 { margin: 0 0 10px; font-size: 15px; }
.ent-quick__list { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.ent-quick__item { display: flex; align-items: center; gap: 8px; padding: 8px 14px; background: var(--rh-surface, #fff); border-radius: 6px; border: 1px solid var(--rh-border-light); font-size: 13px; }
.ent-quick__quota { color: var(--rh-text-sub); }
.ent-quick__quota--full { color: var(--el-color-danger, #f56c6c); font-weight: 600; }
.commercial-actions { display: flex; flex-wrap: wrap; gap: 10px; }
</style>
