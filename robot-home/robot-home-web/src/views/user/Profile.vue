<template>
  <div class="rh-card">
    <h1 class="page-title">个人资料</h1>

    <div v-if="stats" class="stats-row">
      <div class="stats-row__item"><b>{{ stats.favoriteCount || 0 }}</b><span>收藏</span></div>
      <div class="stats-row__item"><b>{{ stats.postCount || 0 }}</b><span>帖子</span></div>
      <div class="stats-row__item"><b>{{ stats.commentCount || 0 }}</b><span>评论</span></div>
      <div class="stats-row__item"><b>{{ stats.followCount || 0 }}</b><span>关注</span></div>
      <div class="stats-row__item"><b>{{ stats.inquiryCount || 0 }}</b><span>询价</span></div>
    </div>

    <el-form v-if="form" label-width="80px" class="profile-form" @submit.prevent>
      <el-form-item label="头像">
        <div class="profile-form__avatar">
          <el-avatar :size="64" :src="form.avatar">
            {{ (form.nickname || '我').charAt(0) }}
          </el-avatar>
          <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
            <el-button size="small">更换头像</el-button>
          </el-upload>
        </div>
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" maxlength="32" style="width: 320px" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio :value="0">保密</el-radio>
          <el-radio :value="1">男</el-radio>
          <el-radio :value="2">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" style="width: 320px" />
      </el-form-item>
      <el-form-item label="省份">
        <el-input v-model="form.province" style="width: 320px" />
      </el-form-item>
      <el-form-item label="城市">
        <el-input v-model="form.city" style="width: 320px" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="255" style="width: 480px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
    <div v-else class="rh-empty">加载中…</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi, userApi } from '@/api'
import { useUserStore } from '@/store/user'
import { setPageMeta } from '@/utils/seo'

const userStore = useUserStore()
const form = ref(null)
const stats = ref(null)
const saving = ref(false)

async function load () {
  const [profile, st] = await Promise.all([
    userStore.profile || userStore.loadProfile(),
    userApi.stats().catch(() => null)
  ])
  stats.value = st
  userStore.stats = st
  if (profile) {
    form.value = reactive({
      nickname: profile.nickname || '',
      avatar: profile.avatar || '',
      gender: profile.gender == null ? 0 : profile.gender,
      email: profile.email || '',
      province: profile.province || '',
      city: profile.city || '',
      intro: profile.intro || ''
    })
  }
}

async function uploadAvatar (option) {
  try {
    const data = await fileApi.upload(option.file, 'avatar')
    const url = (data && (data.url || data.path)) || ''
    if (url && form.value) {
      form.value.avatar = url
    }
    option.onSuccess && option.onSuccess(data)
  } catch (e) {
    option.onError && option.onError(e)
  }
}

async function save () {
  saving.value = true
  try {
    await userApi.update({ ...form.value })
    await userStore.loadProfile()
    ElMessage.success('已保存')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  setPageMeta({ title: '个人资料 - 机器人之家' })
  load()
})
</script>

<style scoped lang="scss">
.page-title {
  margin: 0 0 18px;
  font-size: 18px;
}

.stats-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  padding: 14px 16px;
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
}

.stats-row__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--rh-text-light);
}

.stats-row__item b {
  font-size: 18px;
  color: var(--rh-text);
}

.profile-form__avatar {
  display: flex;
  align-items: center;
  gap: 14px;
}
</style>
