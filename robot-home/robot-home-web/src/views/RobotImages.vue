<template>
  <MainLayout>
    <div class="rh-container">
      <RobotSubNav :id="id" :robot-name="robotName" active="images" current-label="图片" />

      <div class="rh-card">
        <h2 class="rh-section__title">{{ robotName }} · 图片（{{ images.length }}）</h2>
        <div v-if="!images.length" class="rh-empty">暂无图片</div>
        <div v-else class="image-grid">
          <div v-for="(img, idx) in images" :key="img.id || idx" class="image-grid__item">
            <img :src="imageOf(img.url)" :alt="img.type || '图片'" @click="preview(idx)" loading="lazy" />
            <span v-if="img.type" class="image-grid__type">{{ typeText(img.type) }}</span>
          </div>
        </div>
      </div>

      <el-image-viewer
        v-if="viewer.visible"
        :url-list="viewer.urls"
        :initial-index="viewer.index"
        @close="viewer.visible = false"
      />
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import RobotSubNav from '@/components/RobotSubNav.vue'
import { robotApi } from '@/api'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const id = computed(() => Number(route.params.id))
const images = ref([])
const robotName = ref('机器人')

const viewer = reactive({ visible: false, urls: [], index: 0 })

const TYPE_MAP = { normal: '实拍', official: '官方', scene: '场景', detail: '细节' }
const typeText = (type) => TYPE_MAP[type] || type

function preview (idx) {
  viewer.urls = images.value.map((i) => imageOf(i.url))
  viewer.index = idx
  viewer.visible = true
}

onMounted(async () => {
  images.value = await robotApi.images(id.value)
  try {
    const detail = await robotApi.detail(id.value)
    robotName.value = detail.robot.name
  } catch (e) {
    // 忽略
  }
  setPageMeta({ title: robotName.value + ' 图片 - 机器人之家' })
})
</script>

<style scoped lang="scss">
.image-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.image-grid__item {
  position: relative;
  border-radius: var(--rh-radius);
  overflow: hidden;
  background: var(--rh-surface-sub);
}

.image-grid__item img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  cursor: zoom-in;
}

.image-grid__type {
  position: absolute;
  left: 8px;
  top: 8px;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
