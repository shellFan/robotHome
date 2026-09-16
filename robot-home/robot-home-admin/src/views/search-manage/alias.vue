<template>
  <div class="app-container">
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between;">
      <h3>搜索别名管理</h3>
      <el-button type="primary" @click="showAdd = true">新增别名</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="alias" label="别名" width="150" />
      <el-table-column prop="targetType" label="目标类型" width="120" />
      <el-table-column prop="targetId" label="目标ID" width="100" />
      <el-table-column prop="targetName" label="目标名称" min-width="150" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="handleToggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button type="danger" text size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="(p) => { pageNum = p; loadList() }" />

    <el-dialog v-model="showAdd" title="新增别名" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="别名" required><el-input v-model="form.alias" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="目标类型" required>
          <el-select v-model="form.targetType"><el-option label="机器人" value="robot" /><el-option label="品牌" value="brand" /><el-option label="企业" value="company" /></el-select>
        </el-form-item>
        <el-form-item label="目标ID" required><el-input v-model="form.targetId" type="number" /></el-form-item>
        <el-form-item label="目标名称"><el-input v-model="form.targetName" maxlength="200" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAliasPage, createAlias, toggleAliasStatus } from '@/api/searchManage'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showAdd = ref(false)
const submitting = ref(false)
const form = ref({ alias: '', targetType: 'robot', targetId: '', targetName: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await getAliasPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.data) { list.value = res.data.list || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}

async function handleToggle(row) {
  await toggleAliasStatus(row.id)
  ElMessage.success('操作成功')
  loadList()
}

async function handleDelete(row) {
  // No delete API yet, use toggle to disable
  if (row.status === 1) { await toggleAliasStatus(row.id) }
  ElMessage.success('已停用')
  loadList()
}

async function handleAdd() {
  if (!form.value.alias.trim()) return ElMessage.warning('请输入别名')
  if (!form.value.targetId) return ElMessage.warning('请输入目标ID')
  const targetId = Number(form.value.targetId)
  if (isNaN(targetId) || targetId <= 0) return ElMessage.warning('目标ID格式不正确')
  submitting.value = true
  try {
    await createAlias({ alias: form.value.alias.trim(), targetType: form.value.targetType, targetId: targetId, targetName: form.value.targetName.trim() })
    ElMessage.success('添加成功')
    showAdd.value = false
    form.value = { alias: '', targetType: 'robot', targetId: '', targetName: '' }
    loadList()
  } finally { submitting.value = false }
}

onMounted(loadList)
</script>