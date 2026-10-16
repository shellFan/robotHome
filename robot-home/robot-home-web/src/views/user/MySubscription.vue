<template>
  <section class="subscription-page">
    <div class="rh-card sub-card">
      <div class="sub-heading">
        <div>
          <h1>我的订阅</h1>
          <p>查看当前套餐与权益使用情况</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>
      <div v-if="loading" class="rh-empty">正在加载…</div>
      <el-alert v-else-if="error" type="error" :closable="false" :title="error" />
      <template v-else>
        <section v-if="subscription" class="sub-info">
          <div class="sub-info__main">
            <h2>{{ subscription.planName }}</h2>
            <el-tag :type="statusType(subscription.status)">{{ statusText(subscription.status) }}</el-tag>
          </div>
          <div class="sub-info__meta">
            <span v-if="subscription.startTime">开始：{{ formatDate(subscription.startTime) }}</span>
            <span v-if="subscription.endTime">到期：{{ formatDate(subscription.endTime) }}</span>
            <span>自动续费：{{ subscription.autoRenew === 1 ? '已开启' : '未开启' }}</span>
          </div>
          <div class="sub-info__actions">
            <el-button type="primary" @click="$router.push('/plans')">升级套餐</el-button>
            <el-button v-if="subscription.autoRenew === 1" @click="cancelRenew">关闭自动续费</el-button>
          </div>
        </section>
        <el-empty v-else description="暂无订阅记录，请先认领企业" :image-size="60" />

        <section v-if="entitlements.length" class="ent-section">
          <h2>权益使用</h2>
          <el-table :data="entitlements" size="small">
            <el-table-column prop="entCode" label="权益" width="160">
              <template #default="{ row }">{{ entLabel(row.entCode) }}</template>
            </el-table-column>
            <el-table-column label="配额" width="140">
              <template #default="{ row }">
                <span v-if="row.quotaTotal === 0">不限量</span>
                <span v-else>{{ row.quotaUsed }} / {{ row.quotaTotal }}</span>
              </template>
            </el-table-column>
            <el-table-column label="使用率" min-width="160">
              <template #default="{ row }">
                <el-progress
                  v-if="row.quotaTotal > 0"
                  :percentage="Math.round(row.quotaUsed / row.quotaTotal * 100)"
                  :status="row.quotaUsed >= row.quotaTotal ? 'exception' : ''"
                />
                <span v-else class="ent-unlimited">不限</span>
              </template>
            </el-table-column>
            <el-table-column prop="periodType" label="周期" width="100" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </template>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { commercialApi } from '@/api'

const loading = ref(true)
const error = ref('')
const subscription = ref(null)
const entitlements = ref([])

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

function statusText (status) {
  return { FREE: '免费版', ACTIVE: '使用中', EXPIRING: '即将到期', EXPIRED: '已过期', SUSPENDED: '已暂停', CANCELLED: '已取消' }[status] || status
}
function statusType (status) {
  return { ACTIVE: 'success', FREE: 'info', EXPIRING: 'warning', EXPIRED: 'danger', SUSPENDED: 'danger' }[status] || 'info'
}
function formatDate (val) {
  if (!val) return '-'
  return String(val).replace('T', ' ').substring(0, 10)
}

async function load () {
  loading.value = true
  error.value = ''
  try {
    const [sub, ents] = await Promise.all([
      commercialApi.mySubscription(),
      commercialApi.myEntitlements()
    ])
    subscription.value = sub || null
    entitlements.value = ents || []
  } catch (e) {
    error.value = '订阅信息加载失败'
  } finally {
    loading.value = false
  }
}

async function cancelRenew () {
  try {
    await ElMessageBox.confirm('关闭后到期将不再自动续费。', '关闭自动续费')
    await commercialApi.cancelAutoRenew()
    ElMessage.success('已关闭自动续费')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('操作失败，请重试')
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.sub-card { padding: 24px; }
.sub-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.sub-heading h1 { margin: 0 0 6px; }
.sub-heading p { color: var(--rh-text-sub); margin: 0; }
.sub-info { padding: 20px; background: var(--rh-surface-sub); border-radius: 10px; margin-bottom: 24px; }
.sub-info__main { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.sub-info__main h2 { margin: 0; font-size: 22px; }
.sub-info__meta { display: flex; gap: 24px; color: var(--rh-text-sub); font-size: 14px; margin-bottom: 16px; }
.sub-info__actions { display: flex; gap: 10px; }
.ent-section h2 { margin: 0 0 14px; font-size: 17px; }
.ent-unlimited { color: var(--rh-text-sub); font-size: 13px; }
</style>