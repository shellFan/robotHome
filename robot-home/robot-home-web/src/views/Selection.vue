<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>智能选型</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="selection-layout">
        <div class="selection-main">
          <div class="rh-card">
            <h2 class="selection-title">智能选型助手</h2>
            <p class="selection-desc">根据您的需求，为您推荐最合适的机器人</p>

            <el-form :model="form" label-width="100px" class="selection-form">
              <el-form-item label="应用场景">
                <el-select v-model="form.usageScene" placeholder="选择应用场景" clearable style="width: 100%">
                  <el-option v-for="s in usageScenes" :key="s" :label="s" :value="s" />
                </el-select>
              </el-form-item>
              <el-form-item label="预算范围">
                <el-select v-model="form.budgetRange" placeholder="选择预算范围" clearable style="width: 100%">
                  <el-option v-for="b in budgetRanges" :key="b" :label="b" :value="b" />
                </el-select>
              </el-form-item>
              <el-form-item label="品牌偏好">
                <el-select v-model="form.brandIds" multiple placeholder="选择品牌" clearable style="width: 100%">
                  <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="机器人类型">
                <el-select v-model="form.categoryId" placeholder="选择类型" clearable style="width: 100%">
                  <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="关键词">
                <el-input v-model="form.keyword" placeholder="输入关键词（如：清洁、配送）" clearable />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" :loading="searching" @click="doSearch">
                  开始选型
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="results.length" class="rh-card" style="margin-top: 20px;">
            <h3>推荐结果 ({{ total }})</h3>
            <div class="selection-results">
              <router-link
                v-for="r in results"
                :key="r.robotId"
                :to="'/robot/' + r.robotId"
                class="selection-result-item"
              >
                <div class="selection-result__img">
                  <img v-if="r.coverImage" :src="imageOf(r.coverImage)" alt="" />
                  <div v-else class="selection-result__placeholder">{{ (r.robotName || '?').charAt(0) }}</div>
                </div>
                <div class="selection-result__info">
                  <div class="selection-result__name">{{ r.robotName }}</div>
                  <div class="selection-result__brand rh-text-light">{{ r.brandName }}</div>
                  <div class="selection-result__score">
                    <span>匹配度: <strong>{{ r.matchScore }}</strong>分</span>
                    <span v-if="r.confidence != null" class="selection-result__confidence">置信度 {{ r.confidence }}%</span>
                  </div>
                  <!-- Phase11: 匹配条件badge -->
                  <div v-if="r.matchedConditions && r.matchedConditions.length" class="selection-result__conditions">
                    <span
                      v-for="mc in r.matchedConditions"
                      :key="mc.code"
                      class="selection-result__condition"
                      :class="'condition--' + mc.code.toLowerCase()"
                    >{{ mc.text }} +{{ mc.score }}</span>
                  </div>
                  <div v-if="r.matchReason && !r.matchedConditions" class="selection-result__details rh-text-light">
                    {{ r.matchReason }}
                  </div>
                </div>
              </router-link>
            </div>

            <el-pagination
              v-if="total > pageSize"
              background
              layout="total, prev, pager, next"
              :current-page="pageNum"
              :page-size="pageSize"
              :total="total"
              @current-change="changePage"
            />
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import { selectionApi } from '@/api'

const searching = ref(false)
const results = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const categories = ref([])
const brands = ref([])
const budgetRanges = ref(['5万以下', '5-10万', '10-30万', '30-50万', '50-100万', '100万以上'])
const usageScenes = ref(['工业制造', '物流仓储', '清洁服务', '医疗健康', '教育科研', '餐饮服务', '安防巡检', '农业', '家庭服务', '其他'])
const form = ref({
  usageScene: '',
  budgetRange: '',
  brandIds: [],
  categoryId: null,
  keyword: ''
})

async function loadFilters() {
  try {
    const res = await selectionApi.filters()
    if (res.data) {
      categories.value = res.data.categories || []
      brands.value = res.data.brands || []
      if (res.data.budgetRanges && res.data.budgetRanges.length) {
        budgetRanges.value = res.data.budgetRanges
      }
      if (res.data.usageScenes && res.data.usageScenes.length) {
        usageScenes.value = res.data.usageScenes
      }
    }
  } catch (e) { /* use defaults */ }
}

async function doSearch() {
  searching.value = true
  try {
    const data = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (form.value.usageScene) data.usageScene = form.value.usageScene
    if (form.value.budgetRange) data.budgetRange = form.value.budgetRange
    if (form.value.brandIds && form.value.brandIds.length) data.brandIds = form.value.brandIds
    if (form.value.categoryId) data.categoryId = form.value.categoryId
    if (form.value.keyword) data.keyword = form.value.keyword
    const res = await selectionApi.search(data)
    if (res.data) {
      results.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } finally {
    searching.value = false
  }
}

function changePage(p) {
  pageNum.value = p
  doSearch()
}

function imageOf(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : '/api/files/' + url
}

onMounted(loadFilters)
</script>

<style scoped>
.selection-layout { display: flex; gap: 20px; }
.selection-main { flex: 1; min-width: 0; }
.selection-title { font-size: 22px; font-weight: 600; margin-bottom: 8px; }
.selection-desc { color: #666; margin-bottom: 20px; }
.selection-form { max-width: 600px; }
.selection-results { display: flex; flex-direction: column; gap: 12px; margin: 16px 0; }
.selection-result-item { display: flex; gap: 16px; padding: 16px; border-radius: 8px; background: #fafafa; text-decoration: none; color: inherit; transition: background .2s; }
.selection-result-item:hover { background: #f0f0f0; }
.selection-result__img { width: 120px; height: 90px; border-radius: 6px; overflow: hidden; flex-shrink: 0; }
.selection-result__img img { width: 100%; height: 100%; object-fit: cover; }
.selection-result__placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #e0e0e0; font-size: 32px; color: #999; }
.selection-result__info { flex: 1; min-width: 0; }
.selection-result__name { font-size: 16px; font-weight: 500; margin-bottom: 4px; }
.selection-result__brand { font-size: 13px; margin-bottom: 6px; }
.selection-result__score { font-size: 14px; margin-bottom: 6px; }
.selection-result__score strong { color: #409eff; font-size: 18px; }
.selection-result__confidence { font-size: 12px; color: #67c23a; margin-left: 8px; }
.selection-result__details { display: flex; gap: 8px; flex-wrap: wrap; }
.selection-result__tag { font-size: 12px; background: #f0f0f0; padding: 2px 8px; border-radius: 4px; }

/* Phase11: 匹配条件badge */
.selection-result__conditions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.selection-result__condition {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #e8f5e9;
  color: #2e7d32;
  font-weight: 500;
}

.selection-result__condition.condition--category_match { background: #e3f2fd; color: #1565c0; }
.selection-result__condition.condition--budget_match { background: #fff3e0; color: #e65100; }
.selection-result__condition.condition--brand_match { background: #f3e5f5; color: #7b1fa2; }
.selection-result__condition.condition--usage_match { background: #e0f7fa; color: #00838f; }
.selection-result__condition.condition--filter_match { background: #e8f5e9; color: #2e7d32; }
.selection-result__condition.condition--filter_partial { background: #fff8e1; color: #f57f17; }
</style>