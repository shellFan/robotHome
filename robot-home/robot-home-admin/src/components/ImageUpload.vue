<template>
  <div class="image-upload">
    <div class="image-upload__row">
      <el-upload
        class="image-upload__btn"
        :show-file-list="false"
        :http-request="handleRequest"
        :before-upload="beforeUpload"
        accept="image/*"
      >
        <el-button :loading="loading" :icon="'Upload'">上传图片</el-button>
      </el-upload>
      <el-input
        :model-value="modelValue"
        placeholder="图片地址，也可直接粘贴"
        clearable
        @update:model-value="onInput"
      />
    </div>
    <div v-if="modelValue" class="image-upload__preview">
      <el-image :src="modelValue" :preview-src-list="[modelValue]" fit="contain" class="image-upload__img" />
      <el-button link type="danger" @click="clear">移除</el-button>
    </div>
    <div v-if="tip" class="form-tip">{{ tip }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'

const props = defineProps({
  modelValue: { type: String, default: '' },
  module: { type: String, default: 'common' },
  tip: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const loading = ref(false)

function beforeUpload(file) {
  const ok = /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
  if (!ok) {
    ElMessage.error('仅支持 jpg/png/gif/webp/bmp 图片')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('图片不能超过 20MB')
    return false
  }
  return true
}

function handleRequest(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('module', props.module || 'common')
  loading.value = true
  uploadFile(formData)
    .then((res) => {
      const url = res && res.url ? res.url : ''
      if (!url) {
        ElMessage.error('上传失败：未返回地址')
        return
      }
      emit('update:modelValue', url)
      ElMessage.success('上传成功')
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false
    })
}

function onInput(value) {
  emit('update:modelValue', value || '')
}

function clear() {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload__row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.image-upload__btn {
  flex-shrink: 0;
}

.image-upload__preview {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.image-upload__img {
  width: 120px;
  height: 80px;
  border: 1px solid var(--rh-border);
  border-radius: 6px;
  background: #fafbfc;
}
</style>
