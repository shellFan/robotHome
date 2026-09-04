<template>
  <div class="params-panel">
    <div class="filter-bar" style="margin-bottom: 12px">
      <span class="text-secondary">参数模板</span>
      <el-select
        v-model="templateId"
        placeholder="请选择参数模板"
        style="width: 240px"
        @change="handleTemplateChange"
      >
        <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-button :loading="saving" type="primary" @click="save">保存参数</el-button>
      <span class="form-tip">切换模板会重新加载参数项；留空的值提交后会被清除</span>
    </div>

    <el-empty v-if="!groups.length" description="该模板暂无参数分组，请先在「参数模板」中配置" />
    <div v-else>
      <div v-for="group in groups" :key="group.id" class="param-group">
        <div class="param-group__title">{{ group.name }}</div>
        <el-form label-width="140px" class="param-group__form">
          <el-form-item v-for="def in group.defs" :key="def.id" :label="labelOf(def)">
            <el-select
              v-if="def.type === 'select'"
              v-model="valueMap[def.id]"
              :placeholder="`请选择${def.name}`"
              clearable
              style="width: 260px"
            >
              <el-option v-for="opt in optionsOf(def)" :key="opt" :label="opt" :value="opt" />
            </el-select>
            <el-input
              v-else-if="def.type === 'number'"
              v-model="valueMap[def.id]"
              type="number"
              :placeholder="`请输入${def.name}`"
              clearable
              style="width: 260px"
            />
            <el-input
              v-else
              v-model="valueMap[def.id]"
              :placeholder="`请输入${def.name}`"
              clearable
              style="width: 260px"
            />
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRobotTemplates,
  getRobotTemplateDetail,
  getRobotParams,
  saveRobotParams
} from '@/api/robot'

const props = defineProps({
  robotId: { type: [Number, String], required: true },
  defaultTemplateId: { type: [Number, String], default: null }
})

const templates = ref([])
const templateId = ref(null)
const groups = ref([])
const valueMap = reactive({})
const saving = ref(false)

function labelOf(def) {
  return def.unit ? `${def.name}(${def.unit})` : def.name
}

function optionsOf(def) {
  if (!def.options) return []
  return String(def.options)
    .split(/[,，|]/)
    .map((s) => s.trim())
    .filter(Boolean)
}

async function loadTemplates() {
  const data = await getRobotTemplates()
  templates.value = Array.isArray(data) ? data : []
  if (!templates.value.length) return
  const preset = props.defaultTemplateId || templateId.value
  const matched = templates.value.find((t) => String(t.id) === String(preset))
  templateId.value = matched ? matched.id : templates.value[0].id
}

async function loadValues() {
  Object.keys(valueMap).forEach((k) => delete valueMap[k])
  const list = await getRobotParams(props.robotId)
  ;(Array.isArray(list) ? list : []).forEach((item) => {
    valueMap[item.defId] = item.value ?? ''
  })
}

async function loadTemplate() {
  if (!templateId.value) {
    groups.value = []
    return
  }
  const data = await getRobotTemplateDetail(templateId.value)
  const raw = (data && data.groups) || []
  groups.value = raw.map((g) => ({
    id: g.group.id,
    name: g.group.name,
    sort: g.group.sort,
    defs: (g.defs || []).map((d) => d.def || d)
  }))
}

async function handleTemplateChange() {
  await loadTemplate()
}

async function load() {
  await loadTemplates()
  await loadTemplate()
  await loadValues()
}

async function save() {
  const items = []
  groups.value.forEach((group) => {
    group.defs.forEach((def) => {
      items.push({ defId: def.id, value: valueMap[def.id] === undefined ? '' : String(valueMap[def.id] ?? '') })
    })
  })
  if (!items.length) {
    ElMessage.warning('当前模板没有可保存的参数项')
    return
  }
  saving.value = true
  try {
    await saveRobotParams({ robotId: props.robotId, items })
    ElMessage.success('参数已保存')
    await loadValues()
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
.param-group {
  border: 1px solid var(--rh-border);
  border-radius: var(--rh-radius);
  margin-bottom: 12px;
  overflow: hidden;
}

.param-group__title {
  padding: 8px 14px;
  background: #fafbfc;
  border-bottom: 1px solid var(--rh-border);
  font-weight: 600;
  font-size: 13px;
  color: var(--rh-text);
}

.param-group__form {
  padding: 14px 14px 0;
}

.param-group__form :deep(.el-form-item) {
  margin-bottom: 14px;
}
</style>
