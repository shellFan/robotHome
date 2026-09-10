<template>
  <MainLayout>
    <div class="rh-container">
      <RobotSubNav :id="id" :robot-name="robotName" active="videos" current-label="视频" />

      <div class="rh-card">
        <h2 class="rh-section__title">{{ robotName }} · 视频（{{ videos.length }}）</h2>
        <div v-if="!videos.length" class="rh-empty">暂无视频</div>
        <div v-else class="video-grid">
          <div v-for="video in videos" :key="video.id" class="video-grid__item" @click="play(video)">
            <img :src="imageOf(video.cover)" :alt="video.title" loading="lazy" />
            <div class="video-grid__mask">
              <el-icon :size="36"><VideoPlay /></el-icon>
            </div>
            <div class="video-grid__title rh-ellipsis">{{ video.title }}</div>
          </div>
        </div>
      </div>

      <el-dialog v-model="player.visible" :title="player.title" width="800px" destroy-on-close>
        <video
          v-if="player.url"
          :src="player.url"
          controls
          autoplay
          style="width: 100%; max-height: 460px; background: #000"
        />
        <div v-else class="rh-empty">视频地址无效</div>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { VideoPlay } from '@element-plus/icons-vue'
import MainLayout from '@/layout/MainLayout.vue'
import RobotSubNav from '@/components/RobotSubNav.vue'
import { robotApi } from '@/api'
import { imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const id = computed(() => Number(route.params.id))
const videos = ref([])
const robotName = ref('机器人')
const player = reactive({ visible: false, url: '', title: '' })

function play (video) {
  player.url = video.url
  player.title = video.title
  player.visible = true
}

onMounted(async () => {
  videos.value = await robotApi.videos(id.value)
  try {
    const detail = await robotApi.detail(id.value)
    robotName.value = detail.robot.name
  } catch (e) {
    // 忽略
  }
  setPageMeta({ title: robotName.value + ' 视频 - 机器人之家' })
})
</script>

<style scoped lang="scss">
.video-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.video-grid__item {
  position: relative;
  border-radius: var(--rh-radius);
  overflow: hidden;
  cursor: pointer;
  background: #000;
}

.video-grid__item img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  opacity: 0.92;
}

.video-grid__mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.video-grid__title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16px 12px 8px;
  color: #fff;
  font-size: 13px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
}
</style>
