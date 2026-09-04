<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="page-title">机器人分类</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate(0)">新增分类</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        border
        stripe
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="图标" width="90">
          <template #default="{ row }">
            <el-image v-if="row.icon" :src="row.icon" fit="contain" class="icon-thumb" />
            <span v-else class="text-weak">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="80" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row.id)">新增子类</el-button>
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
      :title="form.id ? '编辑分类' : '新增分类'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="无（顶级分类）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="图标">
          <image-upload v-model="form.icon" module="robot" />
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
import { getRobotCategories, saveRobotCategory, deleteRobotCategory } from '@/api/robot'
import ImageUpload from '@/components/ImageUpload.vue'
import { buildTree } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const tree = computed(() => buildTree(list.value))
const parentOptions = computed(() => [{ id: 0, name: '无（顶级分类）', children: tree.value }])

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

function createForm() {
  return { id: null, parentId: 0, name: '', icon: '', sort: 0, status: 1 }
}

async function loadList() {
  loading.value = true
  try {
    const data = await getRobotCategories()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openCreate(parentId) {
  Object.assign(form, createForm(), { parentId: parentId || 0 })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, createForm(), {
    id: row.id,
    parentId: row.parentId || 0,
    name: row.name || '',
    icon: row.icon || '',
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
    const payload = {
      ...form,
      parentId: form.parentId || 0
    }
    await saveRobotCategory(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteRobotCategory(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.icon-thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  border: 1px solid var(--rh-border);
}
</style>
