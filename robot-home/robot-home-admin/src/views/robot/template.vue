<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="page-title">参数模板</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增模板</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe highlight-current-row @current-change="onSelect">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column label="所属分类" width="160">
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click.stop="loadDetail(row)">管理参数</el-button>
            <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>
    </div>

    <div v-if="currentId" class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">参数分组 / 定义 — {{ currentName }}</span>
        <el-button type="primary" :icon="'Plus'" @click="openGroupCreate">新增分组</el-button>
      </div>

      <el-collapse v-model="activeGroups">
        <el-collapse-item v-for="g in groups" :key="g.id" :name="String(g.id)">
          <template #title>
            <div class="group-title">
              <span>{{ g.name }}（排序 {{ g.sort ?? 0 }}）</span>
              <span>
                <el-button link type="primary" @click.stop="openGroupEdit(g)">编辑</el-button>
                <el-button link type="primary" @click.stop="openDefCreate(g)">加参数</el-button>
                <el-button link type="danger" @click.stop="handleDeleteGroup(g)">删除</el-button>
              </span>
            </div>
          </template>
          <el-table :data="g.defs || []" border size="small">
            <el-table-column prop="name" label="参数名" min-width="140" />
            <el-table-column prop="unit" label="单位" width="90" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="options" label="选项" min-width="140" show-overflow-tooltip />
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="对比/展示" width="110">
              <template #default="{ row }">
                {{ row.isCompare === 1 ? '对比' : '-' }} / {{ row.isShow === 1 ? '展示' : '隐藏' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDefEdit(g, row)">编辑</el-button>
                <el-button link type="danger" @click="handleDeleteDef(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-if="!groups.length" description="暂无分组，请先新增" />
    </div>

    <!-- 模板表单 -->
    <el-dialog v-model="tplVisible" :title="tplForm.id ? '编辑模板' : '新增模板'" width="520px" destroy-on-close>
      <el-form ref="tplRef" :model="tplForm" :rules="tplRules" label-width="90px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="tplForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-tree-select
            v-model="tplForm.categoryId"
            :data="categoryTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="请选择分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="tplForm.sort" :min="0" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="tplForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tplVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTpl">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 分组表单 -->
    <el-dialog v-model="groupVisible" :title="groupForm.id ? '编辑分组' : '新增分组'" width="460px" destroy-on-close>
      <el-form ref="groupRef" :model="groupForm" :rules="groupRules" label-width="90px">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="如：运动性能" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="groupForm.sort" :min="0" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveGroup">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 参数定义表单 -->
    <el-dialog v-model="defVisible" :title="defForm.id ? '编辑参数' : '新增参数'" width="560px" destroy-on-close>
      <el-form ref="defRef" :model="defForm" :rules="defRules" label-width="90px">
        <el-form-item label="参数名称" prop="name">
          <el-input v-model="defForm.name" placeholder="如：最大负载" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="defForm.unit" placeholder="如：kg" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="defForm.type" style="width: 100%">
            <el-option label="文本" value="text" />
            <el-option label="数字" value="number" />
            <el-option label="选项" value="select" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="defForm.type === 'select'" label="选项">
          <el-input v-model="defForm.options" placeholder="逗号分隔，如：支持,不支持" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="defForm.sort" :min="0" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="用于对比">
          <el-switch v-model="defForm.isCompare" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="前台展示">
          <el-switch v-model="defForm.isShow" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="defVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDef">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRobotTemplates,
  getRobotTemplateDetail,
  saveRobotTemplate,
  deleteRobotTemplate,
  saveParamGroup,
  deleteParamGroup,
  saveParamDef,
  deleteParamDef,
  getRobotCategories
} from '@/api/robot'
import { buildTree } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const categories = ref([])
const categoryTree = computed(() => buildTree(categories.value))

const currentId = ref(null)
const currentName = ref('')
const groups = ref([])
const activeGroups = ref([])

const tplVisible = ref(false)
const tplRef = ref(null)
const tplForm = reactive({ id: null, name: '', categoryId: null, sort: 0, status: 1 })
const tplRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const groupVisible = ref(false)
const groupRef = ref(null)
const groupForm = reactive({ id: null, templateId: null, name: '', sort: 0 })
const groupRules = { name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }] }

const defVisible = ref(false)
const defRef = ref(null)
const defForm = reactive({
  id: null,
  groupId: null,
  name: '',
  unit: '',
  type: 'text',
  options: '',
  sort: 0,
  isCompare: 0,
  isShow: 1
})
const defRules = {
  name: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function categoryName(id) {
  const hit = categories.value.find((c) => String(c.id) === String(id))
  return hit ? hit.name : '-'
}

async function loadList() {
  loading.value = true
  try {
    const data = await getRobotTemplates()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function onSelect(row) {
  if (row) loadDetail(row)
}

async function loadDetail(row) {
  currentId.value = row.id
  currentName.value = row.name
  const data = await getRobotTemplateDetail(row.id)
  const gs = (data && (data.groups || data.paramGroups)) || []
  groups.value = gs.map((g) => ({
    ...g,
    defs: g.defs || g.paramDefs || g.params || []
  }))
  activeGroups.value = groups.value.map((g) => String(g.id))
}

function openCreate() {
  Object.assign(tplForm, { id: null, name: '', categoryId: null, sort: 0, status: 1 })
  tplVisible.value = true
}

function openEdit(row) {
  Object.assign(tplForm, {
    id: row.id,
    name: row.name || '',
    categoryId: row.categoryId,
    sort: row.sort ?? 0,
    status: row.status === undefined ? 1 : row.status
  })
  tplVisible.value = true
}

async function saveTpl() {
  const valid = await tplRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveRobotTemplate({ ...tplForm })
    ElMessage.success('保存成功')
    tplVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除模板「${row.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteRobotTemplate(row.id)
  ElMessage.success('删除成功')
  if (currentId.value === row.id) {
    currentId.value = null
    groups.value = []
  }
  loadList()
}

function openGroupCreate() {
  Object.assign(groupForm, { id: null, templateId: currentId.value, name: '', sort: 0 })
  groupVisible.value = true
}

function openGroupEdit(g) {
  Object.assign(groupForm, {
    id: g.id,
    templateId: currentId.value,
    name: g.name || '',
    sort: g.sort ?? 0
  })
  groupVisible.value = true
}

async function saveGroup() {
  const valid = await groupRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveParamGroup({ ...groupForm })
    ElMessage.success('保存成功')
    groupVisible.value = false
    loadDetail({ id: currentId.value, name: currentName.value })
  } finally {
    saving.value = false
  }
}

async function handleDeleteGroup(g) {
  try {
    await ElMessageBox.confirm(`确认删除分组「${g.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteParamGroup(g.id)
  ElMessage.success('删除成功')
  loadDetail({ id: currentId.value, name: currentName.value })
}

function openDefCreate(g) {
  Object.assign(defForm, {
    id: null,
    groupId: g.id,
    name: '',
    unit: '',
    type: 'text',
    options: '',
    sort: 0,
    isCompare: 0,
    isShow: 1
  })
  defVisible.value = true
}

function openDefEdit(g, row) {
  Object.assign(defForm, {
    id: row.id,
    groupId: g.id,
    name: row.name || '',
    unit: row.unit || '',
    type: row.type || 'text',
    options: row.options || '',
    sort: row.sort ?? 0,
    isCompare: row.isCompare ?? 0,
    isShow: row.isShow === undefined ? 1 : row.isShow
  })
  defVisible.value = true
}

async function saveDef() {
  const valid = await defRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveParamDef({ ...defForm })
    ElMessage.success('保存成功')
    defVisible.value = false
    loadDetail({ id: currentId.value, name: currentName.value })
  } finally {
    saving.value = false
  }
}

async function handleDeleteDef(row) {
  try {
    await ElMessageBox.confirm(`确认删除参数「${row.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteParamDef(row.id)
  ElMessage.success('删除成功')
  loadDetail({ id: currentId.value, name: currentName.value })
}

onMounted(async () => {
  const cats = await getRobotCategories()
  categories.value = Array.isArray(cats) ? cats : []
  await loadList()
})
</script>

<style scoped>
.group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 12px;
}
</style>
