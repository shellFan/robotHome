<template>
  <router-link :to="'/robot/' + robot.id" class="robot-card">
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
</template>

<script setup>
import { computed } from 'vue'
import { formatPrice, formatCount, imageOf, parseMainParams } from '@/utils/format'

const props = defineProps({
  robot: { type: Object, required: true }
})

const STATUS_MAP = { 1: '在售', 2: '预售', 3: '停产' }

const statusText = computed(() => STATUS_MAP[props.robot.status] || '')
const params = computed(() => parseMainParams(props.robot.mainParams, 3))
</script>

<style scoped lang="scss">
.robot-card {
  display: block;
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
</style>
