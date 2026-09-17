<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/qa' }">问答</el-breadcrumb-item>
        <el-breadcrumb-item>问题详情</el-breadcrumb-item>
      </el-breadcrumb>

      <div v-if="loading" class="rh-empty">加载中…</div>
      <div v-else-if="!question" class="rh-empty">问题不存在</div>
      <template v-else>
        <div class="rh-card qa-detail">
          <h1 class="qa-detail__title">{{ question.title }}</h1>
          <div class="qa-detail__meta rh-text-light">
            <span>{{ question.authorName }}</span>
            <span>{{ fromNow(question.createTime) }}</span>
            <span>{{ question.followCount || 0 }} 人关注</span>
            <span>{{ question.viewCount || 0 }} 次浏览</span>
            <router-link v-if="question.robotId" :to="'/robot/' + question.robotId" class="qa-detail__robot">
              {{ question.robotName }}
            </router-link>
          </div>
          <div class="qa-detail__content">{{ question.content }}</div>
          <div class="qa-detail__actions">
            <el-button :type="followed ? 'primary' : ''" @click="toggleFollow">
              {{ followed ? '已关注' : '关注问题' }}
            </el-button>
          </div>
        </div>

        <div class="rh-card" style="margin-top: 20px;">
          <h3>回答 ({{ answers.length }})</h3>
          <div v-if="!answers.length" class="rh-empty">暂无回答</div>
          <div v-else class="answer-list">
            <div v-for="a in answers" :key="a.id" class="answer-item">
              <div class="answer-item__header">
                <el-avatar :size="32">{{ (a.authorName || '?').charAt(0) }}</el-avatar>
                <div>
                  <div class="answer-item__author">{{ a.authorName }}</div>
                  <div class="rh-text-light">{{ fromNow(a.createTime) }}</div>
                </div>
              </div>
              <div class="answer-item__content">{{ a.content }}</div>
              <div class="answer-item__actions">
                <el-button text size="small" :type="a.helpful ? 'primary' : ''" @click="toggleHelpful(a)">
                  有用 ({{ a.helpfulCount || 0 }})
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="rh-card" style="margin-top: 20px;">
          <h3>写回答</h3>
          <el-input v-model="answerContent" type="textarea" :rows="4" placeholder="分享您的经验和见解…" maxlength="2000" />
          <div style="margin-top: 12px; text-align: right;">
            <el-button type="primary" :loading="submitting" @click="submitAnswer">提交回答</el-button>
          </div>
        </div>
      </template>
    </div>
  </MainLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import MainLayout from '@/layout/MainLayout.vue'
import { qaApi } from '@/api'
import { fromNow } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(true)
const question = ref(null)
const answers = ref([])
const followed = ref(false)
const answerContent = ref('')
const submitting = ref(false)

async function loadDetail() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await qaApi.questionDetail(id)
    if (res.data) {
      question.value = res.data
      answers.value = res.data.answers || []
      followed.value = res.data.followed || false
    }
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!userStore.isLogin) return ElMessage.warning('请先登录')
  if (!question.value) return
  try {
    if (followed.value) {
      await qaApi.unfollowQuestion(question.value.id)
    } else {
      await qaApi.followQuestion(question.value.id)
    }
    followed.value = !followed.value
  } catch (e) { /* handled */ }
}

async function toggleHelpful(answer) {
  if (!userStore.isLogin) return ElMessage.warning('请先登录')
  try {
    if (answer.helpful) {
      await qaApi.unhelpfulAnswer(answer.id)
    } else {
      await qaApi.helpfulAnswer(answer.id)
    }
    answer.helpful = !answer.helpful
    answer.helpfulCount = (answer.helpfulCount || 0) + (answer.helpful ? 1 : -1)
  } catch (e) { /* handled */ }
}

async function submitAnswer() {
  if (!userStore.isLogin) return ElMessage.warning('请先登录')
  if (!answerContent.value.trim()) return
  submitting.value = true
  try {
    await qaApi.createAnswer(question.value.id, { content: answerContent.value })
    answerContent.value = ''
    loadDetail()
  } catch (e) { /* handled */ } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.qa-detail__title { font-size: 22px; font-weight: 600; margin-bottom: 12px; }
.qa-detail__meta { display: flex; gap: 12px; font-size: 13px; margin-bottom: 16px; flex-wrap: wrap; }
.qa-detail__robot { color: #409eff; text-decoration: none; }
.qa-detail__content { font-size: 15px; line-height: 1.7; margin-bottom: 16px; white-space: pre-wrap; }
.qa-detail__actions { padding-top: 12px; border-top: 1px solid #eee; }
.answer-list { display: flex; flex-direction: column; gap: 16px; margin-top: 12px; }
.answer-item { padding: 16px; background: #fafafa; border-radius: 8px; }
.answer-item__header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.answer-item__author { font-weight: 500; }
.answer-item__content { font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.answer-item__actions { margin-top: 8px; }
</style>