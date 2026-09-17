<template>
  <div class="robot-card" @mouseenter="hovered = true" @mouseleave="hovered = false">
    <router-link :to="'/robot/' + robot.id" class="robot-card__link">
      <div class="robot-card__cover">
        <img :src="imageOf(robot.coverImage)" :alt="robot.name" loading="lazy" />
        <span v-if="statusText" class="robot-card__status">{{ statusText }}</span>
      </div>
      <div class="robot-card__body">
        <div class="robot-card__brand">{{ robot.brandName || '未标注品牌' }}</div>
        <div class="robot-card__name rh-ellipsis">{{ robot.name }}</div>
        <div class="robot-card__sub rh-ellipsis rh-text-light">{{ robot.subtitle }}</div>
        <div v-if="params.length" class="robot-card__params">
          <span v-for="p in params" :key="p.label" class="robot-card__param">
            {{ p.label }} {{ p.value }}
          </span>
        </div>
        <div class="robot-card__foot">
          <span class="rh-price">{{ formatPrice(robot.guidePrice) }}</span>
          <span class="rh-text-light">{{ formatCount(robot.viewCount) }} 浏览</span>
        </div>
      </div>
    </router-link>
    <div class="robot-card__actions" :class="{ 'robot-card__actions--show': hovered }">
      <button
        class="robot-card__action"
        :class="{ 'robot-card__action--active': isFavorited }"
        :title="isFavorited ? '取消收藏' : '收藏'"
        @click.prevent.stop="toggleFavorite"
      >
        <el-icon><Star /></el-icon>
      </button>
      <button
        class="robot-card__action"
        :class="{ 'robot-card__action--active': compareStore.has(robot.id) }"
        :title="compareStore.has(robot.id) ? '移出对比' : '加入对比'"
        @click.prevent.stop="toggleCompare"
      >
        <el-icon><DataLine /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Star, DataLine } from '@element-plus/icons-vue'
import { formatPrice, formatCount, imageOf, parseMainParams } from '@/utils/format'
import { favoriteApi, behaviorApi } from '@/api'
import { useCompareStore } from '@/store/compare'
import { useUserStore } from '@/store/user'

const props = defineProps({
  robot: { type: Object, required: true }
})

const compareStore = useCompareStore()
const userStore = useUserStore()
const hovered = ref(false)
const isFavorited = ref(false)

const STATUS_MAP = { 1: '在售', 2: '预售', 3: '停产' }
const statusText = computed(() => STATUS_MAP[props.robot.status] || '')
const params = computed(() => parseMainParams(props.robot.mainParams, 3))

async function toggleFavorite () {
  if (!userStore.isLogin) {
    ElMessage.info('请先登录后再收藏')
    return
  }
  try {
    const data = await favoriteApi.toggle('robot', props.robot.id)
    isFavorited.value = data.favorited
    ElMessage.success(data.favorited ? '已收藏' : '已取消收藏')
    behaviorApi.track('CLICK', 'robot', props.robot.id, { action: data.favorited ? 'favorite' : 'unfavorite' }).catch(() => {})
  } catch (e) {
    // 全局拦截器已处理
  }
}

function toggleCompare () {
  const result = compareStore.toggle(props.robot.id)
  if (!result.ok) {
    ElMessage.warning(result.message)
    return
  }
  const added = compareStore.has(props.robot.id)
  ElMessage.success(added ? '已加入对比' : '已移出对比')
  behaviorApi.track('CLICK', 'robot', props.robot.id, { action: added ? 'compare_add' : 'compare_remove' }).catch(() => {})
}
</script>

<style scoped lang="scss">
.robot-card {
  position: relative;
  background: var(--rh-surface);
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
  overflow: hidden;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.robot-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.robot-card__link {
  display: block;
  text-decoration: none;
  color: inherit;
}

.robot-card__cover {
  position: relative;
  height: 168px;
  background: var(--rh-surface-sub);
}

.robot-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.robot-card__status {
  position: absolute;
  left: 8px;
  top: 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
}

.robot-card__body {
  padding: 12px 14px 14px;
}

.robot-card__brand {
  font-size: 12px;
  color: var(--rh-text-light);
  margin-bottom: 2px;
}

.robot-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--rh-text);
}

.robot-card__sub {
  font-size: 12px;
  margin-top: 2px;
  height: 20px;
}

.robot-card__params {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--rh-text-sub);
  overflow: hidden;
}

.robot-card__param {
  white-space: nowrap;
}

.robot-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 13px;
}

.robot-card__actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.robot-card__actions--show {
  opacity: 1;
}

.robot-card__action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: var(--rh-text-sub);
  cursor: pointer;
  font-size: 16px;
  transition: all 0.15s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

.robot-card__action:hover {
  background: #fff;
  color: var(--rh-primary);
}

.robot-card__action--active {
  color: var(--rh-primary);
  background: #fff;
}

@media (max-width: 768px) {
  .robot-card__actions {
    opacity: 1;
  }
}
</style>