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
        <el-tree-select
          v-model="query.categoryId"
          :data="categoryTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          check-strictly
          clearable
          placeholder="所属分类"
          style="width: 200px"
        />
        <el-select v-model="query.brandId" placeholder="所属品牌" clearable filterable style="width: 180px">
          <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="table-toolbar">
        <span class="page-title">型号列表</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增型号</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe size="default">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="产品" min-width="240">
          <template #default="{ row }">
            <div class="robot-cell">
              <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="robot-cell__cover" />
              <div class="robot-cell__empty" v-else>无图</div>
              <div class="robot-cell__info">
                <span class="robot-cell__name">{{ row.name }}</span>
                <span class="text-weak">{{ row.subtitle || row.model || '' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="型号" width="150" />
        <el-table-column label="分类" width="130">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="品牌" width="140">
          <template #default="{ row }">{{ brandName(row.brandId) }}</template>
        </el-table-column>
        <el-table-column label="指导价" width="120">
          <template #default="{ row }">{{ formatMoney(row.guidePrice) }}</template>
        </el-table-column>
        <el-table-column prop="releaseDate" label="发布时间" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
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

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? `编辑型号 - ${form.name || ''}` : '新增型号'"
      width="980px"
      top="5vh"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="base">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="产品名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入产品名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="型号" prop="model">
                  <el-input v-model="form.model" placeholder="如：RH-1000" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属分类" prop="categoryId">
                  <el-tree-select
                    v-model="form.categoryId"
                    :data="categoryTree"
                    :props="{ label: 'name', children: 'children' }"
                    node-key="id"
                    check-strictly
                    clearable
                    placeholder="请选择分类"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属品牌" prop="brandId">
                  <el-select
                    v-model="form.brandId"
                    filterable
                    clearable
                    placeholder="请选择品牌"
                    style="width: 100%"
                    @change="onBrandChange"
                  >
                    <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属系列">
                  <el-select v-model="form.seriesId" clearable placeholder="请选择系列" style="width: 100%">
                    <el-option v-for="s in seriesList" :key="s.id" :label="s.name" :value="s.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="发布时间">
                  <el-date-picker
                    v-model="form.releaseDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="选择日期"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="指导价">
                  <el-input-number
                    v-model="form.guidePrice"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="市场价">
                  <el-input-number
                    v-model="form.marketPrice"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    style="width: 100%"
                  />
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
              <el-col :span="12">
                <el-form-item label="评分">
                  <el-input-number
                    v-model="form.score"
                    :min="0"
                    :max="10"
                    :precision="1"
                    :controls="false"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="封面图">
                  <image-upload v-model="form.coverImage" module="robot" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="简介">
                  <el-input
                    v-model="form.subtitle"
                    type="textarea"
                    :rows="2"
                    maxlength="200"
                    show-word-limit
                    placeholder="一句话简介"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="核心参数">
                  <el-input
                    v-model="form.mainParams"
                    type="textarea"
                    :rows="2"
                    placeholder="如：负载 20kg｜续航 8h｜自由度 32"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="参数" name="param" :disabled="!form.id">
          <robot-params-panel
            v-if="activeTab === 'param' && form.id"
            ref="paramRef"
            :robot-id="form.id"
            :default-template-id="defaultTemplateId"
          />
          <el-empty v-else-if="activeTab === 'param'" description="请先保存基本信息" />
        </el-tab-pane>

        <el-tab-pane label="图片" name="image" :disabled="!form.id">
          <robot-images-panel v-if="activeTab === 'image' && form.id" ref="imageRef" :robot-id="form.id" />
          <el-empty v-else-if="activeTab === 'image'" description="请先保存基本信息" />
        </el-tab-pane>

        <el-tab-pane label="视频" name="video" :disabled="!form.id">
          <robot-videos-panel v-if="activeTab === 'video' && form.id" ref="videoRef" :robot-id="form.id" />
          <el-empty v-else-if="activeTab === 'video'" description="请先保存基本信息" />
        </el-tab-pane>

        <el-tab-pane label="价格" name="price" :disabled="!form.id">
          <robot-prices-panel v-if="activeTab === 'price' && form.id" ref="priceRef" :robot-id="form.id" />
          <el-empty v-else-if="activeTab === 'price'" description="请先保存基本信息" />
        </el-tab-pane>

        <el-tab-pane label="标签" name="tag" :disabled="!form.id">
          <robot-tags-panel v-if="activeTab === 'tag' && form.id" ref="tagRef" :robot-id="form.id" />
          <el-empty v-else-if="activeTab === 'tag'" description="请先保存基本信息" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">关 闭</el-button>
        <el-button type="primary" :loading="saving" @click="handleDialogSave">
          {{ activeTab === 'base' ? '保 存' : '保存当前页签' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRobotPage,
  getRobotDetail,
  saveRobot,
  deleteRobot,
  updateRobotStatus,
  getRobotCategories,
  getRobotSeries,
  getRobotTemplates
} from '@/api/robot'
import { getBrandPage } from '@/api/brand'
import ImageUpload from '@/components/ImageUpload.vue'
import RobotParamsPanel from './components/RobotParamsPanel.vue'
import RobotImagesPanel from './components/RobotImagesPanel.vue'
import RobotVideosPanel from './components/RobotVideosPanel.vue'
import RobotPricesPanel from './components/RobotPricesPanel.vue'
import RobotTagsPanel from './components/RobotTagsPanel.vue'
import { buildTree, cleanParams } from '@/utils'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const query = reactive({ keyword: '', categoryId: null, brandId: null, status: '' })

const categories = ref([])
const brands = ref([])
const seriesList = ref([])
const templates = ref([])

const categoryTree = computed(() => buildTree(categories.value))
const defaultTemplateId = computed(() => {
  if (!form.categoryId) return null
  const t = templates.value.find((item) => String(item.categoryId) === String(form.categoryId))
  return t ? t.id : null
})

const dialogVisible = ref(false)
const activeTab = ref('base')
const formRef = ref(null)
const paramRef = ref(null)
const imageRef = ref(null)
const videoRef = ref(null)
const priceRef = ref(null)
const tagRef = ref(null)

const form = reactive(createForm())
const rules = {
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }]
}

function createForm() {
  return {
    id: null,
    name: '',
    model: '',
    subtitle: '',
    categoryId: null,
    seriesId: null,
    brandId: null,
    guidePrice: 0,
    marketPrice: 0,
    status: 1,
    releaseDate: '',
    coverImage: '',
    mainParams: '',
    score: 0
  }
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return `¥${num.toLocaleString('zh-CN')}`
}

function categoryName(id) {
  const hit = categories.value.find((c) => String(c.id) === String(id))
  return hit ? hit.name : '-'
}

function brandName(id) {
  const hit = brands.value.find((b) => String(b.id) === String(id))
  return hit ? hit.name : '-'
}

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

async function loadOptions() {
  const [cats, brandData, tpls] = await Promise.all([
    getRobotCategories(),
    getBrandPage({ pageNum: 1, pageSize: 500 }),
    getRobotTemplates()
  ])
  categories.value = Array.isArray(cats) ? cats : []
  brands.value = (brandData && brandData.list) || []
  templates.value = Array.isArray(tpls) ? tpls : []
}

async function loadSeries(brandId) {
  if (!brandId) {
    seriesList.value = []
    return
  }
  const data = await getRobotSeries(brandId)
  seriesList.value = Array.isArray(data) ? data : []
}

function onBrandChange(brandId) {
  form.seriesId = null
  loadSeries(brandId)
}

function handleSearch() {
  page.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.categoryId = null
  query.brandId = null
  query.status = ''
  handleSearch()
}

function openCreate() {
  Object.assign(form, createForm())
  seriesList.value = []
  activeTab.value = 'base'
  dialogVisible.value = true
}

async function openEdit(row) {
  Object.assign(form, createForm())
  activeTab.value = 'base'
  dialogVisible.value = true
  const data = await getRobotDetail(row.id)
  if (data) {
    Object.keys(form).forEach((key) => {
      if (data[key] !== undefined && data[key] !== null) {
        form[key] = data[key]
      }
    })
    form.status = data.status === undefined ? 1 : data.status
  }
  await loadSeries(form.brandId)
}

function onDialogClosed() {
  Object.assign(form, createForm())
  activeTab.value = 'base'
}

async function handleDialogSave() {
  if (activeTab.value !== 'base') {
    const refMap = { param: paramRef, image: imageRef, video: videoRef, price: priceRef, tag: tagRef }
    const target = refMap[activeTab.value]
    if (target && target.value && typeof target.value.save === 'function') {
      await target.value.save()
    }
    return
  }
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = Object.assign({}, form)
    Object.keys(payload).forEach((key) => {
      if (payload[key] === '') payload[key] = null
    })
    const id = await saveRobot(payload)
    if (!form.id && id) {
      form.id = id
      ElMessage.success('新增成功，可继续维护参数 / 图片 / 视频 / 价格 / 标签')
    } else {
      ElMessage.success('保存成功')
    }
    await loadList()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await updateRobotStatus(row.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已下架')
  loadList()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除型号「${row.name}」？该操作不可撤销。`, '提示', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteRobot(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  await loadOptions()
  await loadList()
})
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
