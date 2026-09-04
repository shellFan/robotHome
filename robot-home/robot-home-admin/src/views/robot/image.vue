<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="产品名称 / 型号"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">选择型号管理图片</span>
      </div>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="产品" min-width="240">
          <template #default="{ row }">
            <div class="robot-cell">
              <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="robot-cell__cover" />
              <div v-else class="robot-cell__empty">无图</div>
              <div class="robot-cell__info">
                <span class="robot-cell__name">{{ row.name }}</span>
                <span class="text-weak">{{ row.model || '' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="型号" width="150" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openManage(row)">管理图片</el-button>
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
      :title="`图片管理 — ${currentName}`"
      width="960px"
      top="5vh"
      destroy-on-close
    >
      <robot-images-panel v-if="currentId" :robot-id="currentId" />
      <template #footer>
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getRobotPage } from '@/api/robot'
import { cleanParams } from '@/utils'
import RobotImagesPanel from './components/RobotImagesPanel.vue'

const loading = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '' })

const dialogVisible = ref(false)
const currentId = ref(null)
const currentName = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await getRobotPage({
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
  handleSearch()
}

function openManage(row) {
  currentId.value = row.id
  currentName.value = row.name
  dialogVisible.value = true
}

onMounted(loadList)
</script>

<style scoped>
.robot-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.robot-cell__cover {
  width: 52px;
  height: 38px;
  border-radius: 4px;
  border: 1px solid var(--rh-border);
  flex-shrink: 0;
}
.robot-cell__empty {
  width: 52px;
  height: 38px;
  border-radius: 4px;
  border: 1px dashed var(--rh-border);
  color: var(--rh-text-weak);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.robot-cell__info {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}
.robot-cell__name {
  font-weight: 500;
}
</style>
