<template>
  <div class="rh-card">
    <div class="page-head">
      <h1 class="page-title">我的关注</h1>
      <div class="filters">
        <span class="chip" :class="{ 'chip--active': !followType }" @click="setType(null)">全部</span>
        <span
          v-for="t in types"
          :key="t.value"
          class="chip"
          :class="{ 'chip--active': followType === t.value }"
          @click="setType(t.value)"
        >
          {{ t.label }}
        </span>
      </div>
    </div>

    <div v-if="loading" class="rh-empty">加载中…</div>
    <div v-else-if="!list.length" class="rh-empty">暂无关注</div>
    <div v-else class="item-list">
      <div v-for="item in list" :key="item.id" class="item-row">
        <router-link :to="item.url || '#'" class="item-row__main">
          <img :src="imageOf(item.avatar)" :alt="item.name" />
          <div>
            <div class="item-row__title">{{ item.name }}</div>
            <div class="item-row__meta rh-text-light">
              {{ typeLabel(item.followType) }} · {{ formatDate(item.createTime) }}
            </div>
            <div v-if="item.description" class="item-row__sub rh-clamp-2 rh-text-sub">
              {{ item.description }}
            </div>
          </div>
        </router-link>
        <el-button text type="danger" @click="unfollow(item)">取消关注</el-button>
      </div>
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
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { followApi, userApi } from '@/api'
import { formatDate, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const types = [
  { value: 'brand', label: '品牌' },
  { value: 'company', label: '企业' },
  { value: 'user', label: '用户' },
  { value: 'robot', label: '机器人' }
]

const list = ref([])
const followType = ref(null)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

function typeLabel (t) {
  const hit = types.find((x) => x.value === t)
  return (hit && hit.label) || t
}

async function load () {
  loading.value = true
  try {
    const data = await userApi.myFollows({
      followType: followType.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function setType (t) {
  followType.value = t
  pageNum.value = 1
  load()
}

function changePage (p) {
  pageNum.value = p
  load()
}

async function unfollow (item) {
  await followApi.remove(item.followType, item.followId)
  ElMessage.success('已取消关注')
  load()
}

onMounted(() => {
  setPageMeta({ title: '我的关注 - 机器人之家' })
  load()
})
</script>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0;
  font-size: 18px;
}

.filters {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.chip {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: var(--rh-text-sub);
}

.chip--active {
  background: var(--rh-primary);
  color: #fff;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
}

.item-row__main {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.item-row__main img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
  background: var(--rh-surface-sub);
}

.item-row__title {
  font-weight: 600;
}

.item-row__meta {
  font-size: 12px;
  margin-top: 2px;
}

.item-row__sub {
  font-size: 12px;
  margin-top: 4px;
}
</style>
