<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>意见反馈</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card feedback-card">
        <h1 class="feedback-card__title">意见反馈</h1>
        <p class="rh-text-sub">您的反馈对我们非常重要，帮助我们持续改进产品体验</p>

        <el-form label-position="top" class="feedback-form" @submit.prevent>
          <el-form-item label="反馈类型" required>
            <el-radio-group v-model="form.feedbackType">
              <el-radio value="bug">Bug 反馈</el-radio>
              <el-radio value="feature">功能建议</el-radio>
              <el-radio value="improvement">体验改进</el-radio>
              <el-radio value="other">其他</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="反馈内容" required>
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="6"
              maxlength="2000"
              show-word-limit
              placeholder="请详细描述您的问题或建议，以便我们更好地理解和处理"
            />
          </el-form-item>

          <el-form-item label="联系方式（可选）">
            <el-input
              v-model="form.contact"
              maxlength="128"
              placeholder="手机号或邮箱，方便我们跟进反馈"
            />
          </el-form-item>

          <el-button type="primary" size="large" :loading="submitting" @click="submit">
            提交反馈
          </el-button>
        </el-form>

        <div v-if="submitted" class="feedback-success">
          <el-icon class="feedback-success__icon"><CircleCheckFilled /></el-icon>
          <p class="feedback-success__text">感谢您的反馈！我们会认真处理每一条意见。</p>
          <el-button type="primary" plain @click="resetForm">继续反馈</el-button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import MainLayout from '@/layout/MainLayout.vue'
import { feedbackApi } from '@/api'
import { setPageMeta } from '@/utils/seo'

const form = reactive({
  feedbackType: 'bug',
  content: '',
  contact: '',
  pageUrl: ''
})

const submitting = ref(false)
const submitted = ref(false)

function resetForm () {
  form.feedbackType = 'bug'
  form.content = ''
  form.contact = ''
  form.pageUrl = ''
  submitted.value = false
}

async function submit () {
  if (!form.content.trim()) {
    ElMessage.warning('请填写反馈内容')
    return
  }
  if (form.content.trim().length < 10) {
    ElMessage.warning('反馈内容至少10个字')
    return
  }
  submitting.value = true
  try {
    await feedbackApi.submit({
      feedbackType: form.feedbackType,
      content: form.content.trim(),
      contact: form.contact.trim() || undefined,
      pageUrl: form.pageUrl || undefined
    })
    submitted.value = true
  } catch (e) {
    const msg = e && e.message
    if (msg && (msg.includes('频繁') || msg.includes('429'))) {
      ElMessage.warning('反馈提交过于频繁，请1分钟后再试')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  setPageMeta({ title: '意见反馈 - 机器人之家', description: '提交您的反馈和建议，帮助我们改进产品体验' })
  form.pageUrl = window.location.href
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.feedback-card {
  max-width: 680px;
  margin: 0 auto;
  padding: 32px;
}

.feedback-card__title {
  margin: 0 0 6px;
  font-size: 22px;
}

.feedback-form {
  margin-top: 20px;
}

.feedback-success {
  margin-top: 24px;
  padding: 24px;
  text-align: center;
  background: var(--rh-surface-sub, #f6f8fa);
  border-radius: var(--rh-radius, 8px);
}

.feedback-success__icon {
  font-size: 48px;
  color: var(--rh-primary, #409eff);
}

.feedback-success__text {
  margin: 12px 0 16px;
  font-size: 15px;
  color: var(--rh-text);
}

@media (max-width: 768px) {
  .feedback-card {
    padding: 20px;
  }
}
</style>