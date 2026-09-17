<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="问题管理" name="questions">
        <el-table :data="questions" v-loading="loading" border stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="authorName" label="提问者" width="100" />
          <el-table-column prop="robotName" label="关联机器人" width="120" />
          <el-table-column prop="answerCount" label="回答数" width="80" />
          <el-table-column prop="followCount" label="关注数" width="80" />
          <el-table-column prop="viewCount" label="浏览数" width="80" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : row.status === 3 ? 'danger' : 'info'">{{ row.status === 1 ? '正常' : row.status === 2 ? '隐藏' : row.status === 3 ? '已删除' : '待审核' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-popconfirm title="确定删除该问题？" @confirm="handleDeleteQuestion(row.id)">
                <template #reference>
                  <el-button type="danger" text size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="total, prev, pager, next" :current-page="qPage" :page-size="pageSize" :total="qTotal" @current-change="(p) => { qPage = p; loadQuestions() }" />
      </el-tab-pane>

      <el-tab-pane label="回答管理" name="answers">
        <el-table :data="answers" v-loading="aLoading" border stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="questionTitle" label="问题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="authorName" label="回答者" width="100" />
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="helpfulCount" label="有用数" width="80" />
          <el-table-column prop="createTime" label="创建时间" width="160" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-popconfirm title="确定删除该回答？" @confirm="handleDeleteAnswer(row.id)">
                <template #reference>
                  <el-button type="danger" text size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="total, prev, pager, next" :current-page="aPage" :page-size="pageSize" :total="aTotal" @current-change="(p) => { aPage = p; loadAnswers() }" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionPage, getAnswerPage, deleteQuestion, deleteAnswer } from '@/api/qa'

const activeTab = ref('questions')
const loading = ref(false)
const questions = ref([])
const qPage = ref(1)
const qTotal = ref(0)
const aLoading = ref(false)
const answers = ref([])
const aPage = ref(1)
const aTotal = ref(0)
const pageSize = ref(20)

async function loadQuestions() {
  loading.value = true
  try {
    const res = await getQuestionPage({ pageNum: qPage.value, pageSize: pageSize.value })
    if (res.data) {
      questions.value = res.data.list || []
      qTotal.value = res.data.total || 0
    }
  } finally { loading.value = false }
}

async function loadAnswers() {
  aLoading.value = true
  try {
    const res = await getAnswerPage({ pageNum: aPage.value, pageSize: pageSize.value })
    if (res.data) {
      answers.value = res.data.list || []
      aTotal.value = res.data.total || 0
    }
  } finally { aLoading.value = false }
}

async function handleDeleteQuestion(id) {
  await deleteQuestion(id)
  ElMessage.success('删除成功')
  loadQuestions()
}

async function handleDeleteAnswer(id) {
  await deleteAnswer(id)
  ElMessage.success('删除成功')
  loadAnswers()
}

onMounted(loadQuestions)
</script>