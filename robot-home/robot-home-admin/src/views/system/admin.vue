<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 昵称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">管理员</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增管理员</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="账号" min-width="180">
          <template #default="{ row }">
            <div class="admin-cell">
              <el-avatar :size="32" :src="row.avatar">{{ (row.nickname || row.username || 'A').charAt(0) }}</el-avatar>
              <div>
                <div>{{ row.nickname || '-' }}</div>
                <div class="text-weak" style="font-size: 12px">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openResetPwd(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page.pageNum"
          v-model:page-size="page.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑管理员' : '新增管理员'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="头像">
          <image-upload v-model="form.avatar" module="admin" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple filterable placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPwd">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminPage,
  getAdminDetail,
  saveAdmin,
  deleteAdmin,
  updateAdminStatus,
  resetAdminPassword,
  getRoles
} from '@/api/system'
import ImageUpload from '@/components/ImageUpload.vue'
import { cleanParams, formatTime } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const roles = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', status: '' })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const pwdVisible = ref(false)
const pwdAdminId = ref(null)
const newPassword = ref('')

function createForm() {
  return {
    id: null,
    username: '',
    password: '',
    nickname: '',
    avatar: '',
    phone: '',
    email: '',
    status: 1,
    roleIds: []
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await getAdminPage({
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      ...cleanParams(query)
    })
    list.value = (data && data.list) || []
    page.total = (data && data.total) || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.status = ''
  handleSearch()
}

function openCreate() {
  Object.assign(form, createForm())
  dialogVisible.value = true
}

async function openEdit(row) {
  Object.assign(form, createForm())
  dialogVisible.value = true
  const data = await getAdminDetail(row.id)
  if (data) {
    Object.keys(form).forEach((key) => {
      if (key === 'password') return
      if (data[key] !== undefined && data[key] !== null) form[key] = data[key]
    })
    form.roleIds = data.roleIds || []
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (payload.id) delete payload.password
    await saveAdmin(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await updateAdminStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已禁用')
  loadList()
}

function openResetPwd(row) {
  pwdAdminId.value = row.id
  newPassword.value = ''
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  saving.value = true
  try {
    await resetAdminPassword(pwdAdminId.value, newPassword.value)
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除管理员「${row.username}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteAdmin(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  const data = await getRoles()
  roles.value = Array.isArray(data) ? data : []
  await loadList()
})
</script>

<style scoped>
.admin-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
