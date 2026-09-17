<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-select v-model="query.rankType" placeholder="排名类型" clearable style="width: 160px">
          <el-option label="综合排名" value="overall" />
          <el-option label="工业机器人" value="industrial" />
          <el-option label="服务机器人" value="service" />
          <el-option label="特种机器人" value="special" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">排名快照</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="rankType" label="排名类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.rankType || 'overall' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="snapshotName" label="快照名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="robotCount" label="机器人数" width="100" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewSnapshot(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无快照" />
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

    <!-- 快照详情弹窗 -->
    <el-dialog v-model="detailVisible" title="快照详情" width="800px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="排名类型">{{ detail.rankType || 'overall' }}</el-descriptions-item>
        <el-descriptions-item label="快照名称" :span="2">{{ detail.snapshotName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="机器人数">{{ detail.robotCount }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail && detail.rankData && detail.rankData.length" style="margin-top: 16px">
        <el-table :data="detail.rankData.slice(0, 20)" border size="small" max-height="400">
          <el-table-column type="index" label="排名" width="70" />
          <el-table-column prop="robotName" label="机器人" min-width="160" />
          <el-table-column prop="score" label="得分" width="100" />
        </el-table>
        <div v-if="detail.rankData.length > 20" class="text-weak" style="margin-top: 8px; font-size: 12px">
          仅显示前 20 条，共 {{ detail.rankData.length }} 条
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getRankingSnapshots } from '@/api/ranking'
import { cleanParams, formatTime } from '@/utils'

const loading = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ rankType: '' })

const detailVisible = ref(false)
const detail = ref(null)

async function loadList() {
  loading.value = true
  try {
    const data = await getRankingSnapshots({
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
  query.rankType = ''
  handleSearch()
}

function viewSnapshot(row) {
  detail.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadList()
})
</script>