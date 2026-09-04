<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="page-title">菜单管理</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate(0)">新增菜单</el-button>
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
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.menuType === 1 ? '' : 'warning'">
              {{ row.menuType === 1 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由" min-width="140" show-overflow-tooltip />
        <el-table-column prop="component" label="组件" min-width="160" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="permission" label="权限标识" width="140" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.menuType === 1" link type="primary" @click="openCreate(row.id)">新增</el-button>
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
      :title="form.id ? '编辑菜单' : '新增菜单'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'menuName', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="无（顶级）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 1" label="路由路径">
          <el-input v-model="form.path" placeholder="/xxx" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 1" label="组件路径">
          <el-input v-model="form.component" placeholder="views/xxx/index" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 1" label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permission" placeholder="如：system:admin:list" />
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
import { getMenus, getMenuTree, saveMenu, deleteMenu } from '@/api/system'
import { buildTree } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const tree = ref([])
const parentOptions = computed(() => [{ id: 0, menuName: '无（顶级）', children: tree.value }])

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  menuName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function createForm() {
  return {
    id: null,
    parentId: 0,
    menuName: '',
    menuType: 1,
    path: '',
    component: '',
    icon: '',
    permission: '',
    sort: 0,
    status: 1
  }
}

async function loadList() {
  loading.value = true
  try {
    const [flat, treeData] = await Promise.all([getMenus(), getMenuTree()])
    list.value = Array.isArray(flat) ? flat : []
    tree.value = Array.isArray(treeData) && treeData.length ? treeData : buildTree(list.value)
  } catch (e) {
    list.value = []
    tree.value = []
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
    menuName: row.menuName || '',
    menuType: row.menuType || 1,
    path: row.path || '',
    component: row.component || '',
    icon: row.icon || '',
    permission: row.permission || '',
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
    await saveMenu({ ...form, parentId: form.parentId || 0 })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>
