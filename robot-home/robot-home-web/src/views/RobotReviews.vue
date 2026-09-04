<template>
  <MainLayout>
    <div class="rh-container">
      <RobotSubNav :id="id" :robot-name="robotName" active="reviews" current-label="口碑" />

      <div class="rh-grid rh-grid--2 detail-body">
        <div class="rh-card">
          <CommentPanel biz-type="robot" :biz-id="id" />
        </div>
        <div>
          <div class="rh-card rh-section">
            <h2 class="rh-section__title">综合评分</h2>
            <div class="score-box">
              <div class="score-box__value">{{ scoreText }}</div>
              <el-rate :model-value="score" disabled show-score text-color="#ff9900" />
            </div>
            <div class="score-box__hint">
              评分由平台结合参数完整度与用户评价生成，仅供参考
            </div>
          </div>

          <div class="rh-card rh-section">
            <h2 class="rh-section__title">大家都在关注的参数</h2>
            <div v-if="paramPreview.length" class="param-preview">
              <div v-for="p in paramPreview" :key="p.label" class="param-preview__item">
                <span class="param-preview__label">{{ p.label }}</span>
                <span class="param-preview__value">{{ p.value }}</span>
              </div>
            </div>
            <div v-else class="rh-empty">暂无参数</div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import RobotSubNav from '@/components/RobotSubNav.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { robotApi } from '@/api'
import { parseMainParams } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const route = useRoute()
const id = computed(() => Number(route.params.id))
const robotName = ref('机器人')
const score = ref(0)
const paramPreview = ref([])

const scoreText = computed(() => (score.value > 0 ? score.value.toFixed(1) : '暂无'))

onMounted(async () => {
  const detail = await robotApi.detail(id.value)
  robotName.value = detail.robot.name
  score.value = Number(detail.robot.score || 0) / 2
  paramPreview.value = parseMainParams(detail.robot.mainParams, 6)
  setPageMeta({ title: robotName.value + ' 口碑评价 - 机器人之家' })
})
</script>

<style scoped lang="scss">
.detail-body {
  align-items: start;
}

.score-box {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-box__value {
  font-size: 34px;
  font-weight: 700;
  color: var(--rh-warning);
}

.score-box__hint {
  font-size: 12px;
  color: var(--rh-text-light);
  margin-top: 8px;
}

.param-preview {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.param-preview__item {
  background: var(--rh-surface-sub);
  border-radius: var(--rh-radius);
  padding: 10px;
  text-align: center;
}

.param-preview__label {
  display: block;
  font-size: 12px;
  color: var(--rh-text-light);
}

.param-preview__value {
  font-weight: 600;
}
</style>
