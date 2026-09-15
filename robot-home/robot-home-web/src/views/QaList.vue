<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>问答</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="qa-layout">
        <div class="qa-main">
          <div class="rh-card">
            <div class="qa-toolbar">
              <div class="sorts">
                <span class="chip" :class="{ 'chip--active': sort === 'latest' }" @click="setSort('latest')">最新</span>
                <span class="chip" :class="{ 'chip--active': sort === 'hot' }" @click="setSort('hot')">热门</span>
                <span class="chip" :class="{ 'chip--active': sort === 'unanswered' }" @click="setSort('unanswered')">待回答</span>
              </div>
              <el-button type="primary" @click="showAsk = true">提问</el-button>
            </div>

            <div v-if="loading" class="rh-empty">加载中…</div>
            <div v-else-if="!questions.length" class="rh-empty">暂无问题</div>
            <div v-else class="qa-list">
              <router-link
                v-for="q in questions"
                :key="q.id"
                :to="'/qa/' + q.id"
                class="qa-item"
              >
                <div class="qa-item__stats">
                  <div class="qa-item__stat">
                    <span class="qa-item__num">{{ q.answerCount || 0 }}</span>
                    <span class="qa-item__label">回答</span>
                  </div>
                  <div class="qa-item__stat">
                    <span class="qa-item__num">{{ q.followCount || 0 }}</span>
                    <span class="qa-item__label">关注</span>
                  </div>
                </div>
                <div class="qa-item__body">
                  <div class="qa-item__title">{{ q.title }}</div>
                  <div class="qa-item__desc rh-clamp-2">{{ q.content }}</div>
                  <div class="qa-item__meta rh-text-light">
                    <span v-if="q.robotName" class="qa-item__robot">{{ q.robotName }}</span>
                    <span>{{ fromNow(q.createTime) }}</span>
                    <span>{{ q.authorName }}</span>
                  </div>
                </div>
              </router-link>
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
        </div>
      </div>
    </div>

    <!-- 提问弹窗 -->
    <el-dialog v-model="showAsk" title="提问" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入问题标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请详细描述您的问题" maxlength="2000" />
        </el-form-item>
        <el-form-item label="关联机器人">
          <el-input v-model="form.robotId" placeholder="机器人ID（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAsk = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitQuestion">提交</el-button>
      </template>
    </el-dialog>
  </MainLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import MainLayout from '@/layout/MainLayout.vue'
import { qaApi } from '@/api'
import { formatCount, fromNow } from '@/utils/format'

const userStore = useUserStore()
const loading = ref(false)
const questions = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const sort = ref('latest')
const showAsk = ref(false)
const submitting = ref(false)
const form = ref({ title: '', content: '', robotId: '' })

function setSort(s) {
  sort.value = s
  pageNum.value = 1
  loadQuestions()
}

async function loadQuestions() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value, sort: sort.value }
    const res = await qaApi.questions(params)
    if (res.data) {
      questions.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  pageNum.value = p
  loadQuestions()
}

async function submitQuestion() {
  if (!form.value.title.trim()) return ElMessage.warning('请输入标题')
  if (!form.value.content.trim()) return ElMessage.warning('请输入内容')
  submitting.value = true
  try {
    const data = { title: form.value.title, content: form.value.content }
    if (form.value.robotId) data.robotId = Number(form.value.robotId)
    await qaApi.createQuestion(data)
    ElMessage.success('提问成功')
    showAsk.value = false
    form.value = { title: '', content: '', robotId: '' }
    loadQuestions()
  } catch (e) {
    // error handled by request util
  } finally {
    submitting.value = false
  }
}

onMounted(loadQuestions)
</script>

<style scoped>
.qa-layout { display: flex; gap: 20px; }
.qa-main { flex: 1; min-width: 0; }
.qa-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.qa-toolbar .sorts { display: flex; gap: 8px; }
.qa-list { display: flex; flex-direction: column; gap: 12px; }
.qa-item { display: flex; gap: 16px; padding: 16px; border-radius: 8px; background: #fafafa; text-decoration: none; color: inherit; transition: background .2s; }
.qa-item:hover { background: #f0f0f0; }
.qa-item__stats { display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 60px; }
.qa-item__num { font-size: 18px; font-weight: 600; color: #333; }
.qa-item__label { font-size: 12px; color: #999; }
.qa-item__body { flex: 1; min-width: 0; }
.qa-item__title { font-size: 16px; font-weight: 500; margin-bottom: 6px; line-height: 1.4; }
.qa-item__desc { font-size: 14px; color: #666; line-height: 1.5; margin-bottom: 8px; }
.qa-item__meta { display: flex; gap: 12px; font-size: 12px; }
.qa-item__robot { color: #409eff; }
</style>