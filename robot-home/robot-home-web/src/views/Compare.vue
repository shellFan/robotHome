<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>参数对比</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="rh-card">
        <div class="compare-head">
          <h2 class="rh-section__title">参数对比</h2>
          <div class="compare-head__actions">
            <el-button size="small" @click="clearAll">清空</el-button>
            <el-button size="small" type="primary" @click="doCompare">开始对比</el-button>
          </div>
        </div>

        <div class="compare-slots">
          <div v-for="idx in 4" :key="idx" class="compare-slot">
            <template v-if="ids[idx - 1]">
              <img :src="imageOf(robots[idx - 1] && robots[idx - 1].coverImage)" alt="" loading="lazy" />
              <div class="compare-slot__name rh-ellipsis">
                {{ (robots[idx - 1] && robots[idx - 1].name) || '加载中…' }}
              </div>
              <div class="compare-slot__price rh-price">
                {{ formatPrice(robots[idx - 1] && robots[idx - 1].guidePrice) }}
              </div>
              <el-button size="small" text type="danger" @click="removeAt(idx - 1)">移除</el-button>
            </template>
            <template v-else>
              <div class="compare-slot__empty">
                <el-icon :size="26"><Plus /></el-icon>
                <span>添加机器人</span>
                <el-select
                  size="small"
                  filterable
                  remote
                  placeholder="搜索机器人"
                  :remote-method="remoteSearch"
                  :loading="searching"
                  @change="(id) => addRobot(id)"
                >
                  <el-option
                    v-for="opt in searchOptions"
                    :key="opt.id"
                    :label="opt.name"
                    :value="opt.id"
                  />
                </el-select>
              </div>
            </template>
          </div>
        </div>

        <div class="compare-diff">
          <el-checkbox v-model="onlyDiff">只看参数差异</el-checkbox>
        </div>

        <div v-if="!result" class="rh-empty">请选择至少 2 台机器人开始对比</div>

        <div v-else class="compare-table-wrap">
          <div v-for="group in visibleGroups" :key="group.groupName" class="compare-group">
            <div class="compare-group__title">{{ group.groupName }}</div>
            <table class="compare-table">
              <thead>
                <tr>
                  <th class="compare-table__label">参数</th>
                  <th v-for="robot in result.robots" :key="robot.id">
                    <router-link :to="'/robot/' + robot.id">{{ robot.name }}</router-link>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in group.rows" :key="row.paramName">
                  <td class="compare-table__label">{{ row.paramName }}</td>
                  <td
                    v-for="(value, vi) in row.values"
                    :key="vi"
                    :class="cellClass(row, vi)"
                  >
                    <span v-if="value && value !== ''">{{ value }}</span>
                    <span v-else class="compare-table__na">-</span>
                    <span v-if="row.unit && value && value !== ''"> {{ row.unit }}</span>
                    <el-icon v-if="isBest(row, vi)" class="compare-table__best"><Trophy /></el-icon>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Trophy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { robotApi } from '@/api'
import { useCompareStore } from '@/store/compare'
import { formatPrice, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const router = useRouter()
const compareStore = useCompareStore()

const ids = ref([])
const robots = ref([])
const result = ref(null)
const onlyDiff = ref(false)
const searching = ref(false)
const searchOptions = ref([])

const visibleGroups = computed(() => {
  if (!result.value) return []
  if (!onlyDiff.value) return result.value.groups
  return result.value.groups
    .map((g) => ({ ...g, rows: g.rows.filter((r) => r.different) }))
    .filter((g) => g.rows.length)
})

/** 判断某个单元格是否为最优值 */
function isBest (row, valueIndex) {
  if (!row.comparisonType) return false
  if (row.comparisonType === 'NEUTRAL' || row.comparisonType === 'TEXT' || row.comparisonType === 'BOOLEAN') return false
  return row.bestIndex === valueIndex
}

/** 计算单元格的CSS类 */
function cellClass (row, valueIndex) {
  const classes = []
  if (row.different) classes.push('is-diff')
  if (isBest(row, valueIndex)) classes.push('is-best')
  return classes
}

async function remoteSearch (keyword) {
  if (!keyword) {
    searchOptions.value = []
    return
  }
  searching.value = true
  try {
    const data = await robotApi.page({ keyword, pageSize: 10 })
    searchOptions.value = data.list || []
  } finally {
    searching.value = false
  }
}

async function addRobot (id) {
  if (!id) return
  const res = compareStore.toggle(id)
  if (!res.ok) {
    ElMessage.warning(res.message)
    return
  }
  syncIds()
  await loadRobots()
  if (ids.value.length >= 2) {
    await doCompare()
  }
}

function removeAt (index) {
  compareStore.remove(ids.value[index])
  syncIds()
  robots.value = []
  result.value = null
  loadRobots()
}

function clearAll () {
  compareStore.clear()
  syncIds()
  robots.value = []
  result.value = null
}

function syncIds () {
  ids.value = [...compareStore.ids]
  // 用 URL 记录对比组合，便于分享
  router.replace({ name: 'compare', query: ids.value.length ? { ids: ids.value.join(',') } : {} })
}

async function loadRobots () {
  if (!ids.value.length) {
    robots.value = []
    return
  }
  // 一次请求取足量列表，按对比栏顺序匹配，避免 N 次详情请求
  const data = await robotApi.page({ pageNum: 1, pageSize: 100 })
  const all = data.list || []
  robots.value = ids.value.map((id) => all.find((r) => r.id === id) || null)
}

async function doCompare () {
  if (ids.value.length < 2) {
    ElMessage.warning('请至少选择 2 台机器人')
    return
  }
  result.value = await robotApi.compare(ids.value.join(','))
}

onMounted(async () => {
  setPageMeta({
    title: '机器人参数对比 - 多机型横向对比 - 机器人之家',
    description: '选择多台机器人进行参数横向对比，自动高亮差异项，辅助选型决策。'
  })
  const q = route.query.ids
  if (q && typeof q === 'string') {
    compareStore.clear()
    q.split(',').forEach((id) => {
      const num = Number(id)
      if (num) compareStore.toggle(num)
    })
  }
  syncIds()
  await loadRobots()
  if (ids.value.length >= 2) {
    await doCompare()
  }
})

</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.compare-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.compare-head__actions {
  display: flex;
  gap: 8px;
}

.compare-slots {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.compare-slot {
  border: 1px solid var(--rh-border);
  border-radius: var(--rh-radius);
  padding: 12px;
  text-align: center;
  min-height: 188px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: #fff;
}

.compare-slot img {
  width: 100%;
  height: 96px;
  object-fit: cover;
  border-radius: 4px;
}

.compare-slot__name {
  font-size: 14px;
  font-weight: 600;
  width: 100%;
}

.compare-slot__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--rh-text-light);
  width: 100%;
}

.compare-diff {
  margin-bottom: 10px;
}

.compare-group {
  margin-bottom: 24px;
}

.compare-group__title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid var(--rh-primary);
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.compare-table th,
.compare-table td {
  border: 1px solid var(--rh-border);
  padding: 9px 12px;
  text-align: center;
}

.compare-table th {
  background: var(--rh-surface-sub);
  font-weight: 600;
}

.compare-table__label {
  width: 160px;
  background: var(--rh-surface-sub);
  text-align: left !important;
  color: var(--rh-text-sub);
}

.compare-table .is-diff {
  background: #fff7e8;
  color: #b06c00;
  font-weight: 600;
}

.compare-table .is-best {
  background: #e8f5e9;
  color: #1b5e20;
  font-weight: 700;
}

.compare-table__na {
  color: var(--rh-text-light);
  font-style: italic;
}

.compare-table__best {
  margin-left: 4px;
  color: #f5a623;
  font-size: 12px;
  vertical-align: middle;
}

.compare-table-wrap {
  overflow-x: auto;
}

@media (max-width: 768px) {
  .compare-slots {
    grid-template-columns: repeat(2, 1fr);
  }
  .compare-table th,
  .compare-table td {
    padding: 6px 8px;
    font-size: 12px;
  }
  .compare-table__label {
    width: 100px;
  }
}
</style>
