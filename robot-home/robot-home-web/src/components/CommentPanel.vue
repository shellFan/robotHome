<template>
  <div v-if="FEATURES.comments" class="comment-panel">
    <div class="comment-panel__head">
      <span class="comment-panel__title">评论</span>
      <span class="rh-text-light">共 {{ page.total }} 条</span>
    </div>

    <div class="comment-panel__editor">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        :placeholder="userStore.isLogin ? '说说你的看法…' : '登录后参与评论'"
        :disabled="!userStore.isLogin"
      />
      <div class="comment-panel__editor-actions">
        <router-link v-if="!userStore.isLogin" to="/login">
          <el-button type="primary" size="small">登录</el-button>
        </router-link>
        <el-button v-else type="primary" size="small" :loading="submitting" @click="submit">
          发表评论
        </el-button>
      </div>
    </div>

    <div v-if="page.total === 0 && !loading" class="rh-empty">还没有评论，来说两句吧</div>

    <div v-for="item in page.list" :key="item.id" class="comment">
      <el-avatar :size="36" :src="item.user && item.user.avatar">
        {{ (item.user && item.user.nickname || '?').charAt(0) }}
      </el-avatar>
      <div class="comment__main">
        <div class="comment__meta">
          <span class="comment__author">{{ item.user && item.user.nickname }}</span>
          <span class="rh-text-light">{{ fromNow(item.createTime) }}</span>
        </div>
        <div class="comment__content">{{ item.content }}</div>
        <div class="comment__actions">
          <span :class="{ 'is-active': item.liked }" @click="toggleLike(item)">
            <el-icon><Pointer /></el-icon> {{ item.likeCount || 0 }}
          </span>
          <span @click="openReply(item)">回复</span>
          <span v-if="item.canDelete" class="comment__delete" @click="remove(item)">删除</span>
        </div>

        <!-- 回复预览 -->
        <div v-if="item.replies && item.replies.length" class="comment__replies">
          <div v-for="reply in item.replies" :key="reply.id" class="comment__reply">
            <span class="comment__reply-author">{{ reply.user && reply.user.nickname }}：</span>
            <span>{{ reply.content }}</span>
          </div>
          <div
            v-if="(item.replyCount || 0) > (item.replies || []).length"
            class="comment__more"
            @click="loadReplies(item)"
          >
            共 {{ item.replyCount }} 条回复，点击查看
          </div>
        </div>

        <!-- 回复输入框 -->
        <div v-if="replyTo === item.id" class="comment__reply-editor">
          <el-input
            v-model="replyContent"
            size="small"
            type="textarea"
            :rows="2"
            :placeholder="'回复 @' + (item.user && item.user.nickname)"
          />
          <div class="comment__reply-actions">
            <el-button size="small" text @click="cancelReply">取消</el-button>
            <el-button size="small" type="primary" @click="submitReply(item)">提交</el-button>
          </div>
        </div>

        <!-- 全部回复抽屉 -->
        <el-dialog v-model="replyDialog.visible" :title="'回复（' + replyDialog.total + '）'" width="560px">
          <div v-for="r in replyDialog.list" :key="r.id" class="comment__reply-row">
            <el-avatar :size="28" :src="r.user && r.user.avatar" />
            <div>
              <div class="comment__meta">
                <span class="comment__author">{{ r.user && r.user.nickname }}</span>
                <span class="rh-text-light">{{ fromNow(r.createTime) }}</span>
              </div>
              <div>{{ r.content }}</div>
            </div>
          </div>
          <div v-if="!replyDialog.list.length" class="rh-empty">暂无回复</div>
        </el-dialog>
      </div>
    </div>

    <el-pagination
      v-if="page.total > page.pageSize"
      layout="prev, pager, next"
      :current-page="page.pageNum"
      :page-size="page.pageSize"
      :total="page.total"
      @current-change="changePage"
    />
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Pointer } from '@element-plus/icons-vue'
import { commentApi } from '@/api'
import { useUserStore } from '@/store/user'
import { fromNow } from '@/utils/format'
import { FEATURES } from '@/config/features'

const props = defineProps({
  bizType: { type: String, required: true },
  bizId: { type: [Number, String], required: true }
})

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const content = ref('')
const replyContent = ref('')
const replyTo = ref(null)

const page = reactive({ list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 })
const replyDialog = reactive({ visible: false, list: [], total: 0 })

async function load () {
  loading.value = true
  try {
    const data = await commentApi.list({
      bizType: props.bizType,
      bizId: props.bizId,
      pageNum: page.pageNum,
      pageSize: page.pageSize
    })
    page.list = data.list || []
    page.total = data.total || 0
    page.pages = data.pages || 0
  } catch (e) {
    page.list = []
  } finally {
    loading.value = false
  }
}

function requireLogin () {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return false
  }
  return true
}

async function submit () {
  if (!requireLogin()) return
  if (!content.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await commentApi.add({ bizType: props.bizType, bizId: props.bizId, content: content.value })
    content.value = ''
    ElMessage.success('评论成功')
    page.pageNum = 1
    await load()
  } finally {
    submitting.value = false
  }
}

function openReply (item) {
  if (!requireLogin()) return
  replyTo.value = replyTo.value === item.id ? null : item.id
  replyContent.value = ''
}

function cancelReply () {
  replyTo.value = null
  replyContent.value = ''
}

async function submitReply (item) {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  await commentApi.add({
    bizType: props.bizType,
    bizId: props.bizId,
    content: replyContent.value,
    parentId: item.id,
    replyTo: item.user && item.user.id
  })
  ElMessage.success('回复成功')
  cancelReply()
  await load()
}

async function toggleLike (item) {
  if (!requireLogin()) return
  try {
    const data = await commentApi.like(item.id)
    item.liked = data.liked
    item.likeCount = (item.likeCount || 0) + (data.liked ? 1 : -1)
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function remove (item) {
  await commentApi.remove(item.id)
  ElMessage.success('已删除')
  await load()
}

async function loadReplies (item) {
  const data = await commentApi.replies(item.id, { pageNum: 1, pageSize: 50 })
  replyDialog.list = data.list || []
  replyDialog.total = data.total || 0
  replyDialog.visible = true
}

function changePage (p) {
  page.pageNum = p
  load()
}

watch(
  () => [props.bizType, props.bizId],
  () => {
    page.pageNum = 1
    load()
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.comment-panel__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
}

.comment-panel__title {
  font-size: 17px;
  font-weight: 600;
}

.comment-panel__editor {
  margin-bottom: 20px;
}

.comment-panel__editor-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.comment {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid var(--rh-border-light);
}

.comment__main {
  flex: 1;
  min-width: 0;
}

.comment__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.comment__author {
  font-weight: 600;
}

.comment__content {
  margin: 6px 0 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment__actions {
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: var(--rh-text-light);
}

.comment__actions span {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.comment__actions span:hover {
  color: var(--rh-primary);
}

.comment__actions .is-active {
  color: var(--rh-primary);
}

.comment__delete:hover {
  color: var(--rh-danger) !important;
}

.comment__replies {
  margin-top: 10px;
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
  padding: 10px 12px;
}

.comment__reply {
  font-size: 13px;
  margin-bottom: 6px;
  word-break: break-word;
}

.comment__reply-author {
  color: var(--rh-primary);
}

.comment__more {
  font-size: 13px;
  color: var(--rh-primary);
  cursor: pointer;
}

.comment__reply-editor {
  margin-top: 10px;
}

.comment__reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 6px;
}

.comment__reply-row {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--rh-border-light);
  font-size: 14px;
}
</style>
