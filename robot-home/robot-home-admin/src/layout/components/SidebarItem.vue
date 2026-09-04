<template>
  <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.fullPath">
    <template #title>
      <el-icon v-if="menu.resolvedIcon"><component :is="menu.resolvedIcon" /></el-icon>
      <span>{{ menu.title }}</span>
    </template>
    <sidebar-item v-for="child in menu.children" :key="child.fullPath" :menu="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="menu.fullPath">
    <el-icon v-if="menu.resolvedIcon"><component :is="menu.resolvedIcon" /></el-icon>
    <template #title>{{ menu.title }}</template>
  </el-menu-item>
</template>

<script setup>
defineProps({
  menu: { type: Object, required: true }
})
</script>
