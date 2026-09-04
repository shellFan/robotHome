<template>
  <div class="robot-subnav">
    <el-breadcrumb separator="/" class="page-crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ name: 'robots' }">机器人库</el-breadcrumb-item>
      <el-breadcrumb-item :to="'/robot/' + id">{{ robotName }}</el-breadcrumb-item>
      <el-breadcrumb-item>{{ currentLabel }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-tabs :model-value="active" class="robot-subnav__tabs" @tab-click="onTab">
      <el-tab-pane label="首页" name="home" />
      <el-tab-pane label="参数配置" name="params" />
      <el-tab-pane label="图片" name="images" />
      <el-tab-pane label="视频" name="videos" />
      <el-tab-pane label="口碑" name="reviews" />
    </el-tabs>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  id: { type: [Number, String], required: true },
  robotName: { type: String, default: '机器人' },
  active: { type: String, required: true },
  currentLabel: { type: String, required: true }
})

const router = useRouter()

function onTab (tab) {
  const name = tab.props.name
  if (name === 'home') {
    router.push('/robot/' + props.id)
    return
  }
  router.push('/robot/' + props.id + '/' + name)
}
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.robot-subnav__tabs {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 0 18px;
  margin-bottom: 16px;
}
</style>
