<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ name: 'community' }">社区</el-breadcrumb-item>
        <el-breadcrumb-item>发帖</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card post-create">
        <h1 class="post-create__title">发布帖子</h1>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="圈子">
            <el-select v-model="form.circleId" placeholder="选择圈子" clearable style="width: 100%">
              <el-option
                v-for="circle in circles"
                :key="circle.id"
                :label="circle.name"
                :value="circle.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="话题">
            <el-input v-model="form.topic" maxlength="32" placeholder="可选，如：选型求助" />
          </el-form-item>
          <el-form-item label="正文">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="10"
              maxlength="5000"
              show-word-limit
              placeholder="分享你的看法、经验或问题…"
            />
          </el-form-item>
          <el-form-item label="图片">
            <div class="post-create__images">
              <div v-for="(img, idx) in form.images" :key="idx" class="post-create__thumb">
                <img :src="imageOf(img)" alt="" />
                <span class="post-create__remove" @click="removeImage(idx)">×</span>
              </div>
              <el-upload
                v-if="form.images.length < 9"
                :show-file-list="false"
                :http-request="uploadImage"
                accept="image/*"
              >
                <div class="post-create__upload">+</div>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
            <el-button @click="$router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { communityApi, fileApi } from '@/api'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const router = useRouter()
const circles = ref([])
const submitting = ref(false)
const form = reactive({
  circleId: null,
  title: '',
  topic: '',
  content: '',
  images: []
})

async function uploadImage (option) {
  try {
    const data = await fileApi.upload(option.file, 'community')
    const url = (data && (data.url || data.path)) || ''
    if (url) {
      form.images.push(url)
    }
    option.onSuccess && option.onSuccess(data)
  } catch (e) {
    option.onError && option.onError(e)
  }
}

function removeImage (idx) {
  form.images.splice(idx, 1)
}

async function submit () {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写正文')
    return
  }
  submitting.value = true
  try {
    const data = await communityApi.create({
      circleId: form.circleId || undefined,
      title: form.title.trim(),
      topic: form.topic.trim() || undefined,
      content: form.content.trim(),
      images: form.images.length ? form.images : undefined
    })
    ElMessage.success('发布成功')
    router.replace('/community/' + (data && data.id))
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  setPageMeta({ title: '发帖 - 机器人之家' })
  circles.value = (await communityApi.circles()) || []
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.post-create {
  max-width: 800px;
}

.post-create__title {
  margin: 0 0 20px;
  font-size: 20px;
}

.post-create__images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.post-create__thumb {
  position: relative;
  width: 96px;
  height: 96px;
}

.post-create__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.post-create__remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--rh-danger);
  color: #fff;
  text-align: center;
  line-height: 20px;
  cursor: pointer;
  font-size: 14px;
}

.post-create__upload {
  width: 96px;
  height: 96px;
  border: 1px dashed var(--rh-border);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--rh-text-light);
  cursor: pointer;
}

.post-create__upload:hover {
  border-color: var(--rh-primary);
  color: var(--rh-primary);
}
</style>
