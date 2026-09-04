<template>
  <div>
    <div class="filter-bar" style="margin-bottom: 12px">
      <el-button :loading="saving" type="primary" @click="save">保存标签</el-button>
      <span class="form-tip">三类标签分别整组覆盖保存；可输入自定义标签后回车添加</span>
    </div>

    <el-form label-width="110px">
      <el-form-item label="应用场景 scene">
        <el-select
          v-model="tags.scene"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="如：家庭陪伴 / 工业搬运"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="开发能力 dev">
        <el-select
          v-model="tags.dev"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="如：开放 SDK / 支持二次开发"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="AI 能力 ai">
        <el-select
          v-model="tags.ai"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="如：语音交互 / 视觉识别"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRobotTags, saveRobotTags } from '@/api/robot'

const props = defineProps({
  robotId: { type: [Number, String], required: true }
})

const tags = reactive({ scene: [], dev: [], ai: [] })
const saving = ref(false)
const TYPES = ['scene', 'dev', 'ai']

async function load() {
  const data = await getRobotTags(props.robotId)
  const list = Array.isArray(data) ? data : []
  TYPES.forEach((type) => {
    tags[type] = list.filter((item) => item.tagType === type).map((item) => item.tagValue)
  })
}

async function save() {
  saving.value = true
  try {
    for (const type of TYPES) {
      await saveRobotTags(props.robotId, type, tags[type] || [])
    }
    ElMessage.success('标签已保存')
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
