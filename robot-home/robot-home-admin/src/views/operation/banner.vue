<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-select v-model="query.position" placeholder="位置" style="width: 140px" @change="loadList">
          <el-option label="PC 端" value="pc" />
          <el-option label="App 端" value="app" />
        </el-select>
        <el-button type="primary" @click="loadList">刷新</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">Banner 管理</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增 Banner</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="140">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" :preview-src-list="[row.image]" fit="cover" class="banner-thumb" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="position" label="位置" width="90" />
        <el-table-column prop="url" label="跳转链接" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑 Banner' : '新增 Banner'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="位置" prop="position">
          <el-radio-group v-model="form.position">
            <el-radio value="pc">PC</el-radio>
            <el-radio value="app">App</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="可选标题" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <image-upload v-model="form.image" module="banner" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.url" placeholder="点击跳转 URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="可选"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="可选"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBannerList, saveBanner, deleteBanner } from '@/api/operation'
import ImageUpload from '@/components/ImageUpload.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const query = reactive({ position: 'pc' })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  position: [{ required: true, message: '请选择位置', trigger: 'change' }],
  image: [{ required: true, message: '请上传图片', trigger: 'change' }]
}

function createForm() {
  return {
    id: null,
    position: 'pc',
    title: '',
    image: '',
    url: '',
    sort: 0,
    status: 1,
    startTime: '',
    endTime: ''
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await getBannerList(query.position)
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, createForm(), { position: query.position || 'pc' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, createForm(), {
    id: row.id,
    position: row.position || 'pc',
    title: row.title || '',
    image: row.image || '',
    url: row.url || '',
    sort: row.sort ?? 0,
    status: row.status === undefined ? 1 : row.status,
    startTime: row.startTime || '',
    endTime: row.endTime || ''
  })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveBanner({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    query.position = form.position
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除 Banner「${row.title || row.id}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteBanner(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.banner-thumb {
  width: 100px;
  height: 48px;
  border-radius: 4px;
  border: 1px solid var(--rh-border);
}
</style>
