<template>
  <section class="orders-page">
    <div class="rh-card orders-card">
      <div class="orders-heading">
        <div>
          <h1>商业订单</h1>
          <p>管理您的套餐购买与支付记录</p>
        </div>
        <el-button type="primary" @click="$router.push('/plans')">购买套餐</el-button>
      </div>

      <div v-if="loading" class="rh-empty">正在加载…</div>
      <el-alert v-else-if="error" type="error" :closable="false" :title="error" />
      <el-empty v-else-if="!orders.length" description="暂无订单" :image-size="60" />

      <el-table v-else :data="orders" size="small">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="planName" label="套餐" min-width="120" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ formatAmount(row.amountCents) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_PAYMENT'">
              <el-button size="small" type="primary" @click="openPay(row)">支付</el-button>
              <el-button size="small" @click="cancel(row)">取消</el-button>
            </template>
            <span v-else class="orders-done">--</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 支付对话框 -->
    <el-dialog v-model="payDialog.visible" title="订单支付" width="460px">
      <div v-if="payDialog.order" class="pay-summary">
        <div class="pay-row"><span>订单号</span><b>{{ payDialog.order.orderNo }}</b></div>
        <div class="pay-row"><span>套餐</span><b>{{ payDialog.order.planName }}</b></div>
        <div class="pay-row"><span>应付金额</span><b class="pay-amount">¥{{ formatAmount(payDialog.order.amountCents) }}</b></div>
      </div>
      <el-divider />
      <div class="pay-channels">
        <p class="pay-channels__title">选择支付渠道</p>
        <el-radio-group v-model="payDialog.channel">
          <el-radio v-for="ch in channels" :key="ch.code" :label="ch.code" class="pay-radio">
            {{ ch.name }}<span v-if="ch.sandbox" class="pay-sandbox">沙箱</span>
          </el-radio>
        </el-radio-group>
        <p v-if="sandboxHint" class="pay-hint">{{ sandboxHint }}</p>
      </div>
      <template #footer>
        <el-button @click="payDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="payDialog.paying" @click="doPay">确认支付</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { commercialApi } from '@/api'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const orders = ref([])
const channels = ref([])
const sandboxHint = ref('')

const payDialog = reactive({ visible: false, order: null, channel: '', paying: false })

function orderStatusText (status) {
  return { PENDING_PAYMENT: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款', CLOSED: '已关闭' }[status] || status
}
function orderStatusType (status) {
  return { PAID: 'success', PENDING_PAYMENT: 'warning', CANCELLED: 'info', REFUNDED: 'danger', CLOSED: 'info' }[status] || 'info'
}
function formatAmount (cents) { return ((cents || 0) / 100).toFixed(2) }
function formatTime (val) {
  if (!val) return '-'
  return String(val).replace('T', ' ').substring(0, 19)
}

async function load () {
  loading.value = true
  error.value = ''
  try {
    const [list, chs] = await Promise.all([
      commercialApi.myOrders({ page: 1, size: 50 }),
      commercialApi.paymentChannels().catch(() => [])
    ])
    orders.value = Array.isArray(list) ? list : (list.records || [])
    channels.value = (chs || []).filter(c => c.available)
    if (route.query.planId) await preOrder(route.query.planId)
  } catch (e) {
    error.value = '订单加载失败'
  } finally {
    loading.value = false
  }
}

async function preOrder (planId) {
  try {
    await ElMessageBox.confirm('检测到待购套餐，是否立即创建订单？', '套餐购买')
    const order = await commercialApi.createOrder({ planId, idemKey: `web-${planId}-${Date.now()}` })
    await load()
    if (order && order.status === 'PENDING_PAYMENT') openPay(order)
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('创建订单失败')
  }
}

function openPay (order) {
  payDialog.order = order
  payDialog.channel = channels.value[0] ? channels.value[0].code : ''
  sandboxHint.value = channels.value.some(c => c.code === 'MOCK_SANDBOX' && c.sandbox)
    ? '沙箱渠道仅用于测试，不会产生真实扣款。'
    : ''
  payDialog.visible = true
}

async function doPay () {
  if (!payDialog.channel) { ElMessage.warning('请选择支付渠道'); return }
  payDialog.paying = true
  try {
    await commercialApi.payOrder(payDialog.order.id, payDialog.channel)
    if (payDialog.channel === 'MOCK_SANDBOX') {
      await commercialApi.sandboxConfirm(payDialog.order.id)
      ElMessage.success('沙箱支付成功，权益已开通')
    } else {
      ElMessage.success('支付请求已提交，请按渠道指引完成付款')
    }
    payDialog.visible = false
    await load()
  } catch (e) {
    ElMessage.error('支付失败，请重试')
  } finally {
    payDialog.paying = false
  }
}

async function cancel (order) {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '取消订单')
    await commercialApi.cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('取消失败')
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.orders-card { padding: 24px; }
.orders-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.orders-heading h1 { margin: 0 0 6px; }
.orders-heading p { color: var(--rh-text-sub); margin: 0; }
.orders-done { color: var(--rh-text-sub); }
.pay-summary { margin-bottom: 4px; }
.pay-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; }
.pay-row span { color: var(--rh-text-sub); }
.pay-amount { color: var(--el-color-danger); font-size: 16px; }
.pay-channels__title { color: var(--rh-text-sub); margin: 0 0 10px; font-size: 14px; }
.pay-radio { display: block; margin: 8px 0; }
.pay-sandbox { margin-left: 8px; font-size: 12px; color: var(--el-color-warning); }
.pay-hint { color: var(--el-color-warning); font-size: 12px; margin: 10px 0 0; }
</style>