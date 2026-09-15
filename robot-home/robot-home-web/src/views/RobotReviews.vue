<template>
  <MainLayout>
    <div class="rh-container">
      <RobotSubNav :id="id" :robot-name="robotName" active="reviews" current-label="口碑" />

      <div class="rh-grid rh-grid--2 detail-body">
        <div>
          <!-- 评价汇总 -->
          <section class="rh-card rh-section">
            <h2 class="rh-section__title">用户口碑</h2>
            <div v-if="summaryLoading" class="rh-empty">加载中…</div>
            <div v-else-if="summary && summary.reviewCount > 0" class="review-summary">
              <div class="review-summary__overall">
                <div class="review-summary__score">{{ formatScore(summary.overallAvg) }}</div>
                <div class="review-summary__meta">
                  <el-rate :model-value="Number(summary.overallAvg)" disabled show-score text-color="#ff9900" score-template="{value}" />
                  <span class="rh-text-light">{{ summary.reviewCount }} 条评价</span>
                </div>
              </div>
              <div class="review-summary__dims">
                <div v-if="summary.qualityAvg" class="review-dim">
                  <span class="review-dim__label">质量</span>
                  <el-rate :model-value="Number(summary.qualityAvg)" disabled size="small" />
                  <span class="review-dim__val">{{ formatScore(summary.qualityAvg) }}</span>
                </div>
                <div v-if="summary.serviceAvg" class="review-dim">
                  <span class="review-dim__label">服务</span>
                  <el-rate :model-value="Number(summary.serviceAvg)" disabled size="small" />
                  <span class="review-dim__val">{{ formatScore(summary.serviceAvg) }}</span>
                </div>
                <div v-if="summary.costAvg" class="review-dim">
                  <span class="review-dim__label">性价比</span>
                  <el-rate :model-value="Number(summary.costAvg)" disabled size="small" />
                  <span class="review-dim__val">{{ formatScore(summary.costAvg) }}</span>
                </div>
              </div>
              <div class="review-summary__dist">
                <div class="review-dist__row"><span>5星</span><el-progress :percentage="scorePercent(summary.score5Count)" :stroke-width="8" /></div>
                <div class="review-dist__row"><span>4星</span><el-progress :percentage="scorePercent(summary.score4Count)" :stroke-width="8" color="#67c23a" /></div>
                <div class="review-dist__row"><span>3星</span><el-progress :percentage="scorePercent(summary.score3Count)" :stroke-width="8" color="#e6a23c" /></div>
                <div class="review-dist__row"><span>2星</span><el-progress :percentage="scorePercent(summary.score2Count)" :stroke-width="8" color="#f56c6c" /></div>
                <div class="review-dist__row"><span>1星</span><el-progress :percentage="scorePercent(summary.score1Count)" :stroke-width="8" color="#909399" /></div>
              </div>
            </div>
            <div v-else class="review-empty">
              <p>暂无用户评价</p>
            </div>
          </section>

          <!-- 发布评价 -->
          <section class="rh-card rh-section">
            <h2 class="rh-section__title">发表评价</h2>
            <div v-if="!userStore.isLogin" class="rh-empty">
              <el-button type="primary" @click="$router.push('/login')">登录后评价</el-button>
            </div>
            <div v-else-if="myReview" class="my-review-hint">
              <span>您已评价过此产品</span>
              <el-button type="primary" text size="small" @click="editReview">修改评价</el-button>
              <el-button type="danger" text size="small" @click="deleteReview">删除评价</el-button>
            </div>
            <el-form v-else :model="reviewForm" label-width="80px" class="review-form" @submit.prevent="submitReview">
              <el-form-item label="综合评分" required>
                <el-rate v-model="reviewForm.overallScore" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" show-text :texts="rateTexts" />
              </el-form-item>
              <el-form-item label="质量评分" required>
                <el-rate v-model="reviewForm.qualityScore" />
              </el-form-item>
              <el-form-item label="服务评分" required>
                <el-rate v-model="reviewForm.serviceScore" />
              </el-form-item>
              <el-form-item label="性价比" required>
                <el-rate v-model="reviewForm.costScore" />
              </el-form-item>
              <el-form-item label="评价内容" required>
                <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="请分享您的使用体验（10-2000字）" maxlength="2000" show-word-limit />
              </el-form-item>
              <el-form-item label="图片">
                <el-input v-model="reviewForm.images" placeholder="图片URL，多张用逗号分隔（可选）" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="submitReview">提交评价</el-button>
              </el-form-item>
            </el-form>
          </section>

          <!-- 评价列表 -->
          <section class="rh-card rh-section">
            <h2 class="rh-section__title">全部评价</h2>
            <div v-if="listLoading" class="rh-empty">加载中…</div>
            <div v-else-if="reviews.length === 0" class="rh-empty">暂无评价</div>
            <div v-else class="review-list">
              <div v-for="r in reviews" :key="r.id" class="review-item">
                <div class="review-item__header">
                  <span class="review-item__user">{{ r.userName || '匿名用户' }}</span>
                  <el-rate :model-value="r.overallScore" disabled size="small" />
                  <span class="review-item__date">{{ formatDate(r.createTime) }}</span>
                </div>
                <div class="review-item__dims">
                  <span v-if="r.qualityScore">质量 {{ r.qualityScore }}</span>
                  <span v-if="r.serviceScore">服务 {{ r.serviceScore }}</span>
                  <span v-if="r.costScore">性价比 {{ r.costScore }}</span>
                </div>
                <div class="review-item__content">{{ r.content }}</div>
                <div v-if="r.images" class="review-item__images">
                  <img v-for="(img, idx) in r.images.split(',')" :key="idx" :src="img" class="review-img" loading="lazy" />
                </div>
                <div v-if="r.reply" class="review-item__reply">
                  <span class="review-item__reply-label">官方回复：</span>{{ r.reply }}
                </div>
                <div class="review-item__actions">
                  <el-button type="info" text size="small" @click="markHelpful(r.id)">
                    有用 ({{ r.helpfulCount || 0 }})
                  </el-button>
                </div>
              </div>
              <el-pagination
                v-if="total > pageSize"
                :current-page="pageNum"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                class="review-pagination"
                @current-change="onPageChange"
              />
            </div>
          </section>
        </div>

        <div>
          <!-- 综合评分 -->
          <section class="rh-card rh-section">
            <h2 class="rh-section__title">综合评分</h2>
            <div class="score-box">
              <div class="score-box__value">{{ scoreText }}</div>
              <el-rate :model-value="score" disabled show-score text-color="#ff9900" />
            </div>
            <div class="score-box__hint">评分由平台结合参数完整度与用户评价生成，仅供参考</div>
          </section>

          <section class="rh-card rh-section">
            <h2 class="rh-section__title">大家都在关注的参数</h2>
            <div v-if="paramPreview.length" class="param-preview">
              <div v-for="p in paramPreview" :key="p.label" class="param-preview__item">
                <span class="param-preview__label">{{ p.label }}</span>
                <span class="param-preview__value">{{ p.value }}</span>
              </div>
            </div>
            <div v-else class="rh-empty">暂无参数</div>
          </section>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import RobotSubNav from '@/components/RobotSubNav.vue'
import { robotApi, reviewApi } from '@/api'
import { useUserStore } from '@/store/user'
import { parseMainParams, formatDate } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))
const robotName = ref('机器人')
const score = ref(0)
const paramPreview = ref([])

const scoreText = computed(() => (score.value > 0 ? score.value.toFixed(1) : '暂无'))

// Review Summary
const summary = ref(null)
const summaryLoading = ref(false)

// Review List
const reviews = ref([])
const listLoading = ref(false)
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)

// My Review
const myReview = ref(null)

// Submit Review
const submitting = ref(false)
const reviewForm = ref({
  overallScore: 0,
  qualityScore: 0,
  serviceScore: 0,
  costScore: 0,
  content: '',
  images: ''
})

const rateTexts = ['很差', '较差', '一般', '较好', '很好']

// Edit mode
const editing = ref(false)

async function loadSummary () {
  summaryLoading.value = true
  try {
    summary.value = await reviewApi.summary(id.value)
  } catch (e) {
    summary.value = null
  } finally {
    summaryLoading.value = false
  }
}

async function loadReviews () {
  listLoading.value = true
  try {
    const data = await reviewApi.list(id.value, pageNum.value, pageSize)
    reviews.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (e) {
    reviews.value = []
  } finally {
    listLoading.value = false
  }
}

function onPageChange (page) {
  pageNum.value = page
  loadReviews()
}

function scorePercent (count) {
  if (!summary.value || !summary.value.reviewCount) return 0
  return Math.round((count / summary.value.reviewCount) * 100)
}

function formatScore (val) {
  if (val == null) return '-'
  return Number(val).toFixed(1)
}

async function submitReview () {
  const f = reviewForm.value
  if (!f.overallScore || !f.qualityScore || !f.serviceScore || !f.costScore) {
    ElMessage.warning('请完成所有评分项')
    return
  }
  if (!f.content || f.content.length < 10) {
    ElMessage.warning('评价内容至少10个字')
    return
  }
  submitting.value = true
  try {
    await reviewApi.submit({
      robotId: id.value,
      overallScore: f.overallScore,
      qualityScore: f.qualityScore,
      serviceScore: f.serviceScore,
      costScore: f.costScore,
      content: f.content,
      images: f.images
    })
    ElMessage.success('评价已提交，等待审核')
    reviewForm.value = { overallScore: 0, qualityScore: 0, serviceScore: 0, costScore: 0, content: '', images: '' }
    loadSummary()
    loadReviews()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function editReview () {
  if (!myReview.value) return
  editing.value = true
  reviewForm.value = {
    overallScore: myReview.value.overallScore,
    qualityScore: myReview.value.qualityScore,
    serviceScore: myReview.value.serviceScore,
    costScore: myReview.value.costScore,
    content: myReview.value.content || '',
    images: myReview.value.images || ''
  }
  myReview.value = null
}

async function deleteReview () {
  if (!myReview.value) return
  try {
    await ElMessageBox.confirm('确定删除您的评价？', '提示', { type: 'warning' })
    await reviewApi.remove(myReview.value.id)
    ElMessage.success('评价已删除')
    myReview.value = null
    loadSummary()
    loadReviews()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

async function markHelpful (reviewId) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await reviewApi.helpful(reviewId)
    ElMessage.success('感谢反馈')
    loadReviews()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(async () => {
  const detail = await robotApi.detail(id.value)
  robotName.value = detail.robot.name
  score.value = Number(detail.robot.score || 0) / 2
  paramPreview.value = parseMainParams(detail.robot.mainParams, 6)
  setPageMeta({ title: robotName.value + ' 口碑评价 - 机器人之家' })
  loadSummary()
  loadReviews()
})
</script>

<style scoped lang="scss">
.detail-body {
  align-items: start;
}

.score-box {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-box__value {
  font-size: 34px;
  font-weight: 700;
  color: var(--rh-warning);
}

.score-box__hint {
  font-size: 12px;
  color: var(--rh-text-light);
  margin-top: 8px;
}

.param-preview {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.param-preview__item {
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
  padding: 10px;
  text-align: center;
}

.param-preview__label {
  display: block;
  font-size: 12px;
  color: var(--rh-text-light);
}

.param-preview__value {
  font-weight: 600;
}

/* Review Summary */
.review-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-summary__overall {
  display: flex;
  align-items: center;
  gap: 16px;
}

.review-summary__score {
  font-size: 42px;
  font-weight: 700;
  color: var(--rh-primary);
  line-height: 1;
}

.review-summary__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-summary__dims {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.review-dim {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.review-dim__label {
  color: var(--rh-text-sub);
  min-width: 40px;
}

.review-dim__val {
  font-weight: 500;
  color: var(--rh-primary);
}

.review-summary__dist {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-dist__row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--rh-text-sub);
}

.review-dist__row span {
  min-width: 28px;
  text-align: right;
}

.review-dist__row .el-progress {
  flex: 1;
}

.review-empty {
  text-align: center;
  padding: 24px 0;
  color: var(--rh-text-sub);
}

/* My Review Hint */
.my-review-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  color: var(--rh-text-sub);
}

/* Review Form */
.review-form {
  max-width: 600px;
}

/* Review List */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--rh-border-light);
}

.review-item:last-child {
  border-bottom: none;
}

.review-item__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.review-item__user {
  font-weight: 500;
  font-size: 14px;
}

.review-item__date {
  font-size: 12px;
  color: var(--rh-text-light);
  margin-left: auto;
}

.review-item__dims {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--rh-text-sub);
  margin-bottom: 8px;
}

.review-item__content {
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 8px;
}

.review-item__images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.review-img {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 4px;
  cursor: zoom-in;
}

.review-item__reply {
  background: var(--rh-surface-sub);
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--rh-text-sub);
  margin-bottom: 8px;
}

.review-item__reply-label {
  color: var(--rh-primary);
  font-weight: 500;
}

.review-item__actions {
  display: flex;
  gap: 8px;
}

.review-pagination {
  margin-top: 16px;
  text-align: center;
}
</style>