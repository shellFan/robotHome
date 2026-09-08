<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-bar">
        <el-input v-model="query.sourceName" placeholder="数据源名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        <el-select v-model="query.sourceType" placeholder="类型" clearable style="width: 140px">
          <el-option label="网站" value="WEBSITE" />
          <el-option label="RSS" value="RSS" />
          <el-option label="Sitemap" value="SITEMAP" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="手动" value="MANUAL" />
        </el-select>
        <el-select v-model="query.crawlEnabled" placeholder="采集状态" clearable style="width: 130px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">数据源列表</span>
        <div>
          <el-button @click="handleClearDedup" type="warning" plain>清除去重</el-button>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增数据源</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="sourceName" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sourceType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" label="基础URL" min-width="200" show-overflow-tooltip />
        <el-table-column label="采集策略" width="100">
          <template #default="{ row }">{{ row.crawlStrategy || '-' }}</template>
        </el-table-column>
        <el-table-column label="深度/页数" width="100">
          <template #default="{ row }">{{ row.maxDepth || '-' }} / {{ row.maxPages || '-' }}</template>
        </el-table-column>
        <el-table-column label="健康度" width="90">
          <template #default="{ row }">
            <el-tag :type="healthType(row.healthStatus)" size="small">{{ row.healthStatus || 'UNKNOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="采集" width="80">
          <template #default="{ row }">
            <el-tag :type="row.crawlEnabled === 1 ? 'success' : 'info'" size="small">
              {{ row.crawlEnabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文章/产品" width="100">
          <template #default="{ row }">{{ row.totalArticles || 0 }} / {{ row.totalProducts || 0 }}</template>
        </el-table-column>
        <el-table-column label="最后采集" width="160">
          <template #default="{ row }">{{ row.lastCrawlTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.crawlEnabled === 1 ? 'warning' : 'success'" @click="handleToggle(row)">
              {{ row.crawlEnabled === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据源" />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑数据源' : '新增数据源'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="sourceName">
              <el-input v-model="form.sourceName" placeholder="数据源名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="sourceType">
              <el-select v-model="form.sourceType" placeholder="类型" style="width: 100%">
                <el-option label="网站" value="WEBSITE" />
                <el-option label="RSS" value="RSS" />
                <el-option label="Sitemap" value="SITEMAP" />
                <el-option label="微信" value="WECHAT" />
                <el-option label="手动" value="MANUAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="基础URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="采集策略">
              <el-select v-model="form.crawlStrategy" placeholder="策略" clearable style="width: 100%">
                <el-option label="通用" value="GENERIC" />
                <el-option label="Sitemap" value="SITEMAP" />
                <el-option label="RSS" value="RSS" />
                <el-option label="新闻" value="NEWS" />
                <el-option label="产品" value="PRODUCT" />
                <el-option label="微信" value="WECHAT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="抓取模式">
              <el-select v-model="form.fetchMode" style="width: 100%">
                <el-option label="HTTP" value="http" />
                <el-option label="浏览器" value="browser" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="最大深度">
              <el-input-number v-model="form.maxDepth" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大页数">
              <el-input-number v-model="form.maxPages" :min="1" :max="100000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="请求间隔(ms)">
              <el-input-number v-model="form.requestDelay" :min="0" :max="60000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="种子URL">
          <el-input v-model="form.seedUrls" type="textarea" :rows="3" placeholder="每行一个种子URL" />
        </el-form-item>
        <el-form-item label="RSS/Sitemap">
          <el-input v-model="form.rssUrl" placeholder="RSS Feed URL" style="margin-bottom: 8px" />
          <el-input v-model="form.sitemapUrl" placeholder="Sitemap URL" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="包含规则">
              <el-input v-model="form.includeRules" placeholder="正则,逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排除规则">
              <el-input v-model="form.excludeRules" placeholder="正则,逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题选择器">
              <el-input v-model="form.titleSelector" placeholder="CSS选择器" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="正文选择器">
              <el-input v-model="form.contentSelector" placeholder="CSS选择器" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="遵守Robots">
              <el-switch v-model="form.respectRobots" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="自动发布">
              <el-switch v-model="form.autoPublish" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用采集">
              <el-switch v-model="form.crawlEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
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
  getCrawlerSourcePage,
  getCrawlerSource,
  createCrawlerSource,
  updateCrawlerSource,
  deleteCrawlerSource,
  toggleCrawlerSource,
  clearCrawlerDedup
} from '@/api/crawler'
import { cleanParams } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ sourceName: '', sourceType: '', crawlEnabled: '' })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive(createForm())
const rules = {
  sourceName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入基础URL', trigger: 'blur' }]
}

function createForm() {
  return {
    id: null, sourceName: '', sourceType: 'WEBSITE', baseUrl: '', brandId: null, companyId: null,
    crawlEnabled: 1, crawlStrategy: 'GENERIC', crawlInterval: null, maxDepth: 3, maxPages: 500,
    includeRules: '', excludeRules: '', titleSelector: '', contentSelector: '', dateSelector: '',
    authorSelector: '', coverSelector: '', listSelector: '', detailLinkSelector: '',
    autoPublish: 0, respectRobots: 1, requestDelay: 1000, userAgent: '', seedUrls: '',
    sitemapUrl: '', rssUrl: '', fetchMode: 'http', status: 1
  }
}

function typeLabel(t) {
  const map = { WEBSITE: '网站', RSS: 'RSS', SITEMAP: 'Sitemap', WECHAT: '微信', MANUAL: '手动' }
  return map[t] || t
}

function healthType(h) {
  const map = { HEALTHY: 'success', DEGRADED: 'warning', FAILED: 'danger', DISABLED: 'info', UNKNOWN: 'info' }
  return map[h] || 'info'
}

async function loadList() {
  loading.value = true
  try {
    const data = await getCrawlerSourcePage({
      page: page.pageNum, size: page.pageSize, ...cleanParams(query)
    })
    list.value = (data && data.records) || []
    page.total = (data && data.total) || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.pageNum = 1; loadList() }
function handleReset() { query.sourceName = ''; query.sourceType = ''; query.crawlEnabled = ''; handleSearch() }

function openCreate() {
  Object.assign(form, createForm())
  dialogVisible.value = true
}

async function openEdit(row) {
  Object.assign(form, createForm())
  dialogVisible.value = true
  const data = await getCrawlerSource(row.id)
  if (data) {
    Object.keys(form).forEach((key) => { if (data[key] !== undefined && data[key] !== null) form[key] = data[key] })
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateCrawlerSource(form.id, { ...form })
    } else {
      await createCrawlerSource({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleToggle(row) {
  await toggleCrawlerSource(row.id)
  ElMessage.success(row.crawlEnabled === 1 ? '已停用' : '已启用')
  loadList()
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm(`确认删除数据源「${row.sourceName}」？`, '提示', { type: 'warning' }) } catch { return }
  await deleteCrawlerSource(row.id)
  ElMessage.success('删除成功')
  loadList()
}

async function handleClearDedup() {
  try { await ElMessageBox.confirm('确认清除所有去重数据？重新抓取将处理已访问过的URL。', '提示', { type: 'warning' }) } catch { return }
  await clearCrawlerDedup()
  ElMessage.success('去重数据已清除')
}

onMounted(() => loadList())
</script>