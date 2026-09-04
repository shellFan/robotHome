<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="昵称 / 手机号 / 邮箱"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <el-table v-loading="loading" :data="list" border stripe size="default">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar">{{ (row.nickname || 'U').charAt(0) }}</el-avatar>
              <div class="user-cell__info">
                <span class="user-cell__name">{{ row.nickname || '-' }}</span>
                <span class="user-cell__sub text-weak">{{ row.username || '' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="地区" width="160">
          <template #default="{ row }">
            {{ [row.province, row.city].filter(Boolean).join(' / ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="发帖 / 粉丝" width="120">
          <template #default="{ row }">{{ row.postCount ?? 0 }} / {{ row.fansCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              @click="handleToggleStatus(row, 0)"
            >
              封禁
            </el-button>
            <el-button v-else link type="success" @click="handleToggleStatus(row, 1)">解封</el-button>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, updateUserStatus } from '@/api/user'
import { cleanParams, formatTime } from '@/utils'

const loading = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', status: '' })

async function loadList() {
  loading.value = true
  try {
    const data = await getUserPage({
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

async function handleToggleStatus(row, status) {
  const action = status === 1 ? '解封' : '封禁'
  try {
    await ElMessageBox.confirm(`确认${action}用户「${row.nickname || row.username || row.id}」？`, '提示', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await updateUserStatus(row.id, status)
  ElMessage.success(`${action}成功`)
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell__info {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.user-cell__name {
  font-weight: 500;
}

.user-cell__sub {
  font-size: 12px;
}
</style>
