<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :span="10">
        <div class="page-card">
          <div class="table-toolbar">
            <span class="page-title">推荐位</span>
            <el-button type="primary" :icon="'Plus'" size="small" @click="openPosCreate">新增</el-button>
          </div>
          <el-table
            v-loading="posLoading"
            :data="positions"
            border
            stripe
            highlight-current-row
            @current-change="onSelectPos"
          >
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column prop="code" label="编码" width="110" />
            <el-table-column prop="bizType" label="类型" width="90" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openPosEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <el-col :span="14">
        <div class="page-card">
          <div class="table-toolbar">
            <span class="page-title">推荐内容{{ currentPos ? ` — ${currentPos.name}` : '' }}</span>
            <el-button type="primary" :icon="'Plus'" :disabled="!currentPos" @click="openItemCreate">
              新增内容
            </el-button>
          </div>
          <el-table v-loading="itemLoading" :data="items" border stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="封面" width="90">
              <template #default="{ row }">
                <el-image v-if="row.image" :src="row.image" fit="cover" class="item-thumb" />
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="140" />
            <el-table-column prop="bizType" label="类型" width="90" />
            <el-table-column prop="bizId" label="业务ID" width="90" />
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openItemEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDeleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="currentPos ? '暂无内容' : '请先选择推荐位'" />
            </template>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="posVisible" :title="posForm.id ? '编辑推荐位' : '新增推荐位'" width="480px" destroy-on-close>
      <el-form ref="posRef" :model="posForm" :rules="posRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="posForm.name" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="posForm.code" placeholder="如：home_hot" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="posForm.bizType" style="width: 100%">
            <el-option label="机器人" value="robot" />
            <el-option label="文章" value="article" />
            <el-option label="视频" value="video" />
            <el-option label="企业" value="company" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="posForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="posVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="savePos">保 存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemVisible" :title="itemForm.id ? '编辑推荐内容' : '新增推荐内容'" width="560px" destroy-on-close>
      <el-form ref="itemRef" :model="itemForm" :rules="itemRules" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="itemForm.title" />
        </el-form-item>
        <el-form-item label="封面">
          <image-upload v-model="itemForm.image" module="recommend" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="itemForm.bizType" style="width: 100%">
            <el-option label="机器人" value="robot" />
            <el-option label="文章" value="article" />
            <el-option label="视频" value="video" />
            <el-option label="企业" value="company" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务ID">
          <el-input-number v-model="itemForm.bizId" :min="1" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="itemForm.url" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveItem">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRecommendPositions,
  saveRecommendPosition,
  getRecommendItems,
  saveRecommendItem,
  deleteRecommendItem
} from '@/api/operation'
import ImageUpload from '@/components/ImageUpload.vue'

const posLoading = ref(false)
const itemLoading = ref(false)
const saving = ref(false)
const positions = ref([])
const items = ref([])
const currentPos = ref(null)

const posVisible = ref(false)
const posRef = ref(null)
const posForm = reactive({ id: null, name: '', code: '', bizType: 'robot', status: 1 })
const posRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }]
}

const itemVisible = ref(false)
const itemRef = ref(null)
const itemForm = reactive({
  id: null,
  positionId: null,
  bizType: 'robot',
  bizId: null,
  title: '',
  image: '',
  url: '',
  sort: 0,
  status: 1
})
const itemRules = {}

async function loadPositions() {
  posLoading.value = true
  try {
    const data = await getRecommendPositions()
    positions.value = Array.isArray(data) ? data : []
  } catch (e) {
    positions.value = []
  } finally {
    posLoading.value = false
  }
}

async function loadItems() {
  if (!currentPos.value) {
    items.value = []
    return
  }
  itemLoading.value = true
  try {
    const data = await getRecommendItems(currentPos.value.id)
    items.value = Array.isArray(data) ? data : []
  } catch (e) {
    items.value = []
  } finally {
    itemLoading.value = false
  }
}

function onSelectPos(row) {
  currentPos.value = row
  loadItems()
}

function openPosCreate() {
  Object.assign(posForm, { id: null, name: '', code: '', bizType: 'robot', status: 1 })
  posVisible.value = true
}

function openPosEdit(row) {
  Object.assign(posForm, {
    id: row.id,
    name: row.name || '',
    code: row.code || '',
    bizType: row.bizType || 'robot',
    status: row.status === undefined ? 1 : row.status
  })
  posVisible.value = true
}

async function savePos() {
  const valid = await posRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveRecommendPosition({ ...posForm })
    ElMessage.success('保存成功')
    posVisible.value = false
    loadPositions()
  } finally {
    saving.value = false
  }
}

function openItemCreate() {
  Object.assign(itemForm, {
    id: null,
    positionId: currentPos.value.id,
    bizType: currentPos.value.bizType || 'robot',
    bizId: null,
    title: '',
    image: '',
    url: '',
    sort: 0,
    status: 1
  })
  itemVisible.value = true
}

function openItemEdit(row) {
  Object.assign(itemForm, {
    id: row.id,
    positionId: currentPos.value.id,
    bizType: row.bizType || 'robot',
    bizId: row.bizId,
    title: row.title || '',
    image: row.image || '',
    url: row.url || '',
    sort: row.sort ?? 0,
    status: row.status === undefined ? 1 : row.status
  })
  itemVisible.value = true
}

async function saveItem() {
  saving.value = true
  try {
    await saveRecommendItem({ ...itemForm })
    ElMessage.success('保存成功')
    itemVisible.value = false
    loadItems()
  } finally {
    saving.value = false
  }
}

async function handleDeleteItem(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.title || row.id}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteRecommendItem(row.id)
  ElMessage.success('删除成功')
  loadItems()
}

onMounted(loadPositions)
</script>

<style scoped>
.item-thumb {
  width: 56px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid var(--rh-border);
}
</style>
