<template>
  <MainLayout>
    <div v-if="page" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'companies' }">企业库</el-breadcrumb-item>
        <el-breadcrumb-item>{{ page.company.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <section class="rh-card company-head">
        <img :src="imageOf(page.company.logo)" :alt="page.company.name" class="company-head__logo" loading="lazy" />
        <div class="company-head__body">
          <h1 class="company-head__name">{{ page.company.name }}</h1>
          <div class="company-head__meta">
            <span v-if="page.company.region">{{ page.company.region }}</span>
            <span v-if="page.company.foundYear">{{ page.company.foundYear }} 年成立</span>
            <span>{{ page.brandCount || (page.brands || []).length }} 个品牌</span>
            <span>{{ page.robotCount || 0 }}+ 款产品</span>
            <span v-if="page.followCount">{{ page.followCount }} 人关注</span>
          </div>
          <p class="company-head__intro">{{ page.company.intro }}</p>
          <div v-if="page.company.tags" class="company-head__tags">
            <span v-for="tag in tags" :key="tag" class="rh-tag">{{ tag }}</span>
          </div>
          <div class="company-head__contact">
            <span v-if="page.company.website">官网：{{ page.company.website }}</span>
            <span v-if="page.company.contactPhone">电话：{{ page.company.contactPhone }}</span>
            <span v-if="page.company.contactEmail">邮箱：{{ page.company.contactEmail }}</span>
            <span v-if="page.company.address">地址：{{ page.company.address }}</span>
          </div>
          <el-button size="default" :type="followed ? 'primary' : 'default'" plain @click="toggleFollow">
            {{ followed ? '已关注' : '关注企业' }}
          </el-button>
        </div>
      </section>

      <!-- 旗下品牌 -->
      <section v-if="page.brands && page.brands.length" class="rh-section">
        <h2 class="rh-section__title">旗下品牌</h2>
        <div class="brand-row">
          <router-link
            v-for="brand in page.brands"
            :key="brand.id"
            :to="'/brand/' + brand.id"
            class="brand-row__item"
          >
            <img :src="imageOf(brand.logo)" :alt="brand.name" loading="lazy" />
            <span class="rh-ellipsis">{{ brand.name }}</span>
          </router-link>
        </div>
      </section>

      <!-- 热门机器人 -->
      <section v-if="page.hotRobots && page.hotRobots.length" class="rh-section">
        <h2 class="rh-section__title">热门机器人</h2>
        <div class="rh-grid rh-grid--5">
          <RobotCard v-for="robot in page.hotRobots" :key="robot.id" :robot="robot" />
        </div>
      </section>

      <!-- 全部产品(分页) -->
      <section class="rh-section">
        <h2 class="rh-section__title">全部产品</h2>
        <div v-if="!products.length" class="rh-empty">暂无产品</div>
        <div v-else class="rh-grid rh-grid--5">
          <RobotCard v-for="robot in products" :key="robot.id" :robot="robot" />
        </div>
        <el-pagination
          v-if="productTotal > pageSize"
          background
          layout="total, prev, pager, next"
          :current-page="pageNum"
          :page-size="pageSize"
          :total="productTotal"
          @current-change="changePage"
        />
      </section>

      <!-- 相关文章 -->
      <section v-if="page.articles && page.articles.length" class="rh-section">
        <h2 class="rh-section__title">相关文章（{{ page.articleCount || 0 }}）</h2>
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

      <!-- 社区讨论 -->
      <section v-if="page.posts && page.posts.length" class="rh-section">
        <h2 class="rh-section__title">社区讨论</h2>
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
import { companyApi, followApi } from '@/api'
import { useUserStore } from '@/store/user'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => Number(route.params.id))
const page = ref(null)
const products = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const productTotal = ref(0)
const followed = ref(false)

const tags = computed(() => {
  const raw = page.value && page.value.company.tags
  if (!raw) return []
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
})

async function load () {
  page.value = await companyApi.companyPage(id.value)
  followed.value = !!page.value.followed
  setPageMeta({
    title: page.value.company.name + ' - 企业介绍与产品 - 机器人之家',
    description: page.value.company.intro || page.value.company.name + ' 企业介绍、旗下品牌与机器人产品。'
  })
  await loadProducts()
}

async function loadProducts () {
  const data = await companyApi.robots(id.value, { pageNum: pageNum.value, pageSize: pageSize.value })
  products.value = data.list || []
  productTotal.value = data.total || 0
}

async function toggleFollow () {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const data = await followApi.toggle('company', id.value)
  followed.value = data.followed
  ElMessage.success(data.followed ? '已关注' : '已取消关注')
}

function changePage (p) {
  pageNum.value = p
  loadProducts()
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.company-head {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
}

.company-head__logo {
  width: 120px;
  height: 120px;
  object-fit: contain;
  border-radius: 12px;
  background: var(--rh-surface-sub);
  flex-shrink: 0;
}

.company-head__name {
  font-size: 24px;
  margin: 0 0 8px;
}

.company-head__meta {
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: var(--rh-text-sub);
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.company-head__intro {
  color: var(--rh-text-sub);
  margin: 0 0 10px;
  line-height: 1.8;
}

.company-head__tags {
  margin-bottom: 10px;
}

.company-head__contact {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: var(--rh-text-light);
  margin-bottom: 12px;
}

.brand-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 16px;
}

.brand-row__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border: 1px solid var(--rh-border);
  border-radius: var(--rh-radius);
  font-size: 13px;
  max-width: 180px;
}

.brand-row__item:hover {
  border-color: var(--rh-primary);
  color: var(--rh-primary);
}

.brand-row__item img {
  width: 32px;
  height: 32px;
  object-fit: contain;
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
