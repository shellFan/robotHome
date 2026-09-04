<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="page-title">角色管理</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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
      :title="form.id ? '编辑角色' : '新增角色'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="如：admin" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            show-checkbox
            node-key="id"
            :props="{ label: 'menuName', children: 'children' }"
            default-expand-all
            style="width: 100%; max-height: 240px; overflow: auto; border: 1px solid var(--rh-border); padding: 8px; border-radius: 4px"
          />
        </el-form-item>
        <el-form-item label="接口权限">
          <el-checkbox-group v-model="form.permissionIds">
            <el-checkbox v-for="p in permissions" :key="p.id" :value="p.id">
              {{ p.permissionName }}（{{ p.permissionCode }}）
            </el-checkbox>
          </el-checkbox-group>
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
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoles,
  getRoleDetail,
  saveRole,
  deleteRole,
  getMenuTree,
  getPermissions
} from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const menuTree = ref([])
const permissions = ref([])

const dialogVisible = ref(false)
const formRef = ref(null)
const menuTreeRef = ref(null)
const form = reactive({
  id: null,
  roleName: '',
  roleCode: '',
  description: '',
  status: 1,
  menuIds: [],
  permissionIds: []
})
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const data = await getRoles()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    roleName: '',
    roleCode: '',
    description: '',
    status: 1,
    menuIds: [],
    permissionIds: []
  })
  dialogVisible.value = true
  nextTick(() => {
    menuTreeRef.value && menuTreeRef.value.setCheckedKeys([])
  })
}

async function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    roleName: row.roleName || '',
    roleCode: row.roleCode || '',
    description: row.description || '',
    status: row.status === undefined ? 1 : row.status,
    menuIds: [],
    permissionIds: []
  })
  dialogVisible.value = true
  const data = await getRoleDetail(row.id)
  if (data) {
    form.menuIds = data.menuIds || []
    form.permissionIds = data.permissionIds || []
  }
  await nextTick()
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys(form.menuIds || [])
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const checked = menuTreeRef.value ? menuTreeRef.value.getCheckedKeys() : []
  const half = menuTreeRef.value ? menuTreeRef.value.getHalfCheckedKeys() : []
  saving.value = true
  try {
    await saveRole({
      ...form,
      menuIds: [...checked, ...half],
      permissionIds: form.permissionIds || []
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  const [menus, perms] = await Promise.all([getMenuTree(), getPermissions()])
  menuTree.value = Array.isArray(menus) ? menus : []
  permissions.value = Array.isArray(perms) ? perms : []
  await loadList()
})
</script>
