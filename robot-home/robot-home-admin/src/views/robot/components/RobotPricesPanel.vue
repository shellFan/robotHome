<template>
  <div>
    <div class="filter-bar" style="margin-bottom: 12px">
      <el-button type="primary" :icon="'Plus'" @click="addRow">新增价格</el-button>
      <el-button :loading="saving" @click="save">保存价格</el-button>
      <span class="form-tip">保存会整组覆盖；渠道如「官方 / 经销商」，地区如「华东」</span>
    </div>

    <el-table :data="rows" border size="small">
      <el-table-column label="#" width="60">
        <template #default="{ $index }">{{ $index + 1 }}</template>
      </el-table-column>
      <el-table-column label="渠道" min-width="200">
        <template #default="{ row }">
          <el-input v-model="row.channel" placeholder="如：官方旗舰店" />
        </template>
      </el-table-column>
      <el-table-column label="地区" min-width="160">
        <template #default="{ row }">
          <el-input v-model="row.region" placeholder="如：全国 / 华东" />
        </template>
      </el-table-column>
      <el-table-column label="价格(元)" width="200">
        <template #default="{ row }">
          <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无价格" />
      </template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRobotPrices, saveRobotPrices } from '@/api/robot'

const props = defineProps({
  robotId: { type: [Number, String], required: true }
})

const rows = ref([])
const saving = ref(false)

function addRow() {
  rows.value.push({ channel: '', region: '', price: 0 })
}

function removeRow(index) {
  rows.value.splice(index, 1)
}

async function load() {
  const data = await getRobotPrices(props.robotId)
  const list = Array.isArray(data) ? data : []
  rows.value = list.map((item) => ({
    channel: item.channel || '',
    region: item.region || '',
    price: item.price === null || item.price === undefined ? 0 : Number(item.price)
  }))
}

async function save() {
  const payload = rows.value.map((r) => ({
    channel: r.channel || '',
    region: r.region || '',
    price: r.price || 0
  }))
  saving.value = true
  try {
    await saveRobotPrices(props.robotId, payload)
    ElMessage.success('价格已保存')
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
