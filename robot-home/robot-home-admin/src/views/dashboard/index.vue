<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="4" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label">
            <span class="stat-card__accent" :style="{ background: card.color }"></span>{{ card.label }}
          </div>
          <div class="stat-card__value">{{ formatNumber(stats[card.key]) }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card dashboard__chart-card">
      <div class="page-header">
        <span class="page-title">近 {{ days }} 天趋势</span>
        <el-radio-group v-model="days" size="small" @change="loadTrend">
          <el-radio-button :value="7">7 天</el-radio-button>
          <el-radio-button :value="14">14 天</el-radio-button>
          <el-radio-button :value="30">30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="chartRef" class="dashboard__chart"></div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, reactive, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStats, getTrend } from '@/api/dashboard'

const days = ref(7)
const chartRef = ref(null)
let chart = null

const stats = reactive({
  userCount: 0,
  todayNewUserCount: 0,
  robotCount: 0,
  brandCount: 0,
  companyCount: 0,
  articleCount: 0,
  videoCount: 0,
  tutorialCount: 0,
  postCount: 0,
  inquiryCount: 0,
  pendingInquiryCount: 0,
  commentCount: 0,
  todayPv: 0,
  todayUv: 0
})

const cards = [
  { key: 'userCount', label: '用户数', color: '#1668dc' },
  { key: 'todayNewUserCount', label: '今日新增', color: '#2ba471' },
  { key: 'robotCount', label: '机器人数', color: '#1668dc' },
  { key: 'brandCount', label: '品牌数', color: '#d9822b' },
  { key: 'companyCount', label: '企业数', color: '#7a5af8' },
  { key: 'articleCount', label: '文章数', color: '#1668dc' },
  { key: 'videoCount', label: '视频数', color: '#d9822b' },
  { key: 'tutorialCount', label: '教程数', color: '#2ba471' },
  { key: 'postCount', label: '帖子数', color: '#7a5af8' },
  { key: 'inquiryCount', label: '询价数', color: '#1668dc' },
  { key: 'pendingInquiryCount', label: '待处理询价', color: '#d93026' },
  { key: 'commentCount', label: '评论数', color: '#2ba471' },
  { key: 'todayPv', label: '今日 PV', color: '#1668dc' },
  { key: 'todayUv', label: '今日 UV', color: '#7a5af8' }
]

function formatNumber(value) {
  if (value === null || value === undefined) return '0'
  return Number(value).toLocaleString('zh-CN')
}

async function loadStats() {
  const data = await getStats()
  if (!data) return
  Object.keys(stats).forEach((key) => {
    stats[key] = data[key] ?? 0
  })
}

async function loadTrend() {
  const data = await getTrend(days.value)
  const list = Array.isArray(data) ? data : []
  renderChart(list)
}

function renderChart(list) {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const dates = list.map((item) => item.date)
  const series = [
    { name: 'PV', key: 'pv', color: '#1668dc' },
    { name: 'UV', key: 'uv', color: '#2ba471' },
    { name: '新增用户', key: 'newUsers', color: '#d9822b' },
    { name: '询价', key: 'inquiries', color: '#7a5af8' }
  ]
  chart.setOption(
    {
      tooltip: { trigger: 'axis' },
      legend: { data: series.map((s) => s.name), bottom: 0 },
      grid: { left: 48, right: 24, top: 24, bottom: 48 },
      xAxis: { type: 'category', data: dates, boundaryGap: false, axisLine: { lineStyle: { color: '#e6e8eb' } } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f1f3' } } },
      series: series.map((s) => ({
        name: s.name,
        type: 'line',
        smooth: true,
        showSymbol: true,
        symbolSize: 4,
        itemStyle: { color: s.color },
        lineStyle: { width: 2 },
        data: list.map((item) => item[s.key] ?? 0)
      }))
    },
    true
  )
  chart.resize()
}

function handleResize() {
  if (chart) chart.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadStats()
  await nextTick()
  await loadTrend()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.dashboard__col {
  margin-bottom: 16px;
}

.dashboard__chart-card {
  margin-top: 0;
}

.dashboard__chart {
  width: 100%;
  height: 340px;
}
</style>
