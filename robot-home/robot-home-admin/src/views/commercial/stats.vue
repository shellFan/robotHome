<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">商业统计看板</span>
      <el-button size="small" @click="load" :loading="loading">刷新</el-button>
    </div>

    <!-- 订单统计 -->
    <el-row :gutter="16" style="margin-top: 12px">
      <el-col v-for="card in orderCards" :key="card.key" :xs="12" :sm="8" :md="4" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label">
            <span class="stat-card__accent" :style="{ background: card.color }"></span>{{ card.label }}
          </div>
          <div class="stat-card__value">{{ orderStats[card.key] || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 收入统计（严格区分REAL与SANDBOX） -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="8">
        <div class="page-card revenue-card revenue-card--real">
          <div class="revenue-card__tag">真实收入 (REAL)</div>
          <div class="revenue-card__value">{{ fmtMoney(revenue.realRevenueCents) }}</div>
          <div class="revenue-card__desc">仅统计真实支付渠道到账金额</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="page-card revenue-card revenue-card--sandbox">
          <div class="revenue-card__tag">沙箱收入 (SANDBOX/MOCK)</div>
          <div class="revenue-card__value">{{ fmtMoney(revenue.sandboxRevenueCents) }}</div>
          <div class="revenue-card__desc">沙箱/模拟渠道金额，不计入真实收入</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="page-card revenue-card">
          <div class="revenue-card__tag">合计（参考）</div>
          <div class="revenue-card__value">{{ fmtMoney(revenue.totalRevenueCents) }}</div>
          <div class="revenue-card__desc">真实+沙箱合计，仅供对账参考</div>
        </div>
      </el-col>
    </el-row>

    <!-- 订阅+退款统计 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-header"><span class="page-title">订阅统计</span></div>
          <el-table :data="subRows" size="small" border>
            <el-table-column prop="label" label="状态" />
            <el-table-column prop="count" label="数量" width="100" align="right" />
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-header"><span class="page-title">退款统计</span></div>
          <el-table :data="refundRows" size="small" border>
            <el-table-column prop="label" label="状态" />
            <el-table-column prop="count" label="数量" width="100" align="right" />
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 渠道收入明细 -->
    <div class="page-card" style="margin-top: 16px">
      <div class="page-header"><span class="page-title">渠道收入明细</span></div>
      <el-table :data="channelRows" size="small" border>
        <el-table-column prop="channel" label="支付渠道" width="200" />
        <el-table-column label="渠道类型" width="150">
          <template #default="{ row }">
            <el-tag :type="row.isSandbox ? 'warning' : 'success'" size="small">
              {{ row.isSandbox ? '沙箱/模拟' : '真实渠道' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" align="right">
          <template #default="{ row }">{{ fmtMoney(row.amountCents) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!channelRows.length" description="暂无已支付订单" :image-size="48" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCommercialStats } from '@/api/commercial'

const loading = ref(false)
const orderStats = ref({})
const revenue = ref({})
const subStats = ref({})
const refundStats = ref({})
const channelRevenue = ref({})

const SANDBOX_CHANNELS = ['MOCK_SANDBOX', 'MOCK', 'SANDBOX', 'DEMO']

const orderCards = [
  { key: 'total', label: '总订单', color: '#409eff' },
  { key: 'pendingPayment', label: '待支付', color: '#e6a23c' },
  { key: 'paid', label: '已支付', color: '#67c23a' },
  { key: 'closed', label: '已关闭', color: '#909399' },
  { key: 'refunded', label: '已退款', color: '#f56c6c' }
]

const subRows = computed(() => [
  { label: '总订阅', count: subStats.value.total || 0 },
  { label: '活跃 (ACTIVE)', count: subStats.value.active || 0 },
  { label: '暂停 (SUSPENDED)', count: subStats.value.suspended || 0 },
  { label: '过期 (EXPIRED)', count: subStats.value.expired || 0 }
])

const refundRows = computed(() => [
  { label: '总退款', count: refundStats.value.total || 0 },
  { label: '申请中 (APPLYING)', count: refundStats.value.applying || 0 },
  { label: '成功 (SUCCESS)', count: refundStats.value.success || 0 },
  { label: '失败 (FAIL)', count: refundStats.value.failed || 0 }
])

const channelRows = computed(() => {
  const rows = []
  for (const [channel, amountCents] of Object.entries(channelRevenue.value || {})) {
    rows.push({ channel, amountCents, isSandbox: SANDBOX_CHANNELS.includes(channel) })
  }
  return rows.sort((a, b) => b.amountCents - a.amountCents)
})

function fmtMoney (cents) {
  if (cents == null) return '¥0.00'
  return '¥' + (cents / 100).toFixed(2)
}

async function load () {
  loading.value = true
  try {
    const data = await getCommercialStats()
    orderStats.value = data.orderStats || {}
    revenue.value = data.revenueStats || {}
    subStats.value = data.subscriptionStats || {}
    refundStats.value = data.refundStats || {}
    channelRevenue.value = data.revenueStats?.channelRevenue || {}
  } catch (e) {
    ElMessage.error('统计加载失败: ' + (e.message || e))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.revenue-card {
  padding: 20px;
  text-align: center;
}
.revenue-card--real {
  border-left: 4px solid #67c23a;
}
.revenue-card--sandbox {
  border-left: 4px solid #e6a23c;
}
.revenue-card__tag {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.revenue-card__value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.revenue-card__desc {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}
</style>