<template>
  <MainLayout>
    <div class="rh-container">
      <RobotSubNav :id="id" :robot-name="robotName" active="params" current-label="参数配置" />

      <div class="rh-card">
        <h2 class="rh-section__title">{{ robotName }} · 参数配置</h2>
        <div v-if="!groups.length" class="rh-empty">暂无参数数据</div>
        <div v-for="group in groups" :key="group.group.id" class="param-group">
          <div class="param-group__title">{{ group.group.name }}</div>
          <el-table :data="group.defs" border stripe size="default">
            <el-table-column prop="def.name" label="参数" width="220" />
            <el-table-column label="取值">
              <template #default="{ row }">
                <span>{{ row.value || '-' }}</span>
                <span v-if="row.value && row.def.unit" class="rh-text-light"> {{ row.def.unit }}</span>
              </template>
            </el-table-column>
            <el-table-column label="说明" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.def.isCompare" size="small" type="primary">可对比</el-tag>
                <span v-else class="rh-text-light">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import RobotSubNav from '@/components/RobotSubNav.vue'
import { robotApi } from '@/api'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const id = computed(() => Number(route.params.id))
const groups = ref([])
const robotName = ref('机器人')

onMounted(async () => {
  const data = await robotApi.params(id.value)
  groups.value = data || []
  try {
    const detail = await robotApi.detail(id.value)
    robotName.value = detail.robot.name
  } catch (e) {
    // 名称获取失败不影响参数展示
  }
  setPageMeta({
    title: robotName.value + ' 参数配置 - 机器人之家',
    description: robotName.value + ' 的完整参数配置，包含基础参数、运动参数、AI 与开发能力等。'
  })
})
</script>

<style scoped lang="scss">
.param-group {
  margin-bottom: 24px;
}

.param-group__title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid var(--rh-primary);
}
</style>
