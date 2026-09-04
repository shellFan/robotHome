<template>
  <div class="rh-card">
    <div class="page-head">
      <h1 class="page-title">我的消息</h1>
      <el-button size="small" @click="readAll">全部已读</el-button>
    </div>

    <div v-if="loading" class="rh-empty">加载中…</div>
    <div v-else-if="!list.length" class="rh-empty">暂无消息</div>
    <div v-else class="msg-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="msg-row"
        :class="{ 'msg-row--unread': !item.isRead }"
        @click="readOne(item)"
      >
        <div class="msg-row__title">
          <span v-if="!item.isRead" class="msg-row__dot" />
          {{ item.title }}
        </div>
        <div class="msg-row__content rh-text-sub">{{ item.content }}</div>
        <div class="msg-row__meta rh-text-light">
          {{ typeLabel(item.type) }} · {{ formatDate(item.createTime, 'YYYY-MM-DD HH:mm') }}
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
import { messageApi } from '@/api'
import { formatDate } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

function typeLabel (t) {
  const map = {
    system: '系统',
    comment: '评论',
    like: '点赞',
    follow: '关注',
    inquiry: '询价'
  }
  return map[t] || t || '通知'
}

async function load () {
  loading.value = true
  try {
    const data = await messageApi.list({
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

async function readOne (item) {
  if (!item.isRead) {
    await messageApi.read(item.id)
    item.isRead = 1
  }
}

async function readAll () {
  await messageApi.readAll()
  ElMessage.success('已全部标为已读')
  load()
}

onMounted(() => {
  setPageMeta({ title: '我的消息 - 机器人之家' })
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

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg-row {
  padding: 12px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
  cursor: pointer;
}

.msg-row:hover {
  border-color: var(--rh-primary);
}

.msg-row--unread {
  background: var(--rh-primary-light);
}

.msg-row__title {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.msg-row__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--rh-danger);
}

.msg-row__content {
  margin: 6px 0;
  font-size: 13px;
}

.msg-row__meta {
  font-size: 12px;
}
</style>
