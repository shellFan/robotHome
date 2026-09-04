<template>
  <el-breadcrumb separator="/" class="breadcrumb">
    <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="(item, index) in items" :key="index">
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { defaultMenus } from '@/router'

const route = useRoute()
const userStore = useUserStore()

/** 收集 path -> title 映射 */
const titleMap = computed(() => {
  const map = {}
  const walk = (list, parentPath = '') => {
    ;(list || []).forEach((item) => {
      if (!item || !item.path) return
      const raw = String(item.path)
      const full = raw.startsWith('/') ? raw : `${parentPath.replace(/\/$/, '')}/${raw}`
      map[full] = item.menuName || item.title || item.name || raw
      walk(item.children, full)
    })
  }
  walk(defaultMenus)
  if (userStore.menus && userStore.menus.length) walk(userStore.menus)
  return map
})

const items = computed(() => {
  const path = route.path || ''
  const segments = path.split('/').filter(Boolean)
  if (!segments.length) return []
  const result = []
  let current = ''
  segments.forEach((seg) => {
    current += `/${seg}`
    const title =
      (route.meta && route.meta.title && current === path ? route.meta.title : null) ||
      titleMap.value[current] ||
      seg
    result.push({ path: current, title })
  })
  return result
})
</script>

<style scoped>
.breadcrumb {
  font-size: 13px;
}
</style>
