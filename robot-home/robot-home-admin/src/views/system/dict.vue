<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.dictType"
          placeholder="字典类型，如 robot_status"
          clearable
          style="width: 240px"
          @keyup.enter="loadList"
        />
        <el-button type="primary" @click="loadList">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">数据字典</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增字典</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="dictType" label="字典类型" min-width="160" />
        <el-table-column prop="dictLabel" label="标签" min-width="140" />
        <el-table-column prop="dictValue" label="值" min-width="120" />
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
      :title="form.id ? '编辑字典' : '新增字典'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" placeholder="如：robot_status" />
        </el-form-item>
        <el-form-item label="标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="存储值" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDicts, saveDict, deleteDict } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const query = reactive({ dictType: '' })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  status: 1
})
const rules = {
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入值', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const data = await getDicts(query.dictType || undefined)
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.dictType = ''
  loadList()
}

function openCreate() {
  Object.assign(form, {
    id: null,
    dictType: query.dictType || '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
    status: 1
  })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    dictType: row.dictType || '',
    dictLabel: row.dictLabel || '',
    dictValue: row.dictValue || '',
    sort: row.sort ?? 0,
    status: row.status === undefined ? 1 : row.status
  })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveDict({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典「${row.dictLabel}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteDict(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>
