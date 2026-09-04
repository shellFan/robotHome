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
          <img :src="activeImage" :alt="detail.robot.name" class="detail-head__main" />
          <div v-if="gallery.length > 1" class="detail-head__thumbs">
            <img
              v-for="(img, idx) in gallery"
              :key="idx"
              :src="imageOf(img)"
              :class="{ 'is-active': activeImage === imageOf(img) }"
              @click="activeImage = imageOf(img)"
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
          <p class="detail-head__subtitle">{{ detail.robot.subtitle }}</p>

          <div class="detail-head__price">
            <span class="detail-head__price-label">指导价</span>
            <span class="detail-head__price-value">{{ formatPrice(detail.robot.guidePrice) }}</span>
            <span v-if="detail.robot.marketPrice" class="detail-head__market">
              市场价 {{ formatPrice(detail.robot.marketPrice) }}
            </span>
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
          <section class="rh-card rh-section">
            <h2 class="rh-section__title">核心参数</h2>
            <div v-if="paramPreview.length" class="param-preview">
              <div v-for="p in paramPreview" :key="p.label" class="param-preview__item">
                <span class="param-preview__label">{{ p.label }}</span>
                <span class="param-preview__value">{{ p.value }}</span>
              </div>
            </div>
            <div v-else class="rh-empty">暂无参数</div>
            <router-link :to="'/robot/' + id + '/params'" class="detail-more">
              查看完整参数配置 ›
            </router-link>
          </section>

          <section v-if="detail.robot.detail" class="rh-card rh-section">
            <h2 class="rh-section__title">图文详情</h2>
            <div class="rich-text" v-html="detail.robot.detail" />
          </section>

          <section class="rh-card rh-section">
            <CommentPanel biz-type="robot" :biz-id="id" />
          </section>
        </div>

        <div>
          <section v-if="detail.videos && detail.videos.length" class="rh-card rh-section">
            <h2 class="rh-section__title">视频</h2>
            <router-link
              v-for="video in detail.videos"
              :key="video.id"
              :to="'/robot/' + id + '/videos'"
              class="side-video"
            >
              <img :src="imageOf(video.cover)" :alt="video.title" />
              <span class="rh-ellipsis">{{ video.title }}</span>
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
              <img :src="imageOf(article.cover)" :alt="article.title" />
              <span class="rh-clamp-2">{{ article.title }}</span>
            </router-link>
          </section>

          <section v-if="detail.brandId" class="rh-card rh-section">
            <h2 class="rh-section__title">所属品牌</h2>
            <router-link :to="'/brand/' + detail.brandId" class="side-brand">
              <img :src="imageOf(detail.brandLogo)" :alt="detail.brandName" />
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
import MainLayout from '@/layout/MainLayout.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { robotApi, favoriteApi, likeApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useCompareStore } from '@/store/compare'
import { formatPrice, formatCount, imageOf, parseMainParams } from '@/utils/format'
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

const activeImage = ref('')

const gallery = computed(() => {
  if (!detail.value) return []
  const images = (detail.value.images || []).map((i) => i.url).filter(Boolean)
  if (images.length) return images
  return [detail.value.robot.coverImage]
})

const paramPreview = computed(() => parseMainParams(detail.value && detail.value.robot.mainParams, 6))

const inCompare = computed(() => compareStore.has(id.value))

async function load () {
  const data = await robotApi.detail(id.value)
  // 图文详情由后台发布，仍做一次基础清洗以防历史脏数据
  if (data && data.robot && data.robot.detail) {
    data.robot.detail = XssUtil.clean(data.robot.detail)
  }
  detail.value = data
  activeImage.value = gallery.value.length ? imageOf(gallery.value[0]) : ''
  likeCount.value = (data.robot && data.robot.likeCount) || 0
  await Promise.all([loadLikeState()])
  setPageMeta({
    title: data.robot.name + ' - 参数配置、图片、视频与口碑 - 机器人之家',
    description: data.robot.subtitle || data.robot.name + ' 的详细参数、图片、视频与用户评价。',
    keywords: data.robot.name + ',' + (data.brandName || '') + ',机器人参数,机器人报价'
  })
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
  object-fit: cover;
  border-radius: var(--rh-radius);
  background: var(--rh-surface-sub);
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
</style>
