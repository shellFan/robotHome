<template>
  <MainLayout>
    <div v-if="detail" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'companies' }">企业库</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.company.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <section class="rh-card company-head">
        <img :src="imageOf(detail.company.logo)" :alt="detail.company.name" class="company-head__logo" />
        <div class="company-head__body">
          <h1 class="company-head__name">{{ detail.company.name }}</h1>
          <div class="company-head__meta">
            <span v-if="detail.company.region">{{ detail.company.region }}</span>
            <span v-if="detail.company.foundYear">{{ detail.company.foundYear }} 年成立</span>
            <span>{{ (detail.brandList || []).length }} 个品牌</span>
            <span>{{ (detail.productList || []).length }}+ 款产品</span>
          </div>
          <p class="company-head__intro">{{ detail.company.intro }}</p>
          <div v-if="detail.company.tags" class="company-head__tags">
            <span v-for="tag in tags" :key="tag" class="rh-tag">{{ tag }}</span>
          </div>
          <div class="company-head__contact">
            <span v-if="detail.company.website">官网：{{ detail.company.website }}</span>
            <span v-if="detail.company.contactPhone">电话：{{ detail.company.contactPhone }}</span>
            <span v-if="detail.company.contactEmail">邮箱：{{ detail.company.contactEmail }}</span>
            <span v-if="detail.company.address">地址：{{ detail.company.address }}</span>
          </div>
          <el-button size="default" :type="followed ? 'primary' : 'default'" plain @click="toggleFollow">
            {{ followed ? '已关注' : '关注企业' }}
          </el-button>
        </div>
      </section>

      <section v-if="detail.brandList && detail.brandList.length" class="rh-section">
        <h2 class="rh-section__title">旗下品牌</h2>
        <div class="brand-row">
          <router-link
            v-for="brand in detail.brandList"
            :key="brand.id"
            :to="'/brand/' + brand.id"
            class="brand-row__item"
          >
            <img :src="imageOf(brand.logo)" :alt="brand.name" />
            <span class="rh-ellipsis">{{ brand.name }}</span>
          </router-link>
        </div>
      </section>

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
const detail = ref(null)
const products = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const productTotal = ref(0)
const followed = ref(false)

const tags = computed(() => {
  const raw = detail.value && detail.value.company.tags
  if (!raw) return []
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
})

async function load () {
  detail.value = await companyApi.detail(id.value)
  setPageMeta({
    title: detail.value.company.name + ' - 企业介绍与产品 - 机器人之家',
    description: detail.value.company.intro || detail.value.company.name + ' 企业介绍、旗下品牌与机器人产品。'
  })
  await loadProducts()
  await loadFollow()
}

async function loadProducts () {
  const data = await companyApi.robots(id.value, { pageNum: pageNum.value, pageSize: pageSize.value })
  products.value = data.list || []
  productTotal.value = data.total || 0
}

async function loadFollow () {
  if (!userStore.isLogin) return
  try {
    const data = await followApi.check('company', id.value)
    followed.value = !!(data && data.followed)
  } catch (e) {
    followed.value = false
  }
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
</style>
