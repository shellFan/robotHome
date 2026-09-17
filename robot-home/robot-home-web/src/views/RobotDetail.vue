<template>
  <MainLayout>
    <div v-if="detail" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'robots' }">机器人库</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.robot.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <section class="detail-head">
        <div class="detail-head__gallery">
          <el-image
            :src="activeImage"
            :alt="detail.robot.name"
            class="detail-head__main"
            fit="cover"
            :preview-src-list="galleryFull"
            :initial-index="activeIdx"
            hide-on-click-modal
          />
          <div v-if="gallery.length > 1" class="detail-head__thumbs">
            <img
              v-for="(img, idx) in gallery"
              :key="idx"
              :src="imageOf(img)"
              :class="{ 'is-active': activeIdx === idx }"
              @click="activeIdx = idx"
            />
          </div>
        </div>

        <div class="detail-head__info">
          <div class="detail-head__brand">
            <router-link v-if="detail.brandId" :to="'/brand/' + detail.brandId">
              {{ detail.brandName }}
            </router-link>
            <span v-else>{{ detail.brandName }}</span>
            <span v-if="detail.companyName" class="rh-text-light">
              · {{ detail.companyName }}
            </span>
          </div>
          <h1 class="detail-head__name">{{ detail.robot.name }}</h1>
          <div v-if="detail.robot.model || detail.robot.releaseDate || detail.robot.status" class="detail-head__meta">
            <span v-if="detail.robot.model">型号：{{ detail.robot.model }}</span>
            <span v-if="detail.robot.releaseDate">发布：{{ formatDate(detail.robot.releaseDate) }}</span>
            <el-tag v-if="statusTag" :type="statusTag.type" size="small">{{ statusTag.label }}</el-tag>
          </div>
          <p v-if="detail.robot.subtitle" class="detail-head__subtitle">{{ detail.robot.subtitle }}</p>

          <div class="detail-head__price">
            <span class="detail-head__price-label">指导价</span>
            <span class="detail-head__price-value">{{ formatPrice(detail.robot.guidePrice) }}</span>
            <span v-if="detail.robot.marketPrice" class="detail-head__market">
              市场价 {{ formatPrice(detail.robot.marketPrice) }}
            </span>
          </div>

          <!-- 核心参数速览 -->
          <div v-if="coreParams.length" class="detail-head__core-params">
            <div v-for="p in coreParams" :key="p.label" class="core-param">
              <span class="core-param__label">{{ p.label }}</span>
              <span class="core-param__value">{{ p.value }}</span>
            </div>
          </div>

          <div class="detail-head__tags">
            <span v-for="tag in detail.scenes || []" :key="'s' + tag" class="rh-tag rh-tag--primary">
              {{ tag }}
            </span>
            <span v-for="tag in detail.devs || []" :key="'d' + tag" class="rh-tag">{{ tag }}</span>
            <span v-for="tag in detail.ais || []" :key="'a' + tag" class="rh-tag">{{ tag }}</span>
          </div>

          <div class="detail-head__stats">
            <span>浏览 {{ formatCount(detail.robot.viewCount) }}</span>
            <span>收藏 {{ formatCount(detail.robot.favoriteCount) }}</span>
            <span>对比 {{ formatCount(detail.robot.compareCount) }}</span>
            <span>询价 {{ formatCount(detail.robot.inquiryCount) }}</span>
          </div>

          <div class="detail-head__actions">
            <el-dropdown split-button type="primary" size="large" @click="openProcure('PRICE')">
              获取底价
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openProcure('PRICE')">获取底价</el-dropdown-item>
                  <el-dropdown-item @click="openProcure('PURCHASE')">我要采购</el-dropdown-item>
                  <el-dropdown-item @click="openProcure('LEASE')">租赁咨询</el-dropdown-item>
                  <el-dropdown-item @click="openProcure('COOPERATE')">合作洽谈</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="large" :type="detail.favorited ? 'primary' : 'default'" plain @click="toggleFavorite">
              {{ detail.favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button size="large" plain @click="toggleLike">
              {{ liked ? '已点赞' : '点赞' }} {{ likeCount }}
            </el-button>
            <el-button size="large" :type="inCompare ? 'primary' : 'default'" plain @click="toggleCompare">
              加入对比
            </el-button>
          </div>

          <!-- 来源可信度 -->
          <div v-if="detail.robot.dataSource || detail.robot.sourceName" class="detail-head__source">
            <el-icon><InfoFilled /></el-icon>
            <span>数据来源：{{ detail.robot.sourceName || dataSourceLabel }}</span>
            <span v-if="detail.robot.lastVerifiedTime" class="rh-text-light">
              · 验证于 {{ formatDate(detail.robot.lastVerifiedTime, 'YYYY-MM-DD') }}
            </span>
          </div>

          <div v-if="detail.prices && detail.prices.length" class="detail-head__channels">
            <div class="detail-head__channels-title">渠道报价</div>
            <div v-for="price in detail.prices" :key="price.id" class="detail-head__channel">
              <span>{{ price.channel }} · {{ price.region }}</span>
              <span class="rh-price">{{ formatPrice(price.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 子页面导航（首页 / 参数 / 图片 / 视频 / 口碑） -->
      <el-tabs :model-value="'home'" class="detail-tabs" @tab-click="onTab">
        <el-tab-pane label="首页" name="home" />
        <el-tab-pane label="参数配置" name="params" />
        <el-tab-pane label="图片" name="images" />
        <el-tab-pane label="视频" name="videos" />
        <el-tab-pane label="口碑" name="reviews" />
      </el-tabs>

      <div class="rh-grid rh-grid--2 detail-body">
        <div>
          <!-- 动态参数组 -->
          <section v-if="detail.paramGroups && detail.paramGroups.length" class="rh-card rh-section">
            <h2 class="rh-section__title">参数配置</h2>
            <div v-for="pg in detail.paramGroups" :key="pg.group.id" class="param-group">
              <div class="param-group__title">{{ pg.group.name }}</div>
              <div class="param-group__grid">
                <div v-for="d in pg.defs" :key="d.def.id" class="param-group__item">
                  <span class="param-group__label">{{ d.def.name }}</span>
                  <span class="param-group__value">
                    {{ d.value || '-' }}
                    <span v-if="d.value && d.def.unit" class="rh-text-light">{{ d.def.unit }}</span>
                  </span>
                </div>
              </div>
            </div>
            <router-link :to="'/robot/' + id + '/params'" class="detail-more">
              查看完整参数配置 ›
            </router-link>
            <div v-if="detail.paramGroups && detail.paramGroups.length" class="detail-correction-entry">
              <el-button type="info" text size="small" @click="openCorrection(null, '', '')">参数有误？提交纠错 ›</el-button>
            </div>
          </section>
          <!-- 兼容旧版 mainParams -->
          <section v-else-if="paramPreview.length" class="rh-card rh-section">
            <h2 class="rh-section__title">核心参数</h2>
            <div class="param-preview">
              <div v-for="p in paramPreview" :key="p.label" class="param-preview__item">
                <span class="param-preview__label">{{ p.label }}</span>
                <span class="param-preview__value">{{ p.value }}</span>
              </div>
            </div>
            <router-link :to="'/robot/' + id + '/params'" class="detail-more">
              查看完整参数配置 ›
            </router-link>
            <div v-if="detail.paramGroups && detail.paramGroups.length" class="detail-correction-entry">
              <el-button type="info" text size="small" @click="openCorrection(null, '', '')">参数有误？提交纠错 ›</el-button>
            </div>
          </section>
          <section v-else class="rh-card rh-section">
            <h2 class="rh-section__title">核心参数</h2>
            <div class="rh-empty">暂无参数</div>
          </section>

          <section v-if="detail.robot.detail" class="rh-card rh-section">
            <h2 class="rh-section__title">图文详情</h2>
            <div class="rich-text" v-html="XssUtil.clean(detail.robot.detail)" />
          </section>

          <section class="rh-card rh-section">
            <CommentPanel biz-type="robot" :biz-id="id" />
          </section>

          <!-- Phase7: 用户口碑 -->
          <section class="rh-card rh-section">
            <h2 class="rh-section__title">用户口碑</h2>
            <div v-if="reviewLoading" class="rh-empty">加载中…</div>
            <div v-else-if="reviewSummary && reviewSummary.reviewCount > 0" class="review-summary">
              <div class="review-summary__overall">
                <div class="review-summary__score">{{ formatScore(reviewSummary.overallAvg) }}</div>
                <div class="review-summary__meta">
                  <el-rate :model-value="Number(reviewSummary.overallAvg)" disabled show-score text-color="#ff9900" score-template="{value}" />
                  <span class="rh-text-light">{{ reviewSummary.reviewCount }} 条评价</span>
                </div>
              </div>
              <div class="review-summary__dims">
                <div v-if="reviewSummary.qualityAvg" class="review-dim">
                  <span class="review-dim__label">质量</span>
                  <el-rate :model-value="Number(reviewSummary.qualityAvg)" disabled size="small" />
                  <span class="review-dim__val">{{ formatScore(reviewSummary.qualityAvg) }}</span>
                </div>
                <div v-if="reviewSummary.serviceAvg" class="review-dim">
                  <span class="review-dim__label">服务</span>
                  <el-rate :model-value="Number(reviewSummary.serviceAvg)" disabled size="small" />
                  <span class="review-dim__val">{{ formatScore(reviewSummary.serviceAvg) }}</span>
                </div>
                <div v-if="reviewSummary.costAvg" class="review-dim">
                  <span class="review-dim__label">性价比</span>
                  <el-rate :model-value="Number(reviewSummary.costAvg)" disabled size="small" />
                  <span class="review-dim__val">{{ formatScore(reviewSummary.costAvg) }}</span>
                </div>
              </div>
              <div class="review-summary__dist">
                <div class="review-dist__row"><span>5星</span><el-progress :percentage="scorePercent(reviewSummary.score5Count)" :stroke-width="8" /></div>
                <div class="review-dist__row"><span>4星</span><el-progress :percentage="scorePercent(reviewSummary.score4Count)" :stroke-width="8" color="#67c23a" /></div>
                <div class="review-dist__row"><span>3星</span><el-progress :percentage="scorePercent(reviewSummary.score3Count)" :stroke-width="8" color="#e6a23c" /></div>
                <div class="review-dist__row"><span>2星</span><el-progress :percentage="scorePercent(reviewSummary.score2Count)" :stroke-width="8" color="#f56c6c" /></div>
                <div class="review-dist__row"><span>1星</span><el-progress :percentage="scorePercent(reviewSummary.score1Count)" :stroke-width="8" color="#909399" /></div>
              </div>
              <router-link :to="'/robot/' + id + '/reviews'" class="detail-more">查看全部评价 ›</router-link>
            </div>
            <div v-else class="review-empty">
              <p>暂无用户口碑</p>
              <el-button type="primary" text @click="$router.push('/robot/' + id + '/reviews')">成为第一个评价的人</el-button>
            </div>
          </section>
        </div>

        <div>
          <!-- 同品牌机器人 -->
          <section v-if="detail.sameBrandRobots && detail.sameBrandRobots.length" class="rh-card rh-section">
            <h2 class="rh-section__title">同品牌产品</h2>
            <div class="same-brand-grid">
              <router-link
                v-for="sr in detail.sameBrandRobots"
                :key="sr.id"
                :to="'/robot/' + sr.id"
                class="same-brand-item"
              >
                <img :src="imageOf(sr.coverImage)" :alt="sr.name" loading="lazy" />
                <div class="same-brand-item__name">{{ sr.name }}</div>
                <div class="same-brand-item__price">{{ formatPrice(sr.guidePrice) }}</div>
              </router-link>
            </div>
          </section>

          <!-- Phase7: 相似机器人 -->
          <section v-if="similarRobots.length" class="rh-card rh-section">
            <h2 class="rh-section__title">相似机器人</h2>
            <div class="similar-grid">
              <router-link
                v-for="sr in similarRobots"
                :key="sr.robotId"
                :to="'/robot/' + sr.robotId"
                class="similar-item"
              >
                <img :src="sr.imageUrl" :alt="sr.robotName" loading="lazy" />
                <div class="similar-item__info">
                  <div class="similar-item__name">{{ sr.robotName }}</div>
                  <div class="similar-item__meta">
                    <span v-if="sr.categoryName" class="rh-text-light">{{ sr.categoryName }}</span>
                    <span v-if="sr.brandName" class="rh-text-light">· {{ sr.brandName }}</span>
                  </div>
                  <div class="similar-item__bottom">
                    <span class="similar-item__price">{{ formatPrice(sr.price) }}</span>
                    <span v-if="sr.totalScore" class="similar-item__score">{{ formatScore(sr.totalScore) }}分</span>
                  </div>
                  <div v-if="sr.reason" class="similar-item__reason">{{ sr.reason }}</div>
                </div>
              </router-link>
            </div>
          </section>

          <section v-if="detail.videos && detail.videos.length" class="rh-card rh-section">
            <h2 class="rh-section__title">视频</h2>
            <router-link
              v-for="video in detail.videos"
              :key="video.id"
              :to="'/robot/' + id + '/videos'"
              class="side-video"
            >
              <img :src="imageOf(video.cover)" :alt="video.title" loading="lazy" />
              <span class="rh-ellipsis">{{ video.title }}</span>
            </router-link>
          </section>

          <!-- 相关视频(Phase6) -->
          <section v-if="detail.relatedVideos && detail.relatedVideos.length" class="rh-card rh-section">
            <h2 class="rh-section__title">相关视频</h2>
            <router-link
              v-for="rv in detail.relatedVideos"
              :key="rv.id"
              :to="'/video/' + rv.id"
              class="side-article"
            >
              <img :src="imageOf(rv.cover)" :alt="rv.title" loading="lazy" />
              <span class="rh-clamp-2">{{ rv.title }}</span>
            </router-link>
          </section>

          <section v-if="detail.articles && detail.articles.length" class="rh-card rh-section">
            <h2 class="rh-section__title">相关资讯</h2>
            <router-link
              v-for="article in detail.articles"
              :key="article.id"
              :to="'/article/' + article.id"
              class="side-article"
            >
              <img :src="imageOf(article.cover)" :alt="article.title" loading="lazy" />
              <span class="rh-clamp-2">{{ article.title }}</span>
            </router-link>
          </section>

          <section v-if="detail.brandId" class="rh-card rh-section">
            <h2 class="rh-section__title">所属品牌</h2>
            <router-link :to="'/brand/' + detail.brandId" class="side-brand">
              <img :src="imageOf(detail.brandLogo)" :alt="detail.brandName" loading="lazy" />
              <div>
                <div class="side-brand__name">{{ detail.brandName }}</div>
                <div class="rh-text-light">查看品牌全部产品 ›</div>
              </div>
            </router-link>
          </section>
        </div>
      </div>
    </div>

    <div v-else class="rh-container">
      <div class="rh-empty">加载中…</div>
    </div>

    <!-- Phase7: 参数纠错 Dialog -->
    <el-dialog v-model="correctionVisible" title="提交参数纠错" width="520px" destroy-on-close>
      <el-form :model="correctionForm" label-width="80px" ref="correctionFormRef" @submit.prevent="submitCorrection">
        <el-form-item label="参数项">
          <el-input v-model="correctionForm.defName" disabled />
        </el-form-item>
        <el-form-item label="当前值">
          <el-input v-model="correctionForm.oldValue" disabled />
        </el-form-item>
        <el-form-item label="正确值" required>
          <el-input v-model="correctionForm.newValue" placeholder="请输入正确的参数值" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="纠错理由" required>
          <el-input v-model="correctionForm.reason" type="textarea" :rows="3" placeholder="请说明纠错理由（5-500字）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctionVisible = false">取消</el-button>
        <el-button type="primary" :loading="correctionSubmitting" @click="submitCorrection">提交</el-button>
      </template>
    </el-dialog>

    <!-- Phase7: 询价/采购 Dialog V2 -->
    <el-dialog v-model="procureVisible" :title="procureTitle" width="560px" destroy-on-close>
      <el-form :model="procureForm" label-width="90px" :rules="procureRules" ref="procureFormRef" @submit.prevent="submitProcure">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="procureForm.name" placeholder="您的姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="procureForm.phone" placeholder="手机号码" maxlength="11" />
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="procureForm.region" placeholder="省/市" maxlength="64" />
        </el-form-item>
        <el-form-item label="客户类型">
          <el-radio-group v-model="procureForm.customerType">
            <el-radio :value="1">个人</el-radio>
            <el-radio :value="2">企业</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="procureForm.customerType === 2" label="公司名称">
          <el-input v-model="procureForm.companyName" placeholder="公司全称" maxlength="128" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="procureForm.quantity" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input v-model="procureForm.budget" placeholder="预算范围" maxlength="64" />
        </el-form-item>
        <el-form-item v-if="procureForm.inquiryType === 'PURCHASE' || procureForm.inquiryType === 'LEASE'" label="使用场景">
          <el-select v-model="procureForm.procurementScene" placeholder="请选择使用场景" clearable>
            <el-option label="工业制造" value="INDUSTRIAL" />
            <el-option label="物流仓储" value="LOGISTICS" />
            <el-option label="医疗健康" value="MEDICAL" />
            <el-option label="教育培训" value="EDUCATION" />
            <el-option label="服务行业" value="SERVICE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="procureForm.inquiryType === 'PURCHASE'" label="采购时间">
          <el-input v-model="procureForm.purchaseTime" placeholder="预计采购时间" maxlength="64" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="procureForm.remark" type="textarea" :rows="3" placeholder="其他需求说明" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="procureVisible = false">取消</el-button>
        <el-button type="primary" :loading="procureSubmitting" @click="submitProcure">提交</el-button>
      </template>
    </el-dialog>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import MainLayout from '@/layout/MainLayout.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { robotApi, favoriteApi, likeApi, behaviorApi, inquiryApi, reviewApi, similarApi, correctionApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useCompareStore } from '@/store/compare'
import { formatPrice, formatCount, imageOf, parseMainParams, formatDate } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'
import { XssUtil } from '@/utils/xss'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const compareStore = useCompareStore()

const id = computed(() => Number(route.params.id))
const detail = ref(null)
const liked = ref(false)
const likeCount = ref(0)

const activeIdx = ref(0)

// Phase7: Review Summary
const reviewSummary = ref(null)
const reviewLoading = ref(false)

// Phase7: Similar Robots
const similarRobots = ref([])
const similarLoading = ref(false)

// Phase7: Correction Dialog
const correctionVisible = ref(false)
const correctionSubmitting = ref(false)
const correctionForm = ref({ defId: null, defName: '', oldValue: '', newValue: '', reason: '' })
const correctionRules = {
  defId: [{ required: true, message: '请选择参数项', trigger: 'change' }],
  newValue: [{ required: true, message: '请输入正确值', trigger: 'blur' }, { max: 500, message: '长度不超过500', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入纠错理由', trigger: 'blur' }, { min: 5, max: 500, message: '理由长度5-500字', trigger: 'blur' }]
}
const correctionFormRef = ref(null)

// Phase7: Procurement V2 Dialog
const procureVisible = ref(false)
const procureType = ref('PRICE')
const procureSubmitting = ref(false)
const procureForm = ref({ name: '', phone: '', region: '', customerType: 1, companyName: '', quantity: 1, budget: '', remark: '', inquiryType: 'PRICE', procurementScene: '', purchaseTime: '' })
const procureRules = {
  name: [{ required: true, message: '请填写姓名', trigger: 'blur' }, { max: 32, message: '姓名过长', trigger: 'blur' }],
  phone: [{ required: true, message: '请填写手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  quantity: [{ required: true, message: '请填写采购数量', trigger: 'blur' }],
  budget: [{ max: 64, message: '预算描述过长', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过500字', trigger: 'blur' }]
}
const procureFormRef = ref(null)

const procureTypeOptions = [
  { value: 'PRICE', label: '获取底价' },
  { value: 'PURCHASE', label: '我要采购' },
  { value: 'LEASE', label: '租赁咨询' },
  { value: 'COOPERATE', label: '合作洽谈' }
]
const procureSceneOptions = [
  { value: 'INDUSTRIAL', label: '工业制造' },
  { value: 'LOGISTICS', label: '物流仓储' },
  { value: 'MEDICAL', label: '医疗健康' },
  { value: 'EDUCATION', label: '教育科研' },
  { value: 'SERVICE', label: '商业服务' },
  { value: 'OTHER', label: '其他' }
]

const gallery = computed(() => {
  if (!detail.value) return []
  const images = (detail.value.images || []).map((i) => i.url).filter(Boolean)
  if (images.length) return images
  return [detail.value.robot.coverImage]
})

const galleryFull = computed(() => gallery.value.map((img) => imageOf(img)))

const activeImage = computed(() => {
  const imgs = gallery.value
  if (!imgs.length) return imageOf('')
  return imageOf(imgs[activeIdx.value] || imgs[0])
})

const paramPreview = computed(() => parseMainParams(detail.value && detail.value.robot.mainParams, 6))

const inCompare = computed(() => compareStore.has(id.value))

/** 核心参数速览卡片 */
const coreParams = computed(() => {
  if (!detail.value) return []
  const r = detail.value
  const items = []
  if (r.weight != null) items.push({ label: '重量', value: r.weight + ' kg' })
  if (r.payload != null) items.push({ label: '负载', value: r.payload + ' kg' })
  if (r.maxSpeed != null) items.push({ label: '最大速度', value: r.maxSpeed + ' m/s' })
  if (r.batteryLife != null) items.push({ label: '续航', value: r.batteryLife + ' h' })
  if (r.operatingTemp) items.push({ label: '工作温度', value: r.operatingTemp })
  if (r.protectionLevel) items.push({ label: '防护等级', value: r.protectionLevel })
  return items
})

/** 状态标签 */
const statusTag = computed(() => {
  const s = detail.value && detail.value.robot && detail.value.robot.status
  const map = {
    0: { label: '待上架', type: 'info' },
    1: { label: '在售', type: 'success' },
    2: { label: '停售', type: 'danger' },
    3: { label: '预售', type: 'warning' }
  }
  return s != null ? map[s] || null : null
})

/** 数据来源标签 */
const dataSourceLabel = computed(() => {
  const map = { DEMO: '示例数据', OFFICIAL: '官方数据', CRAWLER: '网络采集', MANUAL: '手动录入' }
  const ds = detail.value && detail.value.robot && detail.value.robot.dataSource
  return ds ? map[ds] || ds : ''
})

async function load () {
  const data = await robotApi.detail(id.value)
  // 图文详情由后台发布，仍做一次基础清洗以防历史脏数据
  if (data && data.robot && data.robot.detail) {
    data.robot.detail = XssUtil.clean(data.robot.detail)
  }
  detail.value = data
  activeIdx.value = 0
  likeCount.value = (data.robot && data.robot.likeCount) || 0
  await Promise.all([loadLikeState()])
  // SEO: 优先使用后端提供的SEO字段
  setPageMeta({
    title: (data.seoTitle || (data.robot.name + ' - 参数配置、图片、视频与口碑 - 机器人之家')),
    description: (data.seoDescription || (data.robot.subtitle || data.robot.name + ' 的详细参数、图片、视频与用户评价。')),
    keywords: (data.seoKeywords || (data.robot.name + ',' + (data.brandName || '') + ',机器人参数,机器人报价'))
  })
  // 行为上报: VIEW
  behaviorApi.track('VIEW', 'robot', id.value).catch(() => {})
  // Phase7: 加载Review Summary和Similar Robots
  loadReviewSummary()
  loadSimilarRobots()
}

async function loadLikeState () {
  if (!userStore.isLogin) return
  try {
    const data = await likeApi.check('robot', id.value)
    liked.value = !!(data && data.liked)
  } catch (e) {
    liked.value = false
  }
}

async function toggleFavorite () {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const data = await favoriteApi.toggle('robot', id.value)
  detail.value.favorited = data.favorited
  ElMessage.success(data.favorited ? '已收藏' : '已取消收藏')
}

async function toggleLike () {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const data = await likeApi.toggle('robot', id.value)
  liked.value = data.liked
  likeCount.value = likeCount.value + (data.liked ? 1 : -1)
}

function toggleCompare () {
  const result = compareStore.toggle(id.value)
  if (!result.ok) {
    ElMessage.warning(result.message)
    return
  }
  ElMessage.success(inCompare.value ? '已加入对比栏' : '已移出对比栏')
}

function goInquiry () {
  if (!userStore.isLogin) {
    ElMessage.warning('登录后询价更方便跟进')
  }
  router.push('/inquiry/' + id.value)
}

function onTab (tab) {
  const name = tab.props.name
  if (name === 'home') return
  router.push('/robot/' + id.value + '/' + name)
}

// ========== Phase7: Review Summary ==========
async function loadReviewSummary () {
  reviewLoading.value = true
  try {
    reviewSummary.value = await reviewApi.summary(id.value)
  } catch (e) {
    reviewSummary.value = null
  } finally {
    reviewLoading.value = false
  }
}

/** 评分分布百分比 */
function scorePercent (count) {
  if (!reviewSummary.value || !reviewSummary.value.reviewCount) return 0
  return Math.round((count / reviewSummary.value.reviewCount) * 100)
}

/** 格式化评分 */
function formatScore (val) {
  if (val == null) return '-'
  return Number(val).toFixed(1)
}

// ========== Phase7: Similar Robots ==========
async function loadSimilarRobots () {
  similarLoading.value = true
  try {
    const data = await similarApi.list(id.value, 6)
    similarRobots.value = data || []
  } catch (e) {
    similarRobots.value = []
  } finally {
    similarLoading.value = false
  }
}

// ========== Phase7: Correction ==========
function openCorrection (defId, paramName, currentValue) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  correctionForm.value = { defId, defName: paramName || '', oldValue: currentValue || '', newValue: currentValue || '', reason: '' }
  correctionVisible.value = true
}

async function submitCorrection () {
  if (!correctionFormRef.value) return
  if (!correctionForm.value.newValue || !correctionForm.value.reason) {
    ElMessage.warning('请填写正确值和纠错理由')
    return
  }
  if (correctionForm.value.reason.length < 5) {
    ElMessage.warning('纠错理由至少5个字')
    return
  }
  correctionSubmitting.value = true
  try {
    await correctionApi.submit({
      robotId: id.value,
      defId: correctionForm.value.defId,
      newValue: correctionForm.value.newValue,
      reason: correctionForm.value.reason
    })
    ElMessage.success('纠错已提交，等待审核')
    correctionVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    correctionSubmitting.value = false
  }
}

// ========== Phase7: Procurement V2 ==========
function openProcure (type) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  procureType.value = type || 'PRICE'
  procureForm.value = { name: '', phone: '', region: '', customerType: 1, companyName: '', quantity: 1, budget: '', remark: '', inquiryType: type || 'PRICE', procurementScene: '', purchaseTime: '' }
  procureVisible.value = true
}

const procureTypeLabel = computed(() => {
  const opt = procureTypeOptions.find(o => o.value === procureType.value)
  return opt ? opt.label : '获取报价'
})
const procureTitle = procureTypeLabel

async function submitProcure () {
  if (!procureFormRef.value) return
  await procureFormRef.value.validate()
  procureSubmitting.value = true
  try {
    await inquiryApi.submit({
      robotId: id.value,
      name: procureForm.value.name,
      phone: procureForm.value.phone,
      region: procureForm.value.region,
      customerType: procureForm.value.customerType,
      companyName: procureForm.value.companyName,
      quantity: procureForm.value.quantity,
      budget: procureForm.value.budget,
      remark: procureForm.value.remark,
      inquiryType: procureType.value,
      procurementScene: procureForm.value.procurementScene,
      purchaseTime: procureForm.value.purchaseTime
    })
    ElMessage.success('提交成功，我们会尽快联系您')
    procureVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    procureSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.detail-head {
  display: grid;
  grid-template-columns: 480px 1fr;
  gap: 28px;
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 20px;
  margin-bottom: 16px;
}

.detail-head__main {
  width: 100%;
  height: 340px;
  border-radius: var(--rh-radius);
  background: var(--rh-surface-sub);
  cursor: zoom-in;
}

.detail-head__thumbs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
}

.detail-head__thumbs img {
  width: 78px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  flex-shrink: 0;
}

.detail-head__thumbs img.is-active {
  border-color: var(--rh-primary);
}

.detail-head__brand {
  font-size: 13px;
  color: var(--rh-primary);
}

.detail-head__name {
  font-size: 26px;
  margin: 6px 0 4px;
}

.detail-head__subtitle {
  color: var(--rh-text-sub);
  margin: 0 0 16px;
}

.detail-head__meta {
  display: flex;
  gap: 14px;
  align-items: center;
  font-size: 13px;
  color: var(--rh-text-sub);
  margin-bottom: 8px;
}

.detail-head__core-params {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 12px 0;
}

.core-param {
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
  padding: 10px 12px;
  text-align: center;
}

.core-param__label {
  display: block;
  font-size: 11px;
  color: var(--rh-text-light);
}

.core-param__value {
  display: block;
  font-size: 15px;
  font-weight: 600;
  margin-top: 2px;
}

.detail-head__source {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--rh-text-light);
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px dashed var(--rh-border-light);
}

.detail-head__price {
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
  padding: 12px 16px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.detail-head__price-label {
  font-size: 13px;
  color: var(--rh-text-sub);
}

.detail-head__price-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--rh-danger);
}

.detail-head__market {
  font-size: 13px;
  color: var(--rh-text-light);
  text-decoration: line-through;
}

.detail-head__tags {
  margin: 14px 0;
}

.detail-head__stats {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: var(--rh-text-light);
  margin-bottom: 16px;
}

.detail-head__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-head__channels {
  margin-top: 18px;
  border-top: 1px solid var(--rh-border-light);
  padding-top: 12px;
}

.detail-head__channels-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.detail-head__channel {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 4px 0;
  color: var(--rh-text-sub);
}

.detail-tabs {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 0 18px;
  margin-bottom: 16px;
}

.detail-body {
  align-items: start;
}

.param-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.param-preview__item {
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
  padding: 12px;
  text-align: center;
}

.param-preview__label {
  display: block;
  font-size: 12px;
  color: var(--rh-text-light);
}

.param-preview__value {
  display: block;
  font-size: 16px;
  font-weight: 600;
  margin-top: 2px;
}

.detail-more {
  display: block;
  text-align: center;
  margin-top: 14px;
  color: var(--rh-primary);
  font-size: 13px;
}

.param-group {
  margin-bottom: 16px;
}

.param-group__title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid var(--rh-primary);
}

.param-group__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.param-group__item {
  display: flex;
  justify-content: space-between;
  padding: 6px 10px;
  background: var(--rh-surface-sub);
  border-radius: 4px;
  font-size: 13px;
}

.param-group__label {
  color: var(--rh-text-sub);
}

.param-group__value {
  font-weight: 500;
}

.same-brand-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.same-brand-item {
  display: flex;
  flex-direction: column;
  border-radius: var(--rh-radius);
  overflow: hidden;
  background: var(--rh-surface-sub);
  transition: box-shadow 0.2s;
}

.same-brand-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.same-brand-item img {
  width: 100%;
  height: 100px;
  object-fit: cover;
}

.same-brand-item__name {
  font-size: 13px;
  font-weight: 500;
  padding: 6px 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.same-brand-item__price {
  font-size: 12px;
  color: var(--rh-danger);
  padding: 2px 8px 6px;
}

.rich-text {
  font-size: 14px;
  line-height: 1.9;
  color: var(--rh-text);
}

.rich-text :deep(img) {
  max-width: 100%;
}

.side-video,
.side-article {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--rh-border-light);
  font-size: 13px;
}

.side-video:last-child,
.side-article:last-child {
  border-bottom: none;
}

.side-video img,
.side-article img {
  width: 96px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.side-video:hover span,
.side-article:hover span {
  color: var(--rh-primary);
}

.side-brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.side-brand img {
  width: 56px;
  height: 56px;
  object-fit: contain;
  border-radius: 8px;
}

.side-brand__name {
  font-size: 15px;
  font-weight: 600;
}

/* Phase7: Review Summary */
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

.review-empty p {
  margin-bottom: 8px;
}

/* Phase7: Similar Robots */
.similar-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.similar-item {
  display: flex;
  gap: 10px;
  padding: 8px;
  border-radius: var(--rh-radius);
  transition: background 0.2s;
}

.similar-item:hover {
  background: var(--rh-surface-sub);
}

.similar-item img {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.similar-item__info {
  flex: 1;
  min-width: 0;
}

.similar-item__name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.similar-item__meta {
  font-size: 12px;
  margin-top: 2px;
}

.similar-item__bottom {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.similar-item__price {
  font-size: 13px;
  color: var(--rh-danger);
  font-weight: 500;
}

.similar-item__score {
  font-size: 12px;
  color: var(--rh-primary);
}

.similar-item__reason {
  font-size: 12px;
  color: var(--rh-text-sub);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Phase7: Correction Entry */
.detail-correction-entry {
  margin-top: 8px;
  text-align: right;
}

@media (max-width: 768px) {
  .detail-head {
    grid-template-columns: 1fr;
  }
  .detail-head__core-params {
    grid-template-columns: repeat(2, 1fr);
  }
  .param-group__grid {
    grid-template-columns: 1fr;
  }
  .same-brand-grid {
    grid-template-columns: 1fr;
  }
  .param-preview {
    grid-template-columns: repeat(2, 1fr);
  }
  .review-summary__dims {
    flex-direction: column;
  }
}
</style>
