<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-select
          v-model="query.brandId"
          placeholder="所属品牌"
          clearable
          filterable
          style="width: 220px"
          @change="handleSearch"
        >
          <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">产品系列</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增系列</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="系列名称" min-width="180" />
        <el-table-column label="品牌" width="160">
          <template #default="{ row }">{{ brandName(row.brandId) }}</template>
        </el-table-column>
        <el-table-column label="分类" width="160">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
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
          <el-empty description="请选择品牌后查看系列，或暂无数据" />
        </template>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑系列' : '新增系列'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属品牌" prop="brandId">
          <el-select v-model="form.brandId" filterable placeholder="请选择品牌" style="width: 100%">
            <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分类">
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="请选择分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="系列名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入系列名称" />
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRobotSeries,
  saveRobotSeries,
  deleteRobotSeries,
  getRobotCategories
} from '@/api/robot'
import { getBrandAll } from '@/api/brand'
import { buildTree } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const brands = ref([])
const categories = ref([])
const categoryTree = computed(() => buildTree(categories.value))
const query = reactive({ brandId: null })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  name: [{ required: true, message: '请输入系列名称', trigger: 'blur' }]
}

function createForm() {
  return { id: null, brandId: null, categoryId: null, name: '', sort: 0, status: 1 }
}

function brandName(id) {
  const hit = brands.value.find((b) => String(b.id) === String(id))
  return hit ? hit.name : '-'
}

function categoryName(id) {
  const hit = categories.value.find((c) => String(c.id) === String(id))
  return hit ? hit.name : '-'
}

async function loadOptions() {
  const [brandData, cats] = await Promise.all([getBrandAll(), getRobotCategories()])
  brands.value = Array.isArray(brandData) ? brandData : []
  categories.value = Array.isArray(cats) ? cats : []
}

async function loadList() {
  if (!query.brandId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    const data = await getRobotSeries(query.brandId)
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadList()
}

function handleReset() {
  query.brandId = null
  list.value = []
}

function openCreate() {
  Object.assign(form, createForm(), { brandId: query.brandId || null })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, createForm(), {
    id: row.id,
    brandId: row.brandId,
    categoryId: row.categoryId || null,
    name: row.name || '',
    sort: row.sort ?? 0,
    status: row.status === undefined ? 1 : row.status
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveRobotSeries({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    if (!query.brandId) query.brandId = form.brandId
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除系列「${row.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteRobotSeries(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  await loadOptions()
})
</script>
