<template>
  <div>
    <div class="filter-bar" style="margin-bottom: 12px">
      <el-button type="primary" :icon="'Plus'" @click="addRow">新增视频</el-button>
      <el-button :loading="saving" @click="save">保存视频</el-button>
      <span class="form-tip">保存会按当前顺序整组覆盖</span>
    </div>

    <el-table :data="rows" border size="small">
      <el-table-column label="#" width="60">
        <template #default="{ $index }">{{ $index + 1 }}</template>
      </el-table-column>
      <el-table-column label="标题" min-width="180">
        <template #default="{ row }">
          <el-input v-model="row.title" placeholder="视频标题" />
        </template>
      </el-table-column>
      <el-table-column label="视频地址" min-width="280">
        <template #default="{ row }">
          <div class="cell-row">
            <el-input v-model="row.url" placeholder="视频地址" />
            <el-upload
              :show-file-list="false"
              :http-request="(opt) => uploadVideo(opt, row)"
              accept="video/*"
            >
              <el-button size="small">上传</el-button>
            </el-upload>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="封面" width="220">
        <template #default="{ row }">
          <div class="cell-row">
            <el-image v-if="row.cover" :src="row.cover" :preview-src-list="[row.cover]" fit="contain" class="thumb" />
            <el-input v-model="row.cover" placeholder="封面地址" />
            <el-upload
              :show-file-list="false"
              :http-request="(opt) => uploadCover(opt, row)"
              :before-upload="beforeImage"
              accept="image/*"
            >
              <el-button size="small">上传</el-button>
            </el-upload>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="时长(秒)" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.duration" :min="0" :controls="false" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无视频" />
      </template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRobotVideos, saveRobotVideos } from '@/api/robot'
import { uploadFile } from '@/api/file'

const props = defineProps({
  robotId: { type: [Number, String], required: true }
})

const rows = ref([])
const saving = ref(false)

function beforeImage(file) {
  const ok = /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
  if (!ok) ElMessage.error('仅支持 jpg/png/gif/webp/bmp 图片格式')
  return ok
}

function doUpload(options, module, apply) {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('module', module)
  return uploadFile(formData)
    .then((res) => {
      if (res && res.url) {
        apply(res.url)
        ElMessage.success('上传成功')
      }
    })
    .catch(() => {})
}

function uploadVideo(options, row) {
  return doUpload(options, 'robot', (url) => {
    row.url = url
  })
}

function uploadCover(options, row) {
  return doUpload(options, 'robot', (url) => {
    row.cover = url
  })
}

function addRow() {
  rows.value.push({ title: '', url: '', cover: '', duration: 0 })
}

function removeRow(index) {
  rows.value.splice(index, 1)
}

async function load() {
  const data = await getRobotVideos(props.robotId)
  const list = Array.isArray(data) ? data : []
  rows.value = list.map((item) => ({
    title: item.title || '',
    url: item.url || '',
    cover: item.cover || '',
    duration: item.duration ?? 0
  }))
}

async function save() {
  const payload = rows.value
    .filter((r) => r.url)
    .map((r) => ({
      title: r.title || '',
      url: r.url,
      cover: r.cover || '',
      duration: r.duration || 0
    }))
  saving.value = true
  try {
    await saveRobotVideos(props.robotId, payload)
    ElMessage.success('视频已保存')
    await load()
  } finally {
    saving.value = false
  }
}

watch(
  () => props.robotId,
  () => {
    if (props.robotId) load()
  }
)

onMounted(() => {
  if (props.robotId) load()
})

defineExpose({ load, save })
</script>

<style scoped>
.thumb {
  width: 56px;
  height: 40px;
  border: 1px solid var(--rh-border);
  border-radius: 4px;
  flex-shrink: 0;
}

.cell-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
