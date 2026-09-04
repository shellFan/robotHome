<template>
  <div class="rich-editor">
    <toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" class="rich-editor__toolbar" />
    <editor
      :model-value="modelValue"
      :default-config="editorConfig"
      mode="default"
      class="rich-editor__body"
      :style="{ height: height + 'px' }"
      @on-created="handleCreated"
      @update:model-value="onUpdate"
    />
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { uploadFile } from '@/api/file'

const props = defineProps({
  modelValue: { type: String, default: '' },
  /** 上传模块目录 */
  module: { type: String, default: 'article' },
  height: { type: Number, default: 420 }
})
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef(null)
const toolbarConfig = ref({})

const editorConfig = ref({
  placeholder: '请输入正文…',
  MENU_CONF: {
    uploadImage: {
      customUpload(file, insertFn) {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('module', props.module || 'article')
        uploadFile(formData)
          .then((res) => {
            if (res && res.url) {
              insertFn(res.url, res.name || '', res.url)
            } else {
              ElMessage.error('图片上传失败')
            }
          })
          .catch(() => {})
        return false
      }
    },
    uploadVideo: {
      customUpload(file, insertFn) {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('module', props.module || 'article')
        uploadFile(formData)
          .then((res) => {
            if (res && res.url) {
              insertFn(res.url, '')
            } else {
              ElMessage.error('视频上传失败')
            }
          })
          .catch(() => {})
        return false
      }
    }
  }
})

function handleCreated(editor) {
  editorRef.value = editor
}

function onUpdate(html) {
  emit('update:modelValue', html || '')
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
    editorRef.value = null
  }
})
</script>

<style scoped>
.rich-editor {
  width: 100%;
  border: 1px solid var(--rh-border);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.rich-editor__toolbar {
  border-bottom: 1px solid var(--rh-border);
}

.rich-editor__body {
  overflow-y: auto;
}
</style>
