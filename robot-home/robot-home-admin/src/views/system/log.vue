<template>
  <div class="page-container">
    <div class="page-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="登录日志" name="login" />
        <el-tab-pane label="操作日志" name="oper" />
        <el-tab-pane label="错误日志" name="error" />
      </el-tabs>

      <div class="filter-bar" style="margin-bottom: 12px">
        <el-input
          v-model="query.keyword"
          placeholder="关键词"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 登录日志 -->
      <el-table v-if="activeTab === 'login'" v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="location" label="地点" width="140" />
        <el-table-column prop="browser" label="浏览器" min-width="140" show-overflow-tooltip />
        <el-table-column prop="os" label="系统" width="120" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 || row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 || row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime || row.loginTime) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <!-- 操作日志 -->
      <el-table v-else-if="activeTab === 'oper'" v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="method" label="方法" min-width="160" show-overflow-tooltip />
        <el-table-column prop="requestUrl" label="URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <!-- 错误日志 -->
      <el-table v-else v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="requestUrl" label="URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="errorMsg" label="错误信息" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLoginLogs, getOperLogs, getErrorLogs } from '@/api/system'
import { cleanParams, formatTime } from '@/utils'

const activeTab = ref('login')
const loading = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '' })

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      ...cleanParams(query)
    }
    let data
    if (activeTab.value === 'login') data = await getLoginLogs(params)
    else if (activeTab.value === 'oper') data = await getOperLogs(params)
    else data = await getErrorLogs(params)
    list.value = (data && data.list) || []
    page.total = (data && data.total) || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.pageNum = 1
  loadList()
}

function handleSearch() {
  page.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  handleSearch()
}

onMounted(loadList)
</script>
