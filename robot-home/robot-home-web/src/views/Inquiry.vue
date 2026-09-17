<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="robot" :to="'/robot/' + robotId">{{ robot.name }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ inquiryTypeLabel }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="inquiry-layout">
        <div class="rh-card inquiry-form">
          <h1 class="inquiry-form__title">{{ inquiryTypeLabel }}</h1>
          <p class="rh-text-sub">提交后销售顾问将尽快与您联系，信息仅用于询价跟进</p>

          <!-- 询价类型选择 -->
          <div class="inquiry-type-tabs">
            <div
              v-for="t in inquiryTypes"
              :key="t.value"
              :class="['inquiry-type-tab', { 'is-active': form.inquiryType === t.value }]"
              @click="form.inquiryType = t.value"
            >
              <span class="inquiry-type-tab__icon">{{ t.icon }}</span>
              <span class="inquiry-type-tab__label">{{ t.label }}</span>
            </div>
          </div>

          <el-form label-position="top" class="inquiry-form__body" @submit.prevent>
            <el-form-item label="联系人" required>
              <el-input v-model="form.name" maxlength="32" placeholder="您的姓名" />
            </el-form-item>
            <el-form-item label="手机号" required>
              <el-input v-model="form.phone" maxlength="11" placeholder="方便联系的手机号" />
            </el-form-item>
            <el-form-item label="所在地区">
              <el-cascader
                v-model="form.regionArr"
                :options="regionOptions"
                :props="{ expandTrigger: 'hover' }"
                placeholder="选择省份/城市"
                clearable
                style="width: 100%"
                @change="onRegionChange"
              />
            </el-form-item>
            <el-form-item label="客户类型">
              <el-radio-group v-model="form.customerType">
                <el-radio :value="1">个人</el-radio>
                <el-radio :value="2">企业</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.customerType === 2" label="企业名称">
              <el-input v-model="form.companyName" maxlength="128" placeholder="企业全称" />
            </el-form-item>
            <el-form-item label="采购数量" required>
              <el-input-number v-model="form.quantity" :min="1" :max="9999" />
            </el-form-item>
            <el-form-item label="预算">
              <el-input v-model="form.budget" maxlength="64" placeholder="如：10-20万 / 面议" />
            </el-form-item>
            <!-- Phase7 V2: 使用场景 -->
            <el-form-item v-if="form.inquiryType === 'PURCHASE' || form.inquiryType === 'LEASE'" label="使用场景">
              <el-select v-model="form.procurementScene" placeholder="请选择使用场景" clearable style="width: 100%">
                <el-option label="工业制造" value="INDUSTRIAL" />
                <el-option label="物流仓储" value="LOGISTICS" />
                <el-option label="医疗健康" value="MEDICAL" />
                <el-option label="教育培训" value="EDUCATION" />
                <el-option label="服务行业" value="SERVICE" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <!-- Phase7 V2: 采购时间 -->
            <el-form-item v-if="form.inquiryType === 'PURCHASE'" label="采购时间">
              <el-input v-model="form.purchaseTime" maxlength="64" placeholder="预计采购时间，如：1个月内" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="使用场景、交付时间等补充说明"
              />
            </el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="submit">
              提交{{ inquiryTypeLabel }}
            </el-button>
          </el-form>
        </div>

        <aside class="rh-card inquiry-robot" v-if="robot">
          <img :src="imageOf(robot.coverImage)" :alt="robot.name" loading="lazy" />
          <div class="inquiry-robot__name">{{ robot.name }}</div>
          <div class="inquiry-robot__price rh-price">{{ formatPrice(robot.guidePrice) }}</div>
          <div class="rh-text-light">指导价仅供参考，询价可获更优方案</div>
          <router-link :to="'/robot/' + robotId" class="inquiry-robot__link">查看产品详情 ›</router-link>
        </aside>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { inquiryApi, robotApi } from '@/api'
import { useUserStore } from '@/store/user'
import { formatPrice, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'
import { regionOptions } from '@/utils/region-data'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const robotId = computed(() => Number(route.params.robotId))
const robot = ref(null)
const submitting = ref(false)

// Phase7 V2: 询价类型
const inquiryTypes = [
  { value: 'PRICE', label: '获取底价', icon: '💰' },
  { value: 'PURCHASE', label: '我要采购', icon: '🛒' },
  { value: 'LEASE', label: '租赁咨询', icon: '📋' },
  { value: 'COOPERATE', label: '合作洽谈', icon: '🤝' }
]

const inquiryTypeLabel = computed(() => {
  const t = inquiryTypes.find(i => i.value === form.inquiryType)
  return t ? t.label : '获取报价'
})

const form = reactive({
  name: '',
  phone: '',
  region: '',
  regionArr: [],
  customerType: 1,
  companyName: '',
  quantity: 1,
  budget: '',
  remark: '',
  inquiryType: route.query.type || 'PRICE',
  procurementScene: '',
  purchaseTime: ''
})

function onRegionChange (val) {
  form.region = val && val.length ? val.join(' ') : ''
}

async function loadRobot () {
  try {
    const data = await robotApi.detail(robotId.value)
    robot.value = data && data.robot
    if (robot.value) {
      setPageMeta({
        title: '询价 ' + robot.value.name + ' - 机器人之家',
        description: '向机器人之家提交 ' + robot.value.name + ' 的询价需求。'
      })
    }
  } catch (e) {
    robot.value = null
  }
}

async function submit () {
  if (!form.name.trim()) {
    ElMessage.warning('请填写联系人')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请填写正确的手机号')
    return
  }
  if (!form.quantity || form.quantity < 1) {
    ElMessage.warning('请填写采购数量')
    return
  }
  submitting.value = true
  try {
    await inquiryApi.submit({
      robotId: robotId.value,
      name: form.name.trim(),
      phone: form.phone.trim(),
      region: form.region || undefined,
      customerType: form.customerType,
      companyName: form.customerType === 2 ? form.companyName : undefined,
      quantity: form.quantity,
      budget: form.budget || undefined,
      remark: form.remark || undefined,
      inquiryType: form.inquiryType,
      procurementScene: form.procurementScene || undefined,
      purchaseTime: form.purchaseTime || undefined
    })
    ElMessage.success('询价已提交，销售顾问将尽快与您联系')
    if (userStore.isLogin) {
      router.push('/user/inquiries')
    } else {
      router.push('/robot/' + robotId.value)
    }
  } catch (e) {
    const msg = e && e.message
    if (msg && (msg.includes('频繁') || msg.includes('429'))) {
      ElMessage.warning('询价过于频繁，请1分钟后再试')
    }
    // 其他错误已由全局拦截器处理
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  setPageMeta({ title: '获取报价 - 机器人之家' })
  if (userStore.profile) {
    form.name = userStore.profile.nickname || ''
    form.phone = (userStore.profile.phone || '').replace(/\*/g, '') || form.phone
  }
  await loadRobot()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.inquiry-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
  align-items: start;
}

.inquiry-form__title {
  margin: 0 0 6px;
  font-size: 22px;
}

.inquiry-form__body {
  margin-top: 20px;
  max-width: 520px;
}

.inquiry-robot img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: var(--rh-radius);
  background: var(--rh-surface-sub);
}

.inquiry-robot__name {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 6px;
}

.inquiry-robot__price {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 8px;
}

.inquiry-robot__link {
  display: inline-block;
  margin-top: 14px;
  color: var(--rh-primary);
  font-size: 13px;
}

.inquiry-form__agreement {
  margin-top: 8px;
  font-size: 12px;
  color: var(--rh-text-light);
}

/* Phase7 V2: 询价类型选择 */
.inquiry-type-tabs {
  display: flex;
  gap: 10px;
  margin: 16px 0;
  flex-wrap: wrap;
}

.inquiry-type-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 2px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.inquiry-type-tab:hover {
  border-color: var(--rh-primary);
}

.inquiry-type-tab.is-active {
  border-color: var(--rh-primary);
  background: rgba(var(--rh-primary-rgb, 64, 158, 255), 0.05);
  color: var(--rh-primary);
  font-weight: 500;
}

.inquiry-type-tab__icon {
  font-size: 18px;
}

@media (max-width: 768px) {
  .inquiry-layout {
    grid-template-columns: 1fr;
  }
  .inquiry-robot {
    order: -1;
  }
  .inquiry-robot img {
    height: 140px;
  }
}
</style>
