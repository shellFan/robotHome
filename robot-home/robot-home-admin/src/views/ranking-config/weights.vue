<template>
  <div class="page-container">
    <!-- 权重管理 -->
    <div class="page-card">
      <div class="table-toolbar">
        <span class="page-title">排名权重配置</span>
        <el-button type="primary" size="small" @click="openAddWeight">新增权重</el-button>
      </div>

      <el-table v-loading="weightsLoading" :data="weights" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="权重名称" min-width="140" />
        <el-table-column prop="key" label="权重Key" width="160" />
        <el-table-column prop="weight" label="权重值" width="100">
          <template #default="{ row }">
            <span class="weight-value">{{ row.weight }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditWeight(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteWeight(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无权重配置" />
        </template>
      </el-table>
    </div>

    <!-- 衰减配置 -->
    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">时间衰减配置</span>
      </div>

      <el-table v-loading="decayLoading" :data="decayConfigs" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="配置名称" min-width="140" />
        <el-table-column prop="key" label="配置Key" width="160" />
        <el-table-column prop="halfLifeDays" label="半衰期(天)" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDecay(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无衰减配置" />
        </template>
      </el-table>
    </div>

    <!-- 操作区 -->
    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">排名操作</span>
      </div>
      <div style="display: flex; gap: 12px; margin-top: 8px">
        <el-button type="primary" :loading="refreshing" @click="handleRefresh">刷新排名</el-button>
        <el-button type="success" :loading="snapshotting" @click="handleSnapshot">创建快照</el-button>
      </div>
    </div>

    <!-- 权重编辑弹窗 -->
    <el-dialog v-model="weightDialogVisible" :title="weightForm.id ? '编辑权重' : '新增权重'" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="权重名称">
          <el-input v-model="weightForm.name" placeholder="如：性能分数" />
        </el-form-item>
        <el-form-item label="权重Key">
          <el-input v-model="weightForm.key" placeholder="如：performanceScore" />
        </el-form-item>
        <el-form-item label="权重值">
          <el-input-number v-model="weightForm.weight" :min="0" :max="100" :precision="2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="weightForm.description" type="textarea" :rows="2" placeholder="权重说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="weightDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveWeight">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 衰减编辑弹窗 -->
    <el-dialog v-model="decayDialogVisible" title="编辑衰减配置" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="配置名称">
          <el-input v-model="decayForm.name" disabled />
        </el-form-item>
        <el-form-item label="配置Key">
          <el-input v-model="decayForm.key" disabled />
        </el-form-item>
        <el-form-item label="半衰期(天)">
          <el-input-number v-model="decayForm.halfLifeDays" :min="1" :max="3650" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="decayForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decayDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDecay">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRankingWeights,
  addRankingWeight,
  updateRankingWeight,
  getDecayConfigs,
  updateDecayConfig,
  refreshRanking,
  createRankingSnapshot
} from '@/api/ranking'

const weightsLoading = ref(false)
const decayLoading = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const snapshotting = ref(false)

const weights = ref([])
const decayConfigs = ref([])

// 权重弹窗
const weightDialogVisible = ref(false)
const weightForm = ref({ id: null, name: '', key: '', weight: 1, description: '' })

// 衰减弹窗
const decayDialogVisible = ref(false)
const decayForm = ref({ id: null, name: '', key: '', halfLifeDays: 30, description: '' })

async function loadWeights() {
  weightsLoading.value = true
  try {
    const data = await getRankingWeights()
    weights.value = Array.isArray(data) ? data : []
  } catch (e) {
    weights.value = []
  } finally {
    weightsLoading.value = false
  }
}

async function loadDecayConfigs() {
  decayLoading.value = true
  try {
    const data = await getDecayConfigs()
    decayConfigs.value = Array.isArray(data) ? data : []
  } catch (e) {
    decayConfigs.value = []
  } finally {
    decayLoading.value = false
  }
}

function openAddWeight() {
  weightForm.value = { id: null, name: '', key: '', weight: 1, description: '' }
  weightDialogVisible.value = true
}

function openEditWeight(row) {
  weightForm.value = { ...row }
  weightDialogVisible.value = true
}

async function saveWeight() {
  const form = weightForm.value
  if (!form.name || !form.key) {
    ElMessage.warning('请填写权重名称和Key')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateRankingWeight(form.id, form)
    } else {
      await addRankingWeight(form)
    }
    ElMessage.success('保存成功')
    weightDialogVisible.value = false
    loadWeights()
  } finally {
    saving.value = false
  }
}

async function deleteWeight(row) {
  try {
    await ElMessageBox.confirm(`确认删除权重「${row.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  // 使用update将weight设为0来"删除"（或后端有delete接口则调用）
  ElMessage.info('请通过编辑将权重设为0来禁用该权重')
}

function openEditDecay(row) {
  decayForm.value = { ...row }
  decayDialogVisible.value = true
}

async function saveDecay() {
  const form = decayForm.value
  if (!form.halfLifeDays) {
    ElMessage.warning('请填写半衰期')
    return
  }
  saving.value = true
  try {
    await updateDecayConfig(form.id, form)
    ElMessage.success('保存成功')
    decayDialogVisible.value = false
    loadDecayConfigs()
  } finally {
    saving.value = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshRanking()
    ElMessage.success('排名刷新成功')
  } finally {
    refreshing.value = false
  }
}

async function handleSnapshot() {
  snapshotting.value = true
  try {
    await createRankingSnapshot()
    ElMessage.success('快照创建成功')
  } finally {
    snapshotting.value = false
  }
}

onMounted(() => {
  loadWeights()
  loadDecayConfigs()
})
</script>

<style scoped>
.weight-value {
  font-weight: 600;
  color: #1668dc;
}
</style>