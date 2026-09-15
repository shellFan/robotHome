<template>
  <div class="app-container">
    <h3 style="margin-bottom: 16px;">零结果搜索管理</h3>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="normalizedKeyword" label="关键词" min-width="200" />
      <el-table-column prop="searchCount" label="搜索次数" width="100" sortable />
      <el-table-column prop="lastSearchTime" label="最后搜索时间" width="160" />
      <el-table-column prop="suggestedAction" label="建议操作" width="150">
        <template #default="{ row }">
          <el-tag v-if="row.suggestedAction" type="warning">{{ row.suggestedAction }}</el-tag>
          <span v-else class="text-gray">待处理</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-select v-model="row.suggestedAction" placeholder="设置操作" size="small" style="width: 140px" @change="handleUpdateAction(row)">
            <el-option label="添加别名" value="ADD_ALIAS" />
            <el-option label="添加机器人" value="ADD_ROBOT" />
            <el-option label="添加品牌" value="ADD_BRAND" />
            <el-option label="忽略" value="IGNORE" />
          </el-select>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="(p) => { pageNum = p; loadList() }" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getZeroResultPage, updateZeroResultAction } from '@/api/searchManage'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function loadList() {
  loading.value = true
  try {
    const res = await getZeroResultPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.data) { list.value = res.data.list || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}

async function handleUpdateAction(row) {
  await updateZeroResultAction(row.id, row.suggestedAction)
  ElMessage.success('更新成功')
}

onMounted(loadList)
</script>