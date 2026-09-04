<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="标题 / 作者"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 160px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">教程列表</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增教程</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <div class="title-cell">
              <el-image v-if="row.cover" :src="row.cover" fit="cover" class="title-cell__cover" />
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.publishTime || row.createTime) }}</template>
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
      :title="form.id ? '编辑教程' : '新增教程'"
      width="900px"
      top="4vh"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" clearable placeholder="请选择分类" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="form.author" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">上架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面">
          <image-upload v-model="form.cover" module="tutorial" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="300" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <rich-text-editor v-model="form.content" module="tutorial" :height="380" />
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
import {
  getTutorialPage,
  getTutorialDetail,
  saveTutorial,
  deleteTutorial,
  getTutorialCategories
} from '@/api/tutorial'
import ImageUpload from '@/components/ImageUpload.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { cleanParams, formatTime } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const categories = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', categoryId: null, status: '' })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function createForm() {
  return {
    id: null,
    categoryId: null,
    title: '',
    cover: '',
    summary: '',
    content: '',
    author: '',
    tags: '',
    status: 1
  }
}

function categoryName(id) {
  const hit = categories.value.find((c) => String(c.id) === String(id))
  return hit ? hit.name : '-'
}

async function loadList() {
  loading.value = true
  try {
    const data = await getTutorialPage({
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
  query.categoryId = null
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
  const data = await getTutorialDetail(row.id)
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
    await saveTutorial({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除教程「${row.title}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteTutorial(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  const cats = await getTutorialCategories()
  categories.value = Array.isArray(cats) ? cats : []
  await loadList()
})
</script>

<style scoped>
.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-cell__cover {
  width: 56px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid var(--rh-border);
  flex-shrink: 0;
}
</style>
