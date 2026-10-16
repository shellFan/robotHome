<template>
  <div class="sidebar">
    <div class="sidebar__logo">
      <span class="sidebar__logo-mark">RH</span>
      <span v-show="!collapse" class="sidebar__logo-text">机器人之家</span>
    </div>
    <el-scrollbar class="sidebar__scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :collapse-transition="false"
        :unique-opened="false"
        background-color="#ffffff"
        text-color="#1f2329"
        active-text-color="#1668dc"
        router
      >
        <sidebar-item v-for="item in menus" :key="item.fullPath" :menu="item" />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { defaultMenus } from '@/router'
import SidebarItem from './SidebarItem.vue'
import { resolveIcon } from './iconMap'

defineProps({
  collapse: { type: Boolean, default: false }
})

const route = useRoute()
const userStore = useUserStore()
const fallbackMenus = normalize(defaultMenus)
const companyReviewMenu = defaultMenus.find((item) => item.path === '/company-review')

/**
 * 归一化菜单：统一 fullPath / title / resolvedIcon
 * 同时兼容后端 menus（path 为绝对路径、menuName 为标题）
 * 与前端内置 defaultMenus（子级 path 为相对路径）。
 */
function normalize(list, parentPath = '') {
  return (list || [])
    .filter((item) => item && item.path)
    .map((item) => {
      const raw = String(item.path)
      const fullPath = raw.startsWith('/') ? raw : `${parentPath.replace(/\/$/, '')}/${raw}`
      const children = normalize(item.children, fullPath)
      const node = {
        id: item.id,
        fullPath,
        title: item.menuName || item.title || item.name || raw,
        resolvedIcon: resolveIcon(item.icon)
      }
      if (children.length) node.children = children
      return node
    })
}

const menus = computed(() => {
  if (userStore.menus && userStore.menus.length) {
    const normalized = normalize(userStore.menus)
    if (normalized.length) {
      appendCompanyReviewMenu(normalized)
      return normalized
    }
  }
  return filterCompanyReviewMenu(fallbackMenus)
})

function filterCompanyReviewMenu(list) {
  return list
    .map((item) => {
      if (item.fullPath !== '/company-review') return item
      const children = (item.children || []).filter((child) => {
        const source = (companyReviewMenu.children || []).find(
          (menu) => `${companyReviewMenu.path}/${menu.path}` === child.fullPath
        )
        return !source || userStore.hasPermission(source.permission)
      })
      return children.length ? { ...item, children } : null
    })
    .filter(Boolean)
}

function appendCompanyReviewMenu(list) {
  if (!companyReviewMenu) return
  const children = (companyReviewMenu.children || []).filter((item) =>
    userStore.hasPermission(item.permission)
  )
  if (!children.length) return

  const existingPaths = new Set()
  const collectPaths = (nodes) => {
    ;(nodes || []).forEach((item) => {
      existingPaths.add(item.fullPath)
      collectPaths(item.children)
    })
  }
  collectPaths(list)

  const missingChildren = children.filter(
    (item) => !existingPaths.has(`${companyReviewMenu.path}/${item.path}`)
  )
  if (!missingChildren.length) return

  const existingParent = list.find((item) => item.fullPath === companyReviewMenu.path)
  const normalizedChildren = normalize(missingChildren, companyReviewMenu.path)
  if (existingParent) {
    existingParent.children = [...(existingParent.children || []), ...normalizedChildren]
  } else {
    const reviewNode = normalize([{ ...companyReviewMenu, children: missingChildren }])[0]
    if (reviewNode) list.push(reviewNode)
  }
}

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta && meta.activeMenu) return meta.activeMenu
  return path
})
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid var(--rh-border);
}

.sidebar__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 18px;
  border-bottom: 1px solid var(--rh-border);
  flex-shrink: 0;
}

.sidebar__logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: var(--rh-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.sidebar__logo-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--rh-text);
}

.sidebar__scroll {
  flex: 1;
  overflow: hidden;
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: var(--rh-primary-light);
}
</style>
