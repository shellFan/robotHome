<template>
  <div class="layout">
    <header class="header">
      <div class="header__top">
        <div class="rh-container header__top-inner">
          <router-link to="/" class="logo">
            <span class="logo__mark">R</span>
            <span class="logo__text">机器人之家</span>
          </router-link>

          <div class="search-box" @keydown="onSuggestKeydown">
            <el-input
              v-model="keyword"
              placeholder="搜索机器人、品牌、企业、资讯…"
              clearable
              size="large"
              @keyup.enter="doSearch"
              @keydown.esc="suggests = []"
            >
              <template #append>
                <el-button :icon="Search" @click="doSearch" />
              </template>
            </el-input>
            <div v-if="suggests.length" class="search-box__suggest">
              <div
                v-for="(item, idx) in suggests"
                :key="item"
                class="search-box__suggest-item"
                :class="{ 'is-active': suggestIdx === idx }"
                @click="goSuggest(item)"
              >
                <el-icon class="search-box__suggest-icon"><Search /></el-icon>
                <span>{{ item }}</span>
              </div>
            </div>
          </div>

          <div class="header__user">
            <template v-if="userStore.isLogin">
              <el-badge :value="unread || 0" :hidden="!unread" class="header__msg">
                <router-link to="/user/messages">
                  <el-icon :size="20"><Bell /></el-icon>
                </router-link>
              </el-badge>
              <el-dropdown @command="onCommand">
                <span class="header__nickname">
                  <el-avatar :size="28" :src="userStore.profile && userStore.profile.avatar" />
                  <span>{{ userStore.nickname || '我的' }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                    <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                    <el-dropdown-item command="history">浏览历史</el-dropdown-item>
                    <el-dropdown-item command="inquiries">我的询价</el-dropdown-item>
                    <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <router-link to="/login">
                <el-button type="primary" size="default">登录 / 注册</el-button>
              </router-link>
            </template>
          </div>
        </div>
      </div>

      <nav class="nav">
        <div class="rh-container nav__inner">
          <router-link
            v-for="item in navList"
            :key="item.path"
            :to="item.path"
            class="nav__item"
            :class="{ 'nav__item--active': isActive(item) }"
          >
            {{ item.name }}
          </router-link>
        </div>
      </nav>
    </header>

    <main class="main">
      <slot />
    </main>

    <footer class="footer">
      <div class="rh-container footer__inner">
        <div class="footer__col">
          <div class="footer__title">机器人之家</div>
          <p class="footer__desc">
            机器人行业的垂直平台，提供机器人产品数据库、品牌库、企业库、资讯、视频、参数对比、排行榜、社区与询价采购服务。
          </p>
        </div>
        <div class="footer__col">
          <div class="footer__title">产品库</div>
          <router-link to="/robots">机器人库</router-link>
          <router-link to="/brands">品牌库</router-link>
          <router-link to="/companies">企业库</router-link>
          <router-link to="/compare">参数对比</router-link>
        </div>
        <div class="footer__col">
          <div class="footer__title">内容</div>
          <router-link to="/articles">行业资讯</router-link>
          <router-link to="/videos">视频</router-link>
          <router-link to="/tutorials">教程</router-link>
          <router-link to="/community">社区</router-link>
        </div>
        <div class="footer__col">
          <div class="footer__title">关于</div>
          <router-link to="/feedback">意见反馈</router-link>
          <span class="footer__text">示例 ICP 备 00000000 号</span>
          <span class="footer__text">数据与参数为示例内容，仅供参考</span>
        </div>
      </div>
      <div class="footer__copyright">© 2026 机器人之家 Robot Home</div>
    </footer>

    <!-- 对比悬浮条 -->
    <div v-if="compareStore.count > 0" class="compare-bar">
      <div class="rh-container compare-bar__inner">
        <span class="compare-bar__label">对比栏（{{ compareStore.count }}/4）</span>
        <div class="compare-bar__items">
          <div v-for="id in compareStore.ids" :key="id" class="compare-bar__item">
            <img v-if="compareRobotMap[id]" :src="imageOf(compareRobotMap[id].coverImage)" :alt="compareRobotMap[id].name" class="compare-bar__img" />
            <span class="compare-bar__name">{{ (compareRobotMap[id] && compareRobotMap[id].name) || ('#' + id) }}</span>
            <el-icon class="compare-bar__close" @click="compareStore.remove(id)"><Close /></el-icon>
          </div>
        </div>
        <div class="compare-bar__actions">
          <el-button size="small" text @click="compareStore.clear()">清空</el-button>
          <el-button size="small" type="primary" @click="goCompare">开始对比</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Bell, ArrowDown, Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useCompareStore } from '@/store/compare'
import { searchApi, messageApi, robotApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const compareStore = useCompareStore()

const keyword = ref(route.query.keyword || '')
const suggests = ref([])
const suggestIdx = ref(-1)
const unread = ref(0)
const compareRobotMap = ref({})

const navList = [
  { path: '/', name: '首页' },
  { path: '/robots', name: '选机器人' },
  { path: '/rankings', name: '排行榜' },
  { path: '/brands', name: '品牌' },
  { path: '/articles', name: '资讯' },
  { path: '/videos', name: '视频' },
  { path: '/community', name: '社区' },
  { path: '/tutorials', name: '教程' },
  { path: '/companies', name: '企业库' }
]

function isActive (item) {
  if (item.path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(item.path)
}

async function doSearch () {
  const kw = keyword.value && keyword.value.trim()
  if (!kw) return
  suggests.value = []
  suggestIdx.value = -1
  router.push({ name: 'search', query: { keyword: kw } })
}

function onSuggestKeydown (e) {
  if (!suggests.value.length) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    suggestIdx.value = (suggestIdx.value + 1) % suggests.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    suggestIdx.value = suggestIdx.value <= 0 ? suggests.value.length - 1 : suggestIdx.value - 1
  } else if (e.key === 'Enter' && suggestIdx.value >= 0) {
    e.preventDefault()
    goSuggest(suggests.value[suggestIdx.value])
  } else if (e.key === 'Escape') {
    suggests.value = []
    suggestIdx.value = -1
  }
}

function goSuggest (item) {
  keyword.value = item
  doSearch()
}

function goCompare () {
  router.push({ name: 'compare', query: { ids: compareStore.ids.join(',') } })
}

function imageOf (url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/api/files/${url}`
}

async function loadCompareRobots () {
  const ids = compareStore.ids
  if (!ids.length) {
    compareRobotMap.value = {}
    return
  }
  const missing = ids.filter(id => !compareRobotMap.value[id])
  if (!missing.length) return
  try {
    const results = await Promise.all(missing.map(id => robotApi.detail(id).catch(() => null)))
    results.forEach((r, i) => {
      if (r) compareRobotMap.value[missing[i]] = r
    })
  } catch (e) { /* ignore */ }
}

watch(() => compareStore.ids, loadCompareRobots, { immediate: true })

async function onCommand (cmd) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/')
    return
  }
  router.push(`/user/${cmd}`)
}

let timer = null
watch(keyword, (val) => {
  clearTimeout(timer)
  const kw = val && val.trim()
  if (!kw) {
    suggests.value = []
    suggestIdx.value = -1
    return
  }
  timer = setTimeout(async () => {
    try {
      suggests.value = await searchApi.suggest(kw, 8)
      suggestIdx.value = -1
    } catch (e) {
      suggests.value = []
      suggestIdx.value = -1
    }
  }, 250)
})

watch(
  () => route.query.keyword,
  (val) => {
    if (typeof val === 'string' && route.name === 'search') {
      keyword.value = val
    }
  }
)

async function loadUnread () {
  if (!userStore.isLogin) return
  try {
    const data = await messageApi.unreadCount()
    unread.value = (data && data.count) || 0
  } catch (e) {
    unread.value = 0
  }
}

onMounted(() => {
  loadUnread()
})
</script>

<style scoped lang="scss">
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid var(--rh-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header__top-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 72px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.logo__mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--rh-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.logo__text {
  font-size: 20px;
  font-weight: 600;
  color: var(--rh-text);
}

.search-box {
  flex: 1;
  max-width: 520px;
  position: relative;
}

.search-box__suggest {
  position: absolute;
  top: 44px;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid var(--rh-border);
  border-radius: var(--rh-radius);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  z-index: 200;
  overflow: hidden;
}

.search-box__suggest-item {
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
}

.search-box__suggest-item:hover,
.search-box__suggest-item.is-active {
  background: var(--rh-primary-light);
  color: var(--rh-primary);
}

.search-box__suggest-icon {
  margin-right: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.header__user {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 18px;
}

.header__msg {
  display: flex;
  align-items: center;
}

.header__nickname {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}

.nav {
  border-top: 1px solid var(--rh-border-light);
}

.nav__inner {
  display: flex;
  gap: 4px;
  height: 44px;
  align-items: center;
}

.nav__item {
  padding: 0 16px;
  height: 44px;
  line-height: 44px;
  font-size: 15px;
  color: var(--rh-text-sub);
  position: relative;
}

.nav__item:hover {
  color: var(--rh-primary);
}

.nav__item--active {
  color: var(--rh-primary);
  font-weight: 600;
}

.nav__item--active::after {
  content: '';
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 0;
  height: 2px;
  background: var(--rh-primary);
}

.main {
  flex: 1;
  padding: 20px 0 40px;
}

.footer {
  background: #1f2329;
  color: #a6adb5;
  padding: 40px 0 0;
}

.footer__inner {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 32px;
  padding-bottom: 32px;
}

.footer__col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
}

.footer__col a:hover {
  color: #fff;
}

.footer__title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.footer__desc,
.footer__text {
  margin: 0;
  line-height: 1.8;
}

.footer__copyright {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
  padding: 16px 0;
  font-size: 12px;
}

.compare-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid var(--rh-border);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  z-index: 90;
  padding: 10px 0;
}

.compare-bar__inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.compare-bar__label {
  font-weight: 600;
  flex-shrink: 0;
}

.compare-bar__items {
  flex: 1;
  display: flex;
  gap: 12px;
  align-items: center;
  overflow-x: auto;
}
.compare-bar__item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--el-bg-color);
  border-radius: 6px;
  padding: 4px 8px;
  white-space: nowrap;
}
.compare-bar__img {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  object-fit: cover;
  background: var(--el-fill-color-lighter);
}
.compare-bar__name {
  font-size: 13px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.compare-bar__close {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  &:hover { color: var(--el-color-danger); }
}

.compare-bar__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
