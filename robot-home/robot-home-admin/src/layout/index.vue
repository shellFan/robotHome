<template>
  <div class="layout">
    <aside class="layout__aside" :style="{ width: collapsed ? '64px' : '210px' }">
      <sidebar :collapse="collapsed" />
    </aside>
    <div class="layout__main">
      <header class="layout__header">
        <el-button
          class="layout__collapse"
          :icon="collapsed ? 'Expand' : 'Fold'"
          text
          @click="collapsed = !collapsed"
        />
        <navbar class="layout__navbar" />
      </header>
      <main class="layout__content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'

const collapsed = ref(false)
</script>

<style scoped>
.layout {
  display: flex;
  height: 100%;
  background: var(--rh-bg);
}

.layout__aside {
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  transition: width 0.2s ease;
}

.layout__main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100%;
}

.layout__header {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  background: #ffffff;
  border-bottom: 1px solid var(--rh-border);
}

.layout__collapse {
  margin-left: 6px;
}

.layout__navbar {
  flex: 1;
  min-width: 0;
  border-bottom: none;
}

.layout__content {
  flex: 1;
  overflow: auto;
  padding: 0;
}
</style>
