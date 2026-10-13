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

    <!-- Phase11: 采集器健康 & 数据质量概览 -->
    <el-row :gutter="16" style="margin-top: 0">
      <el-col :xs="24" :sm="12" :md="8" class="dashboard__col">
        <div class="stat-card" style="cursor: pointer" @click="$router.push('/crawler/source')">
          <div class="stat-card__label">
            <span class="stat-card__accent" :style="{ background: crawlerHealth.collectorAvailable ? '#2ba471' : '#d93026' }"></span>采集器状态
          </div>
          <div class="stat-card__value">
            <el-tag :type="crawlerHealth.collectorAvailable ? 'success' : 'danger'" size="small">
              {{ crawlerHealth.collectorAvailable ? '在线' : '离线' }}
            </el-tag>
            <span v-if="crawlerHealth.collectorAvailable && crawlerHealth.httpStatus" style="margin-left: 8px; font-size: 12px; color: #999">
              HTTP {{ crawlerHealth.httpStatus }}
            </span>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" class="dashboard__col">
        <div class="stat-card" style="cursor: pointer" @click="$router.push('/quality')">
          <div class="stat-card__label">
            <span class="stat-card__accent" :style="{ background: qualitySummary.pendingIssues > 0 ? '#d93026' : '#2ba471' }"></span>数据质量
          </div>
          <div class="stat-card__value">
            <span>已评 {{ qualitySummary.totalScored }}</span>
            <el-tag v-if="qualitySummary.lowScoreCount > 0" type="warning" size="small" style="margin-left: 8px">
              低分 {{ qualitySummary.lowScoreCount }}
            </el-tag>
            <el-tag v-if="qualitySummary.pendingIssues > 0" type="danger" size="small" style="margin-left: 4px">
              待处理 {{ qualitySummary.pendingIssues }}
            </el-tag>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Phase12: 数据覆盖率 + Source健康度 + Freshness -->
    <el-row :gutter="16" style="margin-top: 0">
      <el-col :xs="24" :md="8" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label"><span class="stat-card__accent" style="background:#1668dc"></span>Robot覆盖率</div>
          <div class="stat-card__value" style="font-size:14px;line-height:1.8">
            <div>封面图 <b>{{ dq.robotCoverImageRate || 0 }}%</b> ({{ dq.robotWithCoverImage || 0 }}/{{ dq.robotTotal || 0 }})</div>
            <div>图集 <b>{{ dq.robotGalleryRate || 0 }}%</b> · 参数 <b>{{ dq.robotParamRate || 0 }}%</b></div>
            <div>来源URL <b>{{ dq.robotSourceUrlRate || 0 }}%</b> · 品牌 <b>{{ dq.robotBrandRate || 0 }}%</b></div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label"><span class="stat-card__accent" style="background:#2ba471"></span>Source健康度</div>
          <div class="stat-card__value" style="font-size:14px;line-height:1.8">
            <el-tag type="success" size="small">健康 {{ dq.sourceHealthy || 0 }}</el-tag>
            <el-tag type="warning" size="small" style="margin-left:4px">降级 {{ dq.sourceDegraded || 0 }}</el-tag>
            <el-tag type="danger" size="small" style="margin-left:4px">失败 {{ dq.sourceFailed || 0 }}</el-tag>
            <el-tag type="info" size="small" style="margin-left:4px">未知 {{ dq.sourceUnknown || 0 }}</el-tag>
            <div style="margin-top:4px;font-size:12px;color:#999">
              OFFICIAL {{ dq.sourceOfficial || 0 }} · TRUSTED {{ dq.sourceTrusted || 0 }} · NORMAL {{ dq.sourceNormal || 0 }}
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label">
            <span class="stat-card__accent" style="background:#d9822b"></span>Freshness新鲜度
            <el-button type="primary" size="mini" style="margin-left:8px" @click="doRefreshFreshness">刷新</el-button>
          </div>
          <div class="stat-card__value" style="font-size:14px;line-height:1.8">
            <el-tag type="success" size="small">FRESH {{ dq.robotFresh || 0 }}</el-tag>
            <el-tag type="warning" size="small" style="margin-left:4px">AGING {{ dq.robotAging || 0 }}</el-tag>
            <el-tag type="danger" size="small" style="margin-left:4px">STALE {{ dq.robotStale || 0 }}</el-tag>
            <div style="margin-top:4px;font-size:12px;color:#999">
              Brand官网 {{ dq.brandWebsiteRate || 0 }}% · Article来源 {{ dq.articleSourceUrlRate || 0 }}%
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Phase12 P0-5: Coverage下钻 -->
    <el-row :gutter="16" style="margin-top: 0">
      <el-col :xs="24" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label">
            <span class="stat-card__accent" style="background:#7a5af8"></span>Coverage下钻
            <el-radio-group v-model="drillDimension" size="mini" style="margin-left:12px" @change="loadDrillDown">
              <el-radio-button value="dataSource">来源</el-radio-button>
              <el-radio-button value="brand">品牌</el-radio-button>
              <el-radio-button value="category">分类</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="drillItems" size="small" stripe style="width:100%;margin-top:8px" max-height="320">
            <el-table-column prop="name" label="维度" min-width="120" />
            <el-table-column prop="total" label="总数" width="70" align="center" />
            <el-table-column label="封面图" width="90" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.coverImageRate >= 80 ? '#2ba471' : row.coverImageRate >= 50 ? '#d9822b' : '#d93026' }">
                  {{ row.coverImageRate || 0 }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="图集" width="80" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.galleryRate >= 80 ? '#2ba471' : row.galleryRate >= 50 ? '#d9822b' : '#d93026' }">
                  {{ row.galleryRate || 0 }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="参数" width="80" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.paramRate >= 80 ? '#2ba471' : row.paramRate >= 50 ? '#d9822b' : '#d93026' }">
                  {{ row.paramRate || 0 }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="来源URL" width="90" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.sourceUrlRate >= 80 ? '#2ba471' : row.sourceUrlRate >= 50 ? '#d9822b' : '#d93026' }">
                  {{ row.sourceUrlRate || 0 }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- Phase12 P0-6: Scorecard BEFORE/AFTER -->
    <el-row :gutter="16" style="margin-top: 0">
      <el-col :xs="24" :md="12" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label">
            <span class="stat-card__accent" style="background:#1668dc"></span>BEFORE Scorecard
            <el-button type="primary" size="mini" style="margin-left:8px" @click="captureScorecardAction('BEFORE')">捕获</el-button>
          </div>
          <div v-if="scorecardBefore" class="stat-card__value" style="font-size:13px;line-height:1.8">
            <div>综合评分: <b style="font-size:18px">{{ scorecardBefore.overallScore || 0 }}</b>/100</div>
            <div>Robot {{ scorecardBefore.robotTotal || 0 }} · 封面图{{ scorecardBefore.robotCoverImageRate || 0 }}% · 参数{{ scorecardBefore.robotParamRate || 0 }}%</div>
            <div>Source {{ scorecardBefore.sourceTotal || 0 }} · 健康{{ scorecardBefore.sourceHealthy || 0 }} · FRESH{{ scorecardBefore.robotFresh || 0 }}</div>
            <div style="font-size:11px;color:#999">{{ scorecardBefore.snapshotTime }}</div>
          </div>
          <div v-else class="stat-card__value" style="font-size:13px;color:#999">点击"捕获"记录当前状态</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" class="dashboard__col">
        <div class="stat-card">
          <div class="stat-card__label">
            <span class="stat-card__accent" style="background:#2ba471"></span>AFTER Scorecard
            <el-button type="success" size="mini" style="margin-left:8px" @click="captureScorecardAction('AFTER')">捕获</el-button>
          </div>
          <div v-if="scorecardAfter" class="stat-card__value" style="font-size:13px;line-height:1.8">
            <div>综合评分: <b style="font-size:18px">{{ scorecardAfter.overallScore || 0 }}</b>/100</div>
            <div>Robot {{ scorecardAfter.robotTotal || 0 }} · 封面图{{ scorecardAfter.robotCoverImageRate || 0 }}% · 参数{{ scorecardAfter.robotParamRate || 0 }}%</div>
            <div>Source {{ scorecardAfter.sourceTotal || 0 }} · 健康{{ scorecardAfter.sourceHealthy || 0 }} · FRESH{{ scorecardAfter.robotFresh || 0 }}</div>
            <div style="font-size:11px;color:#999">{{ scorecardAfter.snapshotTime }}</div>
          </div>
          <div v-else class="stat-card__value" style="font-size:13px;color:#999">采集运行后点击"捕获"对比</div>
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
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getStats, getTrend, getCrawlerHealth, getQualitySummary } from '@/api/dashboard'
import { getDataQuality, refreshFreshness, getCoverageDrillDown, captureScorecard, getScorecard } from '@/api/dataQuality'

const days = ref(7)
const chartRef = ref(null)
let chart = null

/* Phase11: 采集器健康 & 数据质量 */
const crawlerHealth = reactive({ collectorAvailable: false, httpStatus: null, error: null })
const qualitySummary = reactive({ totalScored: 0, lowScoreCount: 0, pendingIssues: 0 })

/* Phase12: DataQuality统计 */
const dq = reactive({
  robotTotal: 0, robotWithCoverImage: 0, robotCoverImageRate: 0,
  robotWithGallery: 0, robotGalleryRate: 0, robotWithParams: 0, robotParamRate: 0,
  robotWithSourceUrl: 0, robotSourceUrlRate: 0, robotWithBrand: 0, robotBrandRate: 0,
  robotWithCategory: 0, robotCategoryRate: 0,
  brandTotal: 0, brandWithWebsite: 0, brandWebsiteRate: 0,
  articleTotal: 0, articleWithCoverImage: 0, articleCoverImageRate: 0,
  articleWithSourceUrl: 0, articleSourceUrlRate: 0,
  sourceTotal: 0, sourceHealthy: 0, sourceDegraded: 0, sourceFailed: 0, sourceDisabled: 0, sourceUnknown: 0,
  sourceOfficial: 0, sourceTrusted: 0, sourceNormal: 0,
  robotFresh: 0, robotAging: 0, robotStale: 0
})

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
  feedbackCount: 0,
  pendingFeedbackCount: 0,
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
  { key: 'feedbackCount', label: '反馈数', color: '#1668dc' },
  { key: 'pendingFeedbackCount', label: '待处理反馈', color: '#d93026' },
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

/** Phase11: 加载采集器健康状态 */
async function loadCrawlerHealth() {
  try {
    const res = await getCrawlerHealth()
    const data = res.data || {}
    crawlerHealth.collectorAvailable = data.collectorAvailable || false
    crawlerHealth.httpStatus = data.httpStatus || null
    crawlerHealth.error = data.error || null
  } catch (e) {
    crawlerHealth.collectorAvailable = false
    crawlerHealth.error = '请求失败'
  }
}

/** Phase11: 加载数据质量概览 */
async function loadQualitySummary() {
  try {
    const res = await getQualitySummary()
    const data = res.data || {}
    qualitySummary.totalScored = data.totalScored || 0
    qualitySummary.lowScoreCount = data.lowScoreCount || 0
    qualitySummary.pendingIssues = data.pendingIssues || 0
  } catch (e) {
    /* ignore */
  }
}

/** Phase12: 加载数据覆盖率+Source健康度+Freshness */
async function loadDataQuality() {
  try {
    const res = await getDataQuality()
    const data = res.data || res || {}
    Object.keys(dq).forEach(key => {
      if (data[key] !== undefined) dq[key] = data[key]
    })
  } catch (e) {
    /* ignore */
  }
}

/** Phase12: 刷新Freshness */
async function doRefreshFreshness() {
  try {
    const res = await refreshFreshness()
    const updated = (res.data && res.data.updated) || 0
    ElMessage.success('Freshness刷新完成，更新' + updated + '条')
    await loadDataQuality()
  } catch (e) {
    ElMessage.error('Freshness刷新失败')
  }
}

/** Phase12 P0-5: Coverage下钻 */
const drillDimension = ref('dataSource')
const drillItems = ref([])

async function loadDrillDown() {
  try {
    const res = await getCoverageDrillDown(drillDimension.value)
    const data = res.data || res || {}
    drillItems.value = data.items || []
  } catch (e) {
    drillItems.value = []
  }
}

/** Phase12 P0-6: Scorecard BEFORE/AFTER */
const scorecardBefore = ref(null)
const scorecardAfter = ref(null)

async function captureScorecardAction(label) {
  try {
    const res = await captureScorecard(label)
    const data = res.data || res || {}
    if (label === 'BEFORE') {
      scorecardBefore.value = data
    } else {
      scorecardAfter.value = data
    }
    ElMessage.success(label + ' Scorecard已持久化，评分: ' + (data.overallScore || 0))
  } catch (e) {
    ElMessage.error('Scorecard捕获失败')
  }
}

async function loadScorecards() {
  try {
    const [beforeRes, afterRes] = await Promise.all([
      getScorecard('BEFORE').catch(() => null),
      getScorecard('AFTER').catch(() => null)
    ])
    if (beforeRes && beforeRes.data) scorecardBefore.value = beforeRes.data
    if (afterRes && afterRes.data) scorecardAfter.value = afterRes.data
  } catch (e) {
    /* ignore */
  }
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
  loadCrawlerHealth()
  loadQualitySummary()
  loadDataQuality()
  loadDrillDown()
  loadScorecards()
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
