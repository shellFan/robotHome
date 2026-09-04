<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="企业名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.region"
          placeholder="地区"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">企业列表</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增企业</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="企业" min-width="220">
          <template #default="{ row }">
            <div class="company-cell">
              <el-image v-if="row.logo" :src="row.logo" fit="contain" class="company-cell__logo" />
              <div v-else class="company-cell__empty">无</div>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="地区" width="120" />
        <el-table-column prop="foundYear" label="成立年份" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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
          :page-sizes="[10, 20, 50, 100]"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑企业' : '新增企业'"
      width="700px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="Logo">
          <image-upload v-model="form.logo" module="company" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="地区">
              <el-input v-model="form.region" placeholder="如：广东深圳" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成立年份">
              <el-input-number v-model="form.foundYear" :min="1800" :max="2100" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="官网">
              <el-input v-model="form.website" placeholder="https://" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="500" show-word-limit />
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
import { getCompanyPage, getCompanyDetail, saveCompany, deleteCompany } from '@/api/company'
import ImageUpload from '@/components/ImageUpload.vue'
import { cleanParams } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', region: '', status: '' })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }]
}

function createForm() {
  return {
    id: null,
    name: '',
    logo: '',
    intro: '',
    foundYear: null,
    region: '',
    website: '',
    contactPhone: '',
    contactEmail: '',
    address: '',
    tags: '',
    sort: 0,
    status: 1
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await getCompanyPage({
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
  query.region = ''
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
  const data = await getCompanyDetail(row.id)
  if (data) {
    Object.keys(form).forEach((key) => {
      if (data[key] !== undefined && data[key] !== null) form[key] = data[key]
    })
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveCompany({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除企业「${row.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteCompany(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.company-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.company-cell__logo {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid var(--rh-border);
  flex-shrink: 0;
}
.company-cell__empty {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  border: 1px dashed var(--rh-border);
  color: var(--rh-text-weak);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
