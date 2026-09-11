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
            <el-button type="primary" size="large" @click="goInquiry">获取底价</el-button>
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
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import MainLayout from '@/layout/MainLayout.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { robotApi, favoriteApi, likeApi, behaviorApi } from '@/api'
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
}
</style>
