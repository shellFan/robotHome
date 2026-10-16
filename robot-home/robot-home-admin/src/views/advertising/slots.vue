<template>
  <div class="app-container">
    <div class="page-header">
      <h3>广告位管理</h3>
      <el-button type="primary" @click="openCreate">新建广告位</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="slotCode" label="编码" width="160" />
      <el-table-column prop="slotName" label="名称" min-width="140" />
      <el-table-column prop="page" label="页面" width="130" />
      <el-table-column prop="position" label="位置" width="130" />
      <el-table-column prop="adType" label="类型" width="100" />
      <el-table-column prop="maxAds" label="最大数" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'ACTIVE'" text size="small" type="warning" @click="toggleStatus(row, 'INACTIVE')">停用</el-button>
          <el-button v-else text size="small" type="success" @click="toggleStatus(row, 'ACTIVE')">启用</el-button>
          <el-button text size="small" @click="viewStats(row)">统计</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑广告位 -->
    <el-dialog v-model="showForm" :title="form.id ? '编辑广告位' : '新建广告位'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编码"><el-input v-model="form.slotCode" placeholder="如 home_banner" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.slotName" /></el-form-item>
        <el-form-item label="页面"><el-input v-model="form.page" placeholder="如 home / robot_detail" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.position" placeholder="如 top / sidebar" /></el-form-item>
        <el-form-item label="广告类型">
          <el-select v-model="form.adType" style="width: 100%;">
            <el-option label="图片" value="IMAGE" />
            <el-option label="文字" value="TEXT" />
            <el-option label="视频" value="VIDEO" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大展示数"><el-input-number v-model="form.maxAds" :min="1" :max="20" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 统计 -->
    <el-dialog v-model="showStats" title="广告位统计" width="440px">
      <el-descriptions v-if="stats" :column="1" border size="small">
        <el-descriptions-item v-for="(val, key) in stats" :key="key" :label="statLabel(key)">{{ val }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAllSlots, saveSlot, updateSlotStatus, getSlotStats } from '@/api/advertising'

const loading = ref(false)
const list = ref([])

const showForm = ref(false)
const saving = ref(false)
const form = ref({ slotCode: '', slotName: '', page: '', position: '', adType: 'IMAGE', maxAds: 3 })

const showStats = ref(false)
const stats = ref(null)

function statLabel(key) {
  return { totalCampaigns: '投放数', totalImpressions: '曝光量', totalClicks: '点击量', ctr: '点击率', activeCampaigns: '活跃投放' }[key] || key
}

async function loadList() {
  loading.value = true
  try {
    const data = await listAllSlots()
    list.value = data || []
  } finally { loading.value = false }
}

function openCreate() {
  form.value = { slotCode: '', slotName: '', page: '', position: '', adType: 'IMAGE', maxAds: 3 }
  showForm.value = true
}

function openEdit(row) {
  form.value = { ...row }
  showForm.value = true
}

async function save() {
  saving.value = true
  try {
    await saveSlot(form.value)
    ElMessage.success('保存成功')
    showForm.value = false
    loadList()
  } finally { saving.value = false }
}

async function toggleStatus(row, status) {
  await updateSlotStatus(row.id, status)
  ElMessage.success(status === 'ACTIVE' ? '已启用' : '已停用')
  loadList()
}

async function viewStats(row) {
  const data = await getSlotStats(row.id)
  stats.value = data
  showStats.value = true
}

onMounted(loadList)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
</style>