<template>
  <div>
    <div class="filter-bar" style="margin-bottom: 12px">
      <el-button type="primary" :icon="'Plus'" @click="addRow">新增图片</el-button>
      <el-button :loading="saving" @click="save">保存图片</el-button>
      <span class="form-tip">保存会按当前顺序整组覆盖；拖动排序可用上下箭头调整</span>
    </div>

    <el-table :data="rows" border size="small">
      <el-table-column label="#" width="60">
        <template #default="{ $index }">{{ $index + 1 }}</template>
      </el-table-column>
      <el-table-column label="图片" width="120">
        <template #default="{ row }">
          <el-image v-if="row.url" :src="row.url" :preview-src-list="[row.url]" fit="contain" class="thumb" />
          <span v-else class="text-weak">-</span>
        </template>
      </el-table-column>
      <el-table-column label="地址" min-width="280">
        <template #default="{ row }">
          <div class="cell-row">
            <el-input v-model="row.url" placeholder="图片地址" />
            <el-upload
              :show-file-list="false"
              :http-request="(opt) => upload(opt, row)"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button size="small">上传</el-button>
            </el-upload>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="160">
        <template #default="{ row }">
          <el-select v-model="row.type" placeholder="类型" clearable style="width: 100%">
            <el-option label="外观" value="appearance" />
            <el-option label="细节" value="detail" />
            <el-option label="场景" value="scene" />
            <el-option label="尺寸" value="size" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="排序" width="110">
        <template #default="{ $index }">
          <el-button :disabled="$index === 0" link @click="move($index, -1)">上移</el-button>
          <el-button :disabled="$index === rows.length - 1" link @click="move($index, 1)">下移</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无图片" />
      </template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRobotImages, saveRobotImages } from '@/api/robot'
import { uploadFile } from '@/api/file'

const props = defineProps({
  robotId: { type: [Number, String], required: true }
})

const rows = ref([])
const saving = ref(false)

function beforeUpload(file) {
  const ok = /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
  if (!ok) ElMessage.error('仅支持 jpg/png/gif/webp/bmp 图片格式')
  return ok
}

function upload(options, row) {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('module', 'robot')
  return uploadFile(formData)
    .then((res) => {
      if (res && res.url) {
        row.url = res.url
        ElMessage.success('上传成功')
      }
    })
    .catch(() => {})
}

function addRow() {
  rows.value.push({ url: '', type: 'appearance' })
}

function removeRow(index) {
  rows.value.splice(index, 1)
}

function move(index, step) {
  const target = index + step
  if (target < 0 || target >= rows.value.length) return
  const arr = rows.value.slice()
  const tmp = arr[index]
  arr[index] = arr[target]
  arr[target] = tmp
  rows.value = arr
}

async function load() {
  const data = await getRobotImages(props.robotId)
  const list = Array.isArray(data) ? data : []
  rows.value = list.map((item) => ({ url: item.url || '', type: item.type || 'appearance' }))
}

async function save() {
  const payload = rows.value
    .filter((r) => r.url)
    .map((r) => ({ url: r.url, type: r.type || 'appearance' }))
  saving.value = true
  try {
    await saveRobotImages(props.robotId, payload)
    ElMessage.success('图片已保存')
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
  width: 80px;
  height: 56px;
  border: 1px solid var(--rh-border);
  border-radius: 4px;
  background: #fafbfc;
}

.cell-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
