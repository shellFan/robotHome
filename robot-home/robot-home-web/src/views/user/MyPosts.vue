<template>
  <div class="rh-card">
    <div class="page-head">
      <h1 class="page-title">我的帖子</h1>
      <router-link to="/community/create">
        <el-button type="primary" size="small">发帖</el-button>
      </router-link>
    </div>

    <div v-if="loading" class="rh-empty">加载中…</div>
    <div v-else-if="!list.length" class="rh-empty">还没有发过帖子</div>
    <div v-else class="post-list">
      <div v-for="post in list" :key="post.id" class="post-row">
        <router-link :to="'/community/' + post.id" class="post-row__main">
          <div class="post-row__title">{{ post.title }}</div>
          <div class="post-row__meta rh-text-light">
            {{ fromNow(post.createTime) }} · {{ formatCount(post.likeCount) }} 赞 ·
            {{ formatCount(post.commentCount) }} 评论 · 状态 {{ statusText(post.status) }}
          </div>
        </router-link>
        <el-button v-if="post.canDelete !== false" text type="danger" @click="remove(post)">删除</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { communityApi, userApi } from '@/api'
import { formatCount, fromNow } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

function statusText (s) {
  if (s === 1) return '已发布'
  if (s === 0) return '待审'
  if (s === 2) return '已下架'
  return String(s ?? '-')
}

async function load () {
  loading.value = true
  try {
    const data = await userApi.myPosts({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function changePage (p) {
  pageNum.value = p
  load()
}

async function remove (post) {
  await ElMessageBox.confirm('确定删除该帖子吗？', '提示', { type: 'warning' })
  await communityApi.remove(post.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  setPageMeta({ title: '我的帖子 - 机器人之家' })
  load()
})
</script>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 18px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
}

.post-row__main {
  flex: 1;
  min-width: 0;
}

.post-row__title {
  font-weight: 600;
}

.post-row__meta {
  font-size: 12px;
  margin-top: 4px;
}
</style>
