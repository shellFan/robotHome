<template>
  <MainLayout>
    <section class="rh-container plans-page">
      <div class="plans-header">
        <h1>企业套餐</h1>
        <p>选择适合你企业的商业套餐，解锁更多平台能力</p>
      </div>
      <div v-if="loading" class="rh-empty">正在加载套餐…</div>
      <el-alert v-else-if="error" type="error" :closable="false" :title="error" />
      <div v-else class="plans-grid">
        <div v-for="plan in plans" :key="plan.id" class="plan-card rh-card" :class="{ 'plan-card--featured': plan.level > 0 }">
          <div class="plan-card__head">
            <h2>{{ plan.name }}</h2>
            <el-tag v-if="plan.level === 0" type="info" size="small">免费</el-tag>
            <el-tag v-else-if="plan.level >= 30" type="warning" size="small">旗舰</el-tag>
            <el-tag v-else type="success" size="small">推荐</el-tag>
          </div>
          <p class="plan-card__desc">{{ plan.description }}</p>
          <div class="plan-card__price">
            <span class="price-num">¥{{ formatPrice(plan.displayPrice) }}</span>
            <span class="price-unit">/ {{ plan.durationDays >= 365 ? '年' : plan.durationDays + '天' }}</span>
          </div>
          <ul class="plan-card__features">
            <li v-for="feat in planFeatures(plan)" :key="feat">{{ feat }}</li>
          </ul>
          <el-button
            v-if="plan.saleable === 1"
            type="primary"
            class="plan-card__btn"
            @click="goBuy(plan)"
          >{{ plan.level === 0 ? '当前免费使用' : '立即开通' }}</el-button>
          <el-button v-else type="info" class="plan-card__btn" disabled>即将上线</el-button>
        </div>
      </div>
    </section>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { commercialApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const error = ref('')
const plans = ref([])

const ENT_LABELS = {
  LEAD_VIEW: '商机查看',
  LEAD_RESPONSE: '商机响应',
  PRODUCT_MANAGEMENT: '产品管理',
  PRODUCT_FEATURED: '精选推荐位',
  AD_CAMPAIGN: '广告投放',
  ANALYTICS_ADVANCED: '高级数据分析',
  COMPANY_PROFILE_ENHANCED: '企业主页增强'
}

function formatPrice (val) {
  if (!val) return '0'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function planFeatures (plan) {
  const feats = []
  if (plan.entitlementConfig) {
    try {
      const config = JSON.parse(plan.entitlementConfig)
      for (const item of config) {
        const label = ENT_LABELS[item.code] || item.code
        const quota = item.quotaTotal === 0 ? '不限' : item.quotaTotal + '次/月'
        feats.push(`${label}：${quota}`)
      }
    } catch (e) { /* ignore */ }
  }
  if (feats.length === 0) feats.push('基础企业能力')
  return feats
}

async function goBuy (plan) {
  if (plan.level === 0) {
    router.push('/user/enterprise')
    return
  }
  if (!userStore.isLogin) {
    ElMessage.info('请先登录')
    router.push({ name: 'login', query: { redirect: '/plans' } })
    return
  }
  router.push({ path: '/user/orders', query: { planId: plan.id } })
}

async function load () {
  loading.value = true
  error.value = ''
  try {
    const data = await commercialApi.plans()
    plans.value = (data || []).filter(p => p.status === 'ON_SHELF')
  } catch (e) {
    error.value = '套餐加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.plans-page { padding: 32px 0; }
.plans-header { text-align: center; margin-bottom: 36px; }
.plans-header h1 { margin: 0 0 8px; font-size: 28px; }
.plans-header p { color: var(--rh-text-sub); margin: 0; }
.plans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  max-width: 1100px;
  margin: 0 auto;
}
.plan-card { padding: 28px 24px; display: flex; flex-direction: column; }
.plan-card--featured { border: 2px solid var(--rh-primary); }
.plan-card__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.plan-card__head h2 { margin: 0; font-size: 20px; }
.plan-card__desc { color: var(--rh-text-sub); font-size: 13px; min-height: 36px; }
.plan-card__price { margin: 16px 0; }
.price-num { font-size: 32px; font-weight: 700; color: var(--rh-primary); }
.price-unit { color: var(--rh-text-sub); font-size: 14px; margin-left: 4px; }
.plan-card__features { list-style: none; padding: 0; margin: 0 0 24px; flex: 1; }
.plan-card__features li {
  padding: 6px 0;
  font-size: 14px;
  color: var(--rh-text);
  border-bottom: 1px dashed var(--rh-border-light);
}
.plan-card__features li::before { content: '✓ '; color: var(--rh-primary); font-weight: 700; }
.plan-card__btn { width: 100%; }
</style>