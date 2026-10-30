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
          v-if="player.kind === 'media' && !player.error"
          :src="player.url"
          :poster="player.cover"
          controls
          autoplay
          style="width: 100%; max-height: 460px; background: #000"
          @error="onPlayerError"
        />
        <div v-else class="video-dialog-fallback">
          <img v-if="player.cover" :src="player.cover" :alt="player.title" />
          <div class="video-dialog-fallback__content">
            <p v-if="player.kind === 'none'">视频地址无效</p>
            <p v-else-if="player.error">直接播放失败，可打开视频链接</p>
            <p v-else>该视频需前往来源平台观看</p>
            <a
              v-if="player.kind === 'external' || player.error"
              :href="player.url"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ player.error ? '打开视频链接' : '在原平台观看' }}
            </a>
          </div>
        </div>
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
import { resolveVideoSource } from '@/utils/video-url'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const id = computed(() => Number(route.params.id))
const videos = ref([])
const robotName = ref('机器人')
const player = reactive({ visible: false, kind: 'none', url: '', title: '', cover: '', error: false })

function play (video) {
  const source = resolveVideoSource(video)
  Object.assign(player, {
    visible: true,
    kind: source.kind,
    url: source.url,
    title: video.title || '',
    cover: imageOf(video.cover),
    error: false
  })
}

function onPlayerError () {
  player.error = true
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

.video-dialog-fallback {
  position: relative;
  min-height: 260px;
  overflow: hidden;
  border-radius: var(--rh-radius);
  color: #fff;
  background: #111;
}

.video-dialog-fallback img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  max-height: 460px;
  object-fit: contain;
  opacity: 0.48;
}

.video-dialog-fallback__content {
  position: relative;
  z-index: 1;
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  text-align: center;
}

.video-dialog-fallback__content p {
  margin: 0;
}

.video-dialog-fallback__content a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 18px;
  border-radius: 20px;
  color: #fff;
  background: var(--rh-primary);
}
</style>
