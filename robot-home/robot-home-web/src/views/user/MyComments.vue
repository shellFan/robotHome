<template>
  <div class="rh-card">
    <h1 class="page-title">我的评论</h1>

    <div v-if="loading" class="rh-empty">加载中…</div>
    <div v-else-if="!list.length" class="rh-empty">暂无评论</div>
    <div v-else class="comment-list">
      <div v-for="item in list" :key="item.id" class="comment-row">
        <div class="comment-row__content">{{ item.content }}</div>
        <div class="comment-row__meta rh-text-light">
          {{ bizLabel(item.bizType) }} #{{ item.bizId }} · {{ fromNow(item.createTime) }} ·
          {{ item.likeCount || 0 }} 赞
        </div>
        <div class="comment-row__actions">
          <router-link :to="bizPath(item)">查看原内容</router-link>
          <span v-if="item.canDelete" class="comment-row__delete" @click="remove(item)">删除</span>
        </div>
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
import { commentApi, userApi } from '@/api'
import { fromNow } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

function bizLabel (t) {
  const map = {
    robot: '机器人',
    article: '资讯',
    video: '视频',
    tutorial: '教程',
    post: '帖子'
  }
  return map[t] || t
}

function bizPath (item) {
  const map = {
    robot: '/robot/',
    article: '/article/',
    video: '/video/',
    tutorial: '/tutorial/',
    post: '/community/'
  }
  return (map[item.bizType] || '/') + item.bizId
}

async function load () {
  loading.value = true
  try {
    const data = await userApi.myComments({
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

async function remove (item) {
  await commentApi.remove(item.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  setPageMeta({ title: '我的评论 - 机器人之家' })
  load()
})
</script>

<style scoped lang="scss">
.page-title {
  margin: 0 0 16px;
  font-size: 18px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-row {
  padding: 12px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
}

.comment-row__content {
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-row__meta {
  font-size: 12px;
  margin: 8px 0;
}

.comment-row__actions {
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.comment-row__actions a {
  color: var(--rh-primary);
}

.comment-row__delete {
  color: var(--rh-danger);
  cursor: pointer;
}
</style>
