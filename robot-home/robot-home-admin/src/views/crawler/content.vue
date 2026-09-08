<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stats-bar">
      <div class="stat-card">
        <div class="stat-value">{{ stats.totalArticles || 0 }}</div>
        <div class="stat-label">文章总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value stat-pending">{{ stats.pendingArticles || 0 }}</div>
        <div class="stat-label">待审核文章</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.totalProducts || 0 }}</div>
        <div class="stat-label">产品总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value stat-pending">{{ stats.pendingProducts || 0 }}</div>
        <div class="stat-label">待审核产品</div>
      </div>
      <div class="stat-card stat-action">
        <el-button type="primary" :loading="publishing" @click="handlePublish">发布到主站</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 文章审核 -->
        <el-tab-pane label="文章审核" name="article">
          <div class="filter-bar">
            <el-input v-model="articleQuery.title" placeholder="标题关键词" clearable style="width: 180px" @keyup.enter="searchArticles" />
            <el-select v-model="articleQuery.articleStatus" placeholder="状态" clearable style="width: 130px">
              <el-option label="待审核" value="PENDING_REVIEW" />
              <el-option label="已通过" value="AUTO_APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
            <el-select v-model="articleQuery.matchStatus" placeholder="匹配" clearable style="width: 130px">
              <el-option label="未匹配" value="UNMATCHED" />
              <el-option label="已匹配" value="MATCHED" />
              <el-option label="已关联" value="LINKED" />
            </el-select>
            <el-button type="primary" @click="searchArticles">查询</el-button>
            <el-button @click="resetArticleQuery">重置</el-button>
            <el-button type="success" :disabled="!articleSelection.length" @click="batchReviewArticles('approve')">
              批量通过 ({{ articleSelection.length }})
            </el-button>
            <el-button type="danger" :disabled="!articleSelection.length" @click="batchReviewArticles('reject')">
              批量拒绝 ({{ articleSelection.length }})
            </el-button>
          </div>

          <el-table v-loading="articleLoading" :data="articleList" border stripe @selection-change="onArticleSelection">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="100" />
            <el-table-column prop="sourceName" label="来源" width="120" show-overflow-tooltip />
            <el-table-column prop="publishTime" label="发布时间" width="160" />
            <el-table-column prop="articleStatus" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="reviewStatusType(row.articleStatus)" size="small">{{ reviewStatusLabel(row.articleStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="matchStatus" label="匹配" width="90">
              <template #default="{ row }">
                <el-tag :type="matchStatusType(row.matchStatus)" size="small">{{ matchStatusLabel(row.matchStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="previewArticle(row)">预览</el-button>
                <el-button v-if="row.articleStatus === 'PENDING_REVIEW'" link type="success" @click="reviewArticle(row, 'approve')">通过</el-button>
                <el-button v-if="row.articleStatus === 'PENDING_REVIEW'" link type="danger" @click="reviewArticle(row, 'reject')">拒绝</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无文章" /></template>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination v-model:current-page="articlePage.pageNum" v-model:page-size="articlePage.pageSize"
              :page-sizes="[10, 20, 50]" :total="articlePage.total" layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadArticles" @current-change="loadArticles" />
          </div>
        </el-tab-pane>

        <!-- 产品审核 -->
        <el-tab-pane label="产品审核" name="product">
          <div class="filter-bar">
            <el-input v-model="productQuery.productName" placeholder="产品名称" clearable style="width: 180px" @keyup.enter="searchProducts" />
            <el-select v-model="productQuery.productStatus" placeholder="状态" clearable style="width: 130px">
              <el-option label="待审核" value="PENDING_REVIEW" />
              <el-option label="已通过" value="AUTO_APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
            <el-select v-model="productQuery.matchStatus" placeholder="匹配" clearable style="width: 130px">
              <el-option label="未匹配" value="UNMATCHED" />
              <el-option label="已匹配" value="MATCHED" />
              <el-option label="已关联" value="LINKED" />
            </el-select>
            <el-button type="primary" @click="searchProducts">查询</el-button>
            <el-button @click="resetProductQuery">重置</el-button>
            <el-button type="success" :disabled="!productSelection.length" @click="batchReviewProducts('approve')">
              批量通过 ({{ productSelection.length }})
            </el-button>
            <el-button type="danger" :disabled="!productSelection.length" @click="batchReviewProducts('reject')">
              批量拒绝 ({{ productSelection.length }})
            </el-button>
          </div>

          <el-table v-loading="productLoading" :data="productList" border stripe @selection-change="onProductSelection">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="productName" label="产品名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="brandName" label="品牌" width="120" />
            <el-table-column prop="model" label="型号" width="120" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="sourceName" label="来源" width="120" show-overflow-tooltip />
            <el-table-column prop="productStatus" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="reviewStatusType(row.productStatus)" size="small">{{ reviewStatusLabel(row.productStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="matchStatus" label="匹配" width="90">
              <template #default="{ row }">
                <el-tag :type="matchStatusType(row.matchStatus)" size="small">{{ matchStatusLabel(row.matchStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="previewProduct(row)">预览</el-button>
                <el-button v-if="row.productStatus === 'PENDING_REVIEW'" link type="success" @click="reviewProduct(row, 'approve')">通过</el-button>
                <el-button v-if="row.productStatus === 'PENDING_REVIEW'" link type="danger" @click="reviewProduct(row, 'reject')">拒绝</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无产品" /></template>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination v-model:current-page="productPage.pageNum" v-model:page-size="productPage.pageSize"
              :page-sizes="[10, 20, 50]" :total="productPage.total" layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadProducts" @current-change="loadProducts" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 文章预览弹窗 -->
    <el-dialog v-model="articlePreviewVisible" title="文章预览" width="800px" destroy-on-close>
      <div v-if="articlePreview" style="max-height: 60vh; overflow-y: auto">
        <h2 style="margin: 0 0 12px">{{ articlePreview.title }}</h2>
        <div style="color: #909399; margin-bottom: 16px; font-size: 13px">
          <span v-if="articlePreview.author">作者: {{ articlePreview.author }}</span>
          <span v-if="articlePreview.publishTime" style="margin-left: 16px">发布: {{ articlePreview.publishTime }}</span>
          <span v-if="articlePreview.sourceName" style="margin-left: 16px">来源: {{ articlePreview.sourceName }}</span>
        </div>
        <el-image v-if="articlePreview.coverImage" :src="articlePreview.coverImage" style="max-width: 100%; margin-bottom: 16px" fit="contain" />
        <div v-if="articlePreview.contentHtml" v-html="articlePreview.contentHtml" style="line-height: 1.8" />
        <div v-else-if="articlePreview.contentText" style="white-space: pre-wrap; line-height: 1.8">{{ articlePreview.contentText }}</div>
      </div>
      <template #footer>
        <el-button @click="articlePreviewVisible = false">关 闭</el-button>
        <el-button v-if="articlePreview && articlePreview.articleStatus === 'PENDING_REVIEW'" type="success" @click="reviewArticle(articlePreview, 'approve'); articlePreviewVisible = false">通 过</el-button>
        <el-button v-if="articlePreview && articlePreview.articleStatus === 'PENDING_REVIEW'" type="danger" @click="reviewArticle(articlePreview, 'reject'); articlePreviewVisible = false">拒 绝</el-button>
      </template>
    </el-dialog>

    <!-- 产品预览弹窗 -->
    <el-dialog v-model="productPreviewVisible" title="产品预览" width="700px" destroy-on-close>
      <div v-if="productPreview" style="max-height: 60vh; overflow-y: auto">
        <h2 style="margin: 0 0 12px">{{ productPreview.productName }}</h2>
        <div style="color: #909399; margin-bottom: 16px; font-size: 13px">
          <span v-if="productPreview.brandName">品牌: {{ productPreview.brandName }}</span>
          <span v-if="productPreview.model" style="margin-left: 16px">型号: {{ productPreview.model }}</span>
          <span v-if="productPreview.category" style="margin-left: 16px">分类: {{ productPreview.category }}</span>
        </div>
        <el-image v-if="productPreview.coverImage" :src="productPreview.coverImage" style="max-width: 300px; margin-bottom: 16px" fit="contain" />
        <div v-if="productPreview.gallery" style="margin-bottom: 16px">
          <el-image v-for="(img, i) in (productPreview.gallery || '').split(',').filter(Boolean)" :key="i" :src="img"
            style="width: 120px; height: 120px; margin-right: 8px" fit="cover" :preview-src-list="productPreview.gallery.split(',').filter(Boolean)" />
        </div>
        <div v-if="productPreview.description" style="white-space: pre-wrap; line-height: 1.8">{{ productPreview.description }}</div>
        <div v-if="productPreview.specifications" style="margin-top: 16px">
          <h4>规格参数</h4>
          <div style="white-space: pre-wrap">{{ productPreview.specifications }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="productPreviewVisible = false">关 闭</el-button>
        <el-button v-if="productPreview && productPreview.productStatus === 'PENDING_REVIEW'" type="success" @click="reviewProduct(productPreview, 'approve'); productPreviewVisible = false">通 过</el-button>
        <el-button v-if="productPreview && productPreview.productStatus === 'PENDING_REVIEW'" type="danger" @click="reviewProduct(productPreview, 'reject'); productPreviewVisible = false">拒 绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCrawlerArticlePage, getCrawlerArticle, reviewCrawlerArticle, batchReviewCrawlerArticles,
  getCrawlerProductPage, getCrawlerProduct, reviewCrawlerProduct, batchReviewCrawlerProducts,
  getCrawlerContentStats, publishCrawlerAll
} from '@/api/crawler'
import { cleanParams } from '@/utils'

const activeTab = ref('article')
const stats = ref({})
const publishing = ref(false)

// ---- 文章 ----
const articleLoading = ref(false)
const articleList = ref([])
const articleSelection = ref([])
const articlePage = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const articleQuery = reactive({ title: '', articleStatus: 'PENDING_REVIEW', matchStatus: '' })
const articlePreviewVisible = ref(false)
const articlePreview = ref(null)

// ---- 产品 ----
const productLoading = ref(false)
const productList = ref([])
const productSelection = ref([])
const productPage = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const productQuery = reactive({ productName: '', productStatus: 'PENDING_REVIEW', matchStatus: '' })
const productPreviewVisible = ref(false)
const productPreview = ref(null)

function reviewStatusLabel(s) {
  const map = { PENDING_REVIEW: '待审核', AUTO_APPROVED: '已通过', REJECTED: '已拒绝' }
  return map[s] || s
}
function reviewStatusType(s) {
  const map = { PENDING_REVIEW: 'warning', AUTO_APPROVED: 'success', REJECTED: 'danger' }
  return map[s] || 'info'
}
function matchStatusLabel(s) {
  const map = { UNMATCHED: '未匹配', MATCHED: '已匹配', LINKED: '已关联' }
  return map[s] || s
}
function matchStatusType(s) {
  const map = { UNMATCHED: 'info', MATCHED: 'success', LINKED: 'success' }
  return map[s] || 'info'
}

async function loadStats() {
  try {
    const data = await getCrawlerContentStats() || {}
    // 后端返回 articleTotal/articlePending/productTotal/productPending，映射为前端字段
    stats.value = {
      totalArticles: data.articleTotal || 0,
      pendingArticles: data.articlePending || 0,
      totalProducts: data.productTotal || 0,
      pendingProducts: data.productPending || 0
    }
  } catch { stats.value = {} }
}

async function loadArticles() {
  articleLoading.value = true
  try {
    const data = await getCrawlerArticlePage({ page: articlePage.pageNum, size: articlePage.pageSize, ...cleanParams(articleQuery) })
    articleList.value = (data && data.records) || []
    articlePage.total = (data && data.total) || 0
  } catch { articleList.value = [] } finally { articleLoading.value = false }
}

async function loadProducts() {
  productLoading.value = true
  try {
    const data = await getCrawlerProductPage({ page: productPage.pageNum, size: productPage.pageSize, ...cleanParams(productQuery) })
    productList.value = (data && data.records) || []
    productPage.total = (data && data.total) || 0
  } catch { productList.value = [] } finally { productLoading.value = false }
}

function searchArticles() { articlePage.pageNum = 1; loadArticles() }
function resetArticleQuery() { articleQuery.title = ''; articleQuery.articleStatus = 'PENDING_REVIEW'; articleQuery.matchStatus = ''; searchArticles() }
function searchProducts() { productPage.pageNum = 1; loadProducts() }
function resetProductQuery() { productQuery.productName = ''; productQuery.productStatus = 'PENDING_REVIEW'; productQuery.matchStatus = ''; searchProducts() }
function handleTabChange() { if (activeTab.value === 'article') loadArticles(); else loadProducts() }
function onArticleSelection(rows) { articleSelection.value = rows }
function onProductSelection(rows) { productSelection.value = rows }

async function previewArticle(row) {
  const data = await getCrawlerArticle(row.id)
  articlePreview.value = data
  articlePreviewVisible.value = true
}

async function previewProduct(row) {
  const data = await getCrawlerProduct(row.id)
  productPreview.value = data
  productPreviewVisible.value = true
}

async function reviewArticle(row, action) {
  const reason = action === 'reject' ? await ElMessageBox.prompt('请输入拒绝原因', '拒绝文章', { confirmButtonText: '确认', cancelButtonText: '取消', inputPlaceholder: '拒绝原因' }).then(r => r.value).catch(() => null) : ''
  if (action === 'reject' && reason === null) return
  await reviewCrawlerArticle(row.id, action, reason || '')
  ElMessage.success(action === 'approve' ? '已通过' : '已拒绝')
  loadArticles(); loadStats()
}

async function reviewProduct(row, action) {
  const reason = action === 'reject' ? await ElMessageBox.prompt('请输入拒绝原因', '拒绝产品', { confirmButtonText: '确认', cancelButtonText: '取消', inputPlaceholder: '拒绝原因' }).then(r => r.value).catch(() => null) : ''
  if (action === 'reject' && reason === null) return
  await reviewCrawlerProduct(row.id, action, reason || '')
  ElMessage.success(action === 'approve' ? '已通过' : '已拒绝')
  loadProducts(); loadStats()
}

async function batchReviewArticles(action) {
  const ids = articleSelection.value.map(r => r.id)
  const label = action === 'approve' ? '通过' : '拒绝'
  try { await ElMessageBox.confirm(`确认批量${label} ${ids.length} 篇文章？`, '提示', { type: 'warning' }) } catch { return }
  await batchReviewCrawlerArticles(ids, action)
  ElMessage.success(`批量${label}成功`)
  loadArticles(); loadStats()
}

async function batchReviewProducts(action) {
  const ids = productSelection.value.map(r => r.id)
  const label = action === 'approve' ? '通过' : '拒绝'
  try { await ElMessageBox.confirm(`确认批量${label} ${ids.length} 个产品？`, '提示', { type: 'warning' }) } catch { return }
  await batchReviewCrawlerProducts(ids, action)
  ElMessage.success(`批量${label}成功`)
  loadProducts(); loadStats()
}

async function handlePublish() {
  try { await ElMessageBox.confirm('确认将所有已通过审核的内容发布到主站？', '发布确认', { type: 'warning' }) } catch { return }
  publishing.value = true
  try {
    const res = await publishCrawlerAll()
    ElMessage.success(res.message || '发布成功')
    loadStats()
  } finally { publishing.value = false }
}

onMounted(() => { loadStats(); loadArticles() })
</script>

<style scoped>
.stats-bar { display: flex; gap: 16px; }
.stat-card { flex: 1; background: #fff; border-radius: 8px; padding: 20px; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-value.stat-pending { color: #e6a23c; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }
.stat-action { display: flex; align-items: center; justify-content: center; }
</style>