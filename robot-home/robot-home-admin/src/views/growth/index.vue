<template>
  <div class="page-container">
    <!-- 今日概览 -->
    <el-row :gutter="16">
      <el-col v-for="card in todayCards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="3" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label">
            <span class="stat-card__accent" :style="{ background: card.color }"></span>{{ card.label }}
          </div>
          <div class="stat-card__value">{{ fmt(dashboard.today && dashboard.today[card.key]) }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <div class="page-card dashboard__chart-card">
      <div class="page-header">
        <span class="page-title">增长趋势</span>
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :value="7">7 天</el-radio-button>
          <el-radio-button :value="30">30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="chartRef" class="dashboard__chart"></div>
    </div>

    <!-- 转化漏斗 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-header"><span class="page-title">转化漏斗</span></div>
          <div class="funnel">
            <div v-for="item in funnelItems" :key="item.label" class="funnel__row">
              <span class="funnel__label">{{ item.label }}</span>
              <div class="funnel__bar-wrap">
                <div class="funnel__bar" :style="{ width: item.pct + '%', background: item.color }"></div>
              </div>
              <span class="funnel__value">{{ item.value }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- Top列表 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-header"><span class="page-title">热门排行</span></div>
          <el-tabs v-model="topTab">
            <el-tab-pane label="热门机器人" name="robots">
              <div v-for="(r, i) in dashboard.topRobots || []" :key="r.id" class="top-item">
                <span class="top-item__rank">{{ i + 1 }}</span>
                <span class="top-item__name">{{ r.name }}</span>
                <span class="top-item__count">{{ r.count }} 次</span>
              </div>
              <el-empty v-if="!(dashboard.topRobots && dashboard.topRobots.length)" description="暂无数据" :image-size="48" />
            </el-tab-pane>
            <el-tab-pane label="热门品牌" name="brands">
              <div v-for="(r, i) in dashboard.topBrands || []" :key="r.id" class="top-item">
                <span class="top-item__rank">{{ i + 1 }}</span>
                <span class="top-item__name">{{ r.name }}</span>
                <span class="top-item__count">{{ r.count }} 次</span>
              </div>
              <el-empty v-if="!(dashboard.topBrands && dashboard.topBrands.length)" description="暂无数据" :image-size="48" />
            </el-tab-pane>
            <el-tab-pane label="搜索关键词" name="keywords">
              <div v-for="(r, i) in dashboard.topSearchKeywords || []" :key="i" class="top-item">
                <span class="top-item__rank">{{ i + 1 }}</span>
                <span class="top-item__name">{{ r.name }}</span>
                <span class="top-item__count">{{ r.count }} 次</span>
              </div>
              <el-empty v-if="!(dashboard.topSearchKeywords && dashboard.topSearchKeywords.length)" description="暂无数据" :image-size="48" />
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, reactive, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getGrowthDashboard } from '@/api/growth'

const days = ref(7)
const chartRef = ref(null)
let chart = null
const topTab = ref('robots')

const dashboard = reactive({
  today: null,
  trend7d: [],
  trend30d: [],
  funnel: null,
  topRobots: [],
  topBrands: [],
  topSearchKeywords: []
})

const todayCards = [
  { key: 'dau', label: 'DAU', color: '#1668dc' },
  { key: 'newUsers', label: '新增用户', color: '#2ba471' },
  { key: 'robotViews', label: '机器人浏览', color: '#d9822b' },
  { key: 'searches', label: '搜索次数', color: '#7a5af8' },
  { key: 'favorites', label: '收藏', color: '#e34d59' },
  { key: 'compares', label: '对比', color: '#1668dc' },
  { key: 'questions', label: '提问', color: '#2ba471' },
  { key: 'answers', label: '回答', color: '#d9822b' },
  { key: 'posts', label: '发帖', color: '#7a5af8' },
  { key: 'reviews', label: '评测', color: '#e34d59' },
  { key: 'inquiries', label: '询价', color: '#d9822b' }
]

const funnelItems = computed(() => {
  const f = dashboard.funnel || {}
  const views = f.views || 0
  return [
    { label: '浏览', value: views, pct: 100, color: '#1668dc' },
    { label: '互动(对比/收藏)', value: f.engages || 0, pct: views ? Math.round(((f.engages || 0) / views) * 100) : 0, color: '#2ba471' },
    { label: '选型', value: f.selections || 0, pct: views ? Math.round(((f.selections || 0) / views) * 100) : 0, color: '#d9822b' },
    { label: '询价/采购', value: f.inquiries || 0, pct: views ? Math.round(((f.inquiries || 0) / views) * 100) : 0, color: '#e34d59' }
  ]
})

function fmt(v) {
  if (v === null || v === undefined) return '0'
  return Number(v).toLocaleString('zh-CN')
}

async function load() {
  try {
    const data = await getGrowthDashboard(days.value)
    if (!data) return
    dashboard.today = data.today || null
    dashboard.trend7d = data.trend7d || []
    dashboard.trend30d = data.trend30d || []
    dashboard.funnel = data.funnel || null
    dashboard.topRobots = data.topRobots || []
    dashboard.topBrands = data.topBrands || []
    dashboard.topSearchKeywords = data.topSearchKeywords || []
    await nextTick()
    renderChart(days.value === 30 ? dashboard.trend30d : dashboard.trend7d)
  } catch (e) {
    console.error('Failed to load growth dashboard', e)
  }
}

function renderChart(list) {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const dates = list.map((item) => item.date)
  const series = [
    { name: 'DAU', key: 'dau', color: '#1668dc' },
    { name: '新增用户', key: 'newUsers', color: '#2ba471' },
    { name: '机器人浏览', key: 'robotViews', color: '#d9822b' },
    { name: '搜索', key: 'searches', color: '#7a5af8' }
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

onMounted(() => {
  window.addEventListener('resize', handleResize)
  load()
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

.funnel {
  padding: 8px 0;
}

.funnel__row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.funnel__label {
  width: 120px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}

.funnel__bar-wrap {
  flex: 1;
  height: 20px;
  background: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
  margin: 0 12px;
}

.funnel__bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.funnel__value {
  width: 50px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.top-item {
  display: flex;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f0f1f3;
}

.top-item:last-child {
  border-bottom: none;
}

.top-item__rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f5f7fa;
  text-align: center;
  line-height: 24px;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-right: 10px;
  flex-shrink: 0;
}

.top-item__name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-item__count {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
  flex-shrink: 0;
}
</style>