<template>
  <MainLayout>
    <div v-if="page" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'brands' }">品牌库</el-breadcrumb-item>
        <el-breadcrumb-item>{{ page.brand.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <section class="rh-card brand-head">
        <img :src="imageOf(page.brand.logo)" :alt="page.brand.name" class="brand-head__logo" loading="lazy" />
        <div class="brand-head__body">
          <h1 class="brand-head__name">{{ page.brand.name }}</h1>
          <div class="brand-head__meta">
            <span v-if="page.brand.country">{{ page.brand.country }}</span>
            <span v-if="page.brand.foundYear">{{ page.brand.foundYear }} 年成立</span>
            <span v-if="page.brand.companyId">
              所属企业：
              <router-link :to="'/company/' + page.brand.companyId">{{ page.brand.companyName }}</router-link>
            </span>
            <span>{{ page.robotCount || 0 }} 款产品</span>
            <span v-if="page.followCount">{{ page.followCount }} 人关注</span>
          </div>
          <p class="brand-head__intro">{{ page.brand.intro }}</p>
          <div class="brand-head__actions">
            <el-button
              size="default"
              :type="followed ? 'primary' : 'default'"
              plain
              @click="toggleFollow"
            >
              {{ followed ? '已关注' : '关注品牌' }}
            </el-button>
            <el-button v-if="page.brand.website" size="default" text @click="openSite">
              访问官网
            </el-button>
          </div>
        </div>
      </section>

      <!-- 热门机器人 -->
      <section v-if="page.hotRobots && page.hotRobots.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">热门机器人</h2>
        </div>
        <div class="rh-grid rh-grid--5">
          <RobotCard v-for="robot in page.hotRobots" :key="robot.id" :robot="robot" />
        </div>
      </section>

      <!-- 新品机器人 -->
      <section v-if="page.newRobots && page.newRobots.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">新品机器人</h2>
        </div>
        <div class="rh-grid rh-grid--5">
          <RobotCard v-for="robot in page.newRobots" :key="robot.id" :robot="robot" />
        </div>
      </section>

      <!-- 全部机器人(分页) -->
      <section class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">旗下机器人（{{ page.robotCount || 0 }}）</h2>
        </div>
        <div v-if="!products.length" class="rh-empty">暂无产品</div>
        <div v-else class="rh-grid rh-grid--5">
          <RobotCard v-for="robot in products" :key="robot.id" :robot="robot" />
        </div>
        <el-pagination
          v-if="productTotal > productPageSize"
          background
          layout="total, prev, pager, next"
          :current-page="productPageNum"
          :page-size="productPageSize"
          :total="productTotal"
          @current-change="changeProductPage"
        />
      </section>

      <!-- 相关文章 -->
      <section v-if="page.articles && page.articles.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">相关文章（{{ page.articleCount || 0 }}）</h2>
        </div>
        <div class="rh-grid rh-grid--3">
          <div v-for="a in page.articles" :key="a.id" class="rh-card article-card" @click="$router.push('/article/' + a.id)">
            <img v-if="a.coverImage" :src="imageOf(a.coverImage)" class="article-card__cover" loading="lazy" />
            <div class="article-card__body">
              <h3 class="article-card__title">{{ a.title }}</h3>
              <span class="article-card__meta">{{ a.viewCount || 0 }} 阅读</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 相关评测 -->
      <section v-if="page.reviews && page.reviews.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">相关评测（{{ page.reviewCount || 0 }}）</h2>
        </div>
        <div class="rh-list">
          <div v-for="r in page.reviews" :key="r.id" class="rh-card review-card">
            <span class="review-card__score">{{ r.score }}</span>
            <span class="review-card__title">{{ r.title }}</span>
            <span class="review-card__meta">{{ r.viewCount || 0 }} 阅读</span>
          </div>
        </div>
      </section>

      <!-- 社区讨论 -->
      <section v-if="page.posts && page.posts.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">社区讨论</h2>
        </div>
        <div class="rh-list">
          <div v-for="p in page.posts" :key="p.id" class="rh-card post-card" @click="$router.push('/community/' + p.id)">
            <span class="post-card__title">{{ p.title }}</span>
            <span class="post-card__meta">{{ p.likeCount || 0 }} 赞 · {{ p.commentCount || 0 }} 评论</span>
          </div>
        </div>
      </section>
    </div>
    <div v-else class="rh-container"><div class="rh-empty">加载中…</div></div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import RobotCard from '@/components/RobotCard.vue'
import { brandApi, followApi } from '@/api'
import { useUserStore } from '@/store/user'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))
const page = ref(null)
const products = ref([])
const productPageNum = ref(1)
const productPageSize = ref(10)
const productTotal = ref(0)
const followed = ref(false)

async function load () {
  page.value = await brandApi.brandPage(id.value)
  followed.value = !!page.value.followed
  setPageMeta({
    title: page.value.brand.name + ' - 品牌介绍与旗下机器人 - 机器人之家',
    description: page.value.brand.intro || page.value.brand.name + ' 品牌介绍、旗下机器人产品与报价。',
    keywords: page.value.brand.name + ',机器人品牌,机器人产品'
  })
  await loadProducts()
}

async function loadProducts () {
  const data = await brandApi.robots(id.value, {
    pageNum: productPageNum.value,
    pageSize: productPageSize.value
  })
  products.value = data.list || []
  productTotal.value = data.total || 0
}

async function toggleFollow () {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const data = await followApi.toggle('brand', id.value)
  followed.value = data.followed
  ElMessage.success(data.followed ? '已关注' : '已取消关注')
}

function openSite () {
  window.open(page.value.brand.website, '_blank')
}

function changeProductPage (p) {
  productPageNum.value = p
  loadProducts()
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.brand-head {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
}

.brand-head__logo {
  width: 120px;
  height: 120px;
  object-fit: contain;
  border-radius: 12px;
  background: var(--rh-surface-sub);
  flex-shrink: 0;
}

.brand-head__name {
  font-size: 24px;
  margin: 0 0 8px;
}

.brand-head__meta {
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: var(--rh-text-sub);
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.brand-head__meta a {
  color: var(--rh-primary);
}

.brand-head__intro {
  color: var(--rh-text-sub);
  margin: 0 0 14px;
  line-height: 1.8;
}

.brand-head__actions {
  display: flex;
  gap: 10px;
}

.article-card {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.article-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.article-card__cover {
  width: 100%;
  height: 140px;
  object-fit: cover;
  border-radius: 8px 8px 0 0;
}
.article-card__body {
  padding: 10px 12px;
}
.article-card__title {
  font-size: 14px;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.article-card__meta {
  font-size: 12px;
  color: var(--rh-text-sub);
}

.review-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 8px;
}
.review-card__score {
  font-size: 20px;
  font-weight: 700;
  color: var(--rh-primary);
  min-width: 36px;
}
.review-card__title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.review-card__meta {
  font-size: 12px;
  color: var(--rh-text-sub);
}

.post-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 8px;
  cursor: pointer;
}
.post-card__title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.post-card__meta {
  font-size: 12px;
  color: var(--rh-text-sub);
  white-space: nowrap;
}
</style>
