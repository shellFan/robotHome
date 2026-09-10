<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>排行榜</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card">
        <div class="rank-head">
          <h2 class="rh-section__title">机器人排行榜</h2>
          <div class="rank-tabs">
            <span
              v-for="type in types"
              :key="type.code"
              class="sort-chip"
              :class="{ 'sort-chip--active': active === type.code }"
              @click="setType(type.code)"
            >
              {{ type.name }}
            </span>
          </div>
        </div>

        <div class="rank-note">
          排序依据热度权重算法：浏览量 ×1 + 收藏 ×5 + 评论 ×8 + 对比 ×6 + 询价 ×15 + 评分 ×100 + 新品加权（每 30 分钟重算一次）
        </div>

        <div v-if="loading" class="rh-empty">加载中…</div>
        <div v-else-if="!list.length" class="rh-empty">暂无榜单数据</div>
        <div v-else>
          <div class="rank-row rank-row--head">
            <span class="rank-row__no">排名</span>
            <span class="rank-row__name">机器人</span>
            <span class="rank-row__brand">品牌</span>
            <span class="rank-row__price">指导价</span>
            <span class="rank-row__score">评分</span>
            <span class="rank-row__stat">热度</span>
            <span class="rank-row__op">操作</span>
          </div>
          <router-link
            v-for="(robot, index) in list"
            :key="robot.id"
            :to="'/robot/' + robot.id"
            class="rank-row"
          >
            <span class="rank-row__no">
              <em :class="{ 'is-top': index < 3 }">{{ index + 1 }}</em>
            </span>
            <span class="rank-row__name">
              <img :src="imageOf(robot.coverImage)" :alt="robot.name" loading="lazy" />
              <b class="rh-ellipsis">{{ robot.name }}</b>
              <span class="rh-text-light rh-ellipsis">{{ robot.subtitle }}</span>
            </span>
            <span class="rank-row__brand">{{ robot.brandName || '-' }}</span>
            <span class="rank-row__price rh-price">{{ formatPrice(robot.guidePrice) }}</span>
            <span class="rank-row__score">{{ robot.score ? Number(robot.score).toFixed(1) : '-' }}</span>
            <span class="rank-row__stat">{{ formatCount(robot.hotScore) }}</span>
            <span class="rank-row__op">
              <el-button size="small" text type="primary" @click.prevent="addCompare(robot.id)">
                对比
              </el-button>
            </span>
          </router-link>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { rankingApi } from '@/api'
import { useCompareStore } from '@/store/compare'
import { formatPrice, formatCount, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()
const compareStore = useCompareStore()

const types = ref([])
const list = ref([])
const active = ref(route.query.type || 'hot')
const loading = ref(false)

async function loadTypes () {
  types.value = await rankingApi.types()
}

async function load () {
  loading.value = true
  try {
    list.value = await rankingApi.rank(active.value, 20)
  } finally {
    loading.value = false
  }
}

function setType (code) {
  active.value = code
  router.replace({ name: 'rankings', query: { type: code } })
  load()
}

function addCompare (id) {
  const res = compareStore.toggle(id)
  if (!res.ok) {
    ElMessage.warning(res.message)
    return
  }
  ElMessage.success(compareStore.has(id) ? '已加入对比栏' : '已移出对比栏')
}

onMounted(async () => {
  setPageMeta({
    title: '机器人排行榜 - 热门榜 / 人形机器人榜 / 机器狗榜 - 机器人之家',
    description: '基于浏览量、收藏、评论、对比、询价与评分的加权热度算法生成的机器人排行榜。'
  })
  await loadTypes()
  await load()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.rank-head {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.rank-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.sort-chip {
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: var(--rh-text-sub);
}

.sort-chip:hover {
  color: var(--rh-primary);
}

.sort-chip--active {
  background: var(--rh-primary);
  color: #fff;
}

.rank-note {
  margin: 14px 0 18px;
  padding: 10px 14px;
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
  font-size: 12px;
  color: var(--rh-text-sub);
  line-height: 1.8;
}

.rank-row {
  display: grid;
  grid-template-columns: 60px 2fr 1fr 1fr 70px 90px 70px;
  gap: 10px;
  align-items: center;
  padding: 12px 8px;
  border-bottom: 1px solid var(--rh-border-light);
  font-size: 13px;
}

.rank-row--head {
  color: var(--rh-text-light);
  font-size: 12px;
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
}

.rank-row:not(.rank-row--head):hover {
  background: var(--rh-surface-sub);
}

.rank-row__no em {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 4px;
  background: var(--rh-surface-sub);
  color: var(--rh-text-light);
  font-style: normal;
  font-weight: 600;
}

.rank-row__no em.is-top {
  background: var(--rh-primary);
  color: #fff;
}

.rank-row__name {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.rank-row__name img {
  width: 64px;
  height: 44px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.rank-row__name b {
  display: block;
  font-size: 14px;
}

.rank-row__name span {
  display: block;
  font-size: 12px;
  min-width: 0;
}

.rank-row__brand,
.rank-row__price,
.rank-row__score,
.rank-row__stat {
  font-size: 13px;
}

.rank-row__stat {
  color: var(--rh-text-sub);
}
</style>
