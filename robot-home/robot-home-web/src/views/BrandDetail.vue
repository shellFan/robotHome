<template>
  <MainLayout>
    <div v-if="detail" class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'brands' }">品牌库</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.brand.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <section class="rh-card brand-head">
        <img :src="imageOf(detail.brand.logo)" :alt="detail.brand.name" class="brand-head__logo" loading="lazy" />
        <div class="brand-head__body">
          <h1 class="brand-head__name">{{ detail.brand.name }}</h1>
          <div class="brand-head__meta">
            <span v-if="detail.brand.country">{{ detail.brand.country }}</span>
            <span v-if="detail.brand.foundYear">{{ detail.brand.foundYear }} 年成立</span>
            <span v-if="detail.companyName">
              所属企业：
              <router-link v-if="detail.brand.companyId" :to="'/company/' + detail.brand.companyId">
                {{ detail.companyName }}
              </router-link>
              <template v-else>{{ detail.companyName }}</template>
            </span>
            <span>{{ detail.productCount }} 款产品</span>
          </div>
          <p class="brand-head__intro">{{ detail.brand.intro }}</p>
          <div class="brand-head__actions">
            <el-button
              size="default"
              :type="followed ? 'primary' : 'default'"
              plain
              @click="toggleFollow"
            >
              {{ followed ? '已关注' : '关注品牌' }}
            </el-button>
            <el-button v-if="detail.brand.website" size="default" text @click="openSite">
              访问官网
            </el-button>
          </div>
        </div>
      </section>

      <section class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">旗下机器人（{{ detail.productCount }}）</h2>
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
const detail = ref(null)
const products = ref([])
const productPageNum = ref(1)
const productPageSize = ref(10)
const productTotal = ref(0)
const followed = ref(false)

async function load () {
  detail.value = await brandApi.detail(id.value)
  setPageMeta({
    title: detail.value.brand.name + ' - 品牌介绍与旗下机器人 - 机器人之家',
    description: detail.value.brand.intro || detail.value.brand.name + ' 品牌介绍、旗下机器人产品与报价。',
    keywords: detail.value.brand.name + ',机器人品牌,机器人产品'
  })
  await loadProducts()
  await loadFollow()
}

async function loadProducts () {
  const data = await brandApi.robots(id.value, {
    pageNum: productPageNum.value,
    pageSize: productPageSize.value
  })
  products.value = data.list || []
  productTotal.value = data.total || 0
}

async function loadFollow () {
  if (!userStore.isLogin) return
  try {
    const data = await followApi.check('brand', id.value)
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
  const data = await followApi.toggle('brand', id.value)
  followed.value = data.followed
  ElMessage.success(data.followed ? '已关注' : '已取消关注')
}

function openSite () {
  window.open(detail.value.brand.website, '_blank')
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
</style>
