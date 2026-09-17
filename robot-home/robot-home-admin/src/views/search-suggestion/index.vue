<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="关键词搜索"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.type" placeholder="类型" clearable style="width: 130px">
          <el-option
            v-for="(item, key) in SUGGESTION_TYPE_MAP"
            :key="key"
            :label="item.label"
            :value="key"
          />
        </el-select>
        <el-select v-model="query.enabled" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" @click="openAdd">新增建议</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">搜索建议列表</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="text" label="建议词" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag
              :color="(SUGGESTION_TYPE_MAP[row.type] || {}).color || '#909399'"
              size="small"
              style="color: #fff; border: none"
            >
              {{ (SUGGESTION_TYPE_MAP[row.type] || {}).label || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              size="small"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑搜索建议' : '新增搜索建议'" width="500px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="建议词">
          <el-input v-model="form.text" placeholder="搜索建议文本" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option
              v-for="(item, key) in SUGGESTION_TYPE_MAP"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSuggestionPage,
  addSuggestion,
  updateSuggestion,
  deleteSuggestion,
  toggleSuggestionEnabled,
  SUGGESTION_TYPE_MAP
} from '@/api/searchSuggestion'
import { cleanParams, formatTime } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', type: '', enabled: '' })

const dialogVisible = ref(false)
const form = ref({ id: null, text: '', type: 'keyword', sortOrder: 0, enabled: true })

async function loadList() {
  loading.value = true
  try {
    const data = await getSuggestionPage({
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
  query.type = ''
  query.enabled = ''
  handleSearch()
}

function openAdd() {
  form.value = { id: null, text: '', type: 'keyword', sortOrder: 0, enabled: true }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function saveForm() {
  if (!form.value.text.trim()) {
    ElMessage.warning('请输入建议词')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateSuggestion(form.value.id, form.value)
    } else {
      await addSuggestion(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleToggle(row) {
  try {
    await toggleSuggestionEnabled(row.id)
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    loadList()
  } catch (e) {
    // ignore
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除建议词「${row.text}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteSuggestion(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => {
  loadList()
})
</script>