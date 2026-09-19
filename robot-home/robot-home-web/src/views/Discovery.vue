<template>
  <MainLayout>
    <div class="rh-container">
      <div v-if="data" class="discovery-page">
        <!-- 热门机器人 -->
        <section v-if="data.hotRobots && data.hotRobots.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">热门机器人</h2>
            <router-link to="/robots" class="rh-section__more">查看全部 ›</router-link>
          </div>
          <div class="rh-grid rh-grid--5">
            <RobotCard v-for="robot in data.hotRobots" :key="robot.id" :robot="robot" />
          </div>
        </section>

        <!-- 近期热门 -->
        <section v-if="data.trendingRobots && data.trendingRobots.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">近期热门</h2>
          </div>
          <div class="rh-grid rh-grid--5">
            <RobotCard v-for="robot in data.trendingRobots" :key="robot.id" :robot="robot" />
          </div>
        </section>

        <!-- 新品机器人 -->
        <section v-if="data.newRobots && data.newRobots.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">新品机器人</h2>
            <router-link to="/robots?sort=new" class="rh-section__more">查看全部 ›</router-link>
          </div>
          <div class="rh-grid rh-grid--5">
            <RobotCard v-for="robot in data.newRobots" :key="robot.id" :robot="robot" />
          </div>
        </section>

        <!-- 高评分 -->
        <section v-if="data.topRatedRobots && data.topRatedRobots.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">高评分机器人</h2>
          </div>
          <div class="rh-grid rh-grid--5">
            <RobotCard v-for="robot in data.topRatedRobots" :key="robot.id" :robot="robot" />
          </div>
        </section>

        <!-- 高收藏 -->
        <section v-if="data.mostFavoritedRobots && data.mostFavoritedRobots.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">高收藏机器人</h2>
          </div>
          <div class="rh-grid rh-grid--5">
            <RobotCard v-for="robot in data.mostFavoritedRobots" :key="robot.id" :robot="robot" />
          </div>
        </section>

        <!-- 高讨论 -->
        <section v-if="data.mostDiscussedRobots && data.mostDiscussedRobots.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">高讨论机器人</h2>
          </div>
          <div class="rh-grid rh-grid--5">
            <RobotCard v-for="robot in data.mostDiscussedRobots" :key="robot.id" :robot="robot" />
          </div>
        </section>

        <!-- 榜单卡片 -->
        <section v-if="data.rankingCards && data.rankingCards.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">排行榜</h2>
            <router-link to="/rankings" class="rh-section__more">查看全部 ›</router-link>
          </div>
          <div class="rh-grid rh-grid--4">
            <div v-for="card in data.rankingCards" :key="card.rankType" class="rank-card">
              <div class="rank-card__head">
                <span class="rank-card__title">{{ card.rankName }}</span>
                <router-link :to="{ name: 'rankings', query: { type: card.rankType } }" class="rh-text-light">更多 ›</router-link>
              </div>
              <router-link
                v-for="(item, idx) in (card.items || []).slice(0, 10)"
                :key="item.robotId"
                :to="'/robot/' + item.robotId"
                class="rank-card__row"
              >
                <span class="rank-card__no" :class="{ 'rank-card__no--top': idx < 3 }">{{ idx + 1 }}</span>
                <span class="rank-card__name rh-ellipsis">{{ item.robotName }}</span>
                <span class="rank-card__score">{{ item.score }}</span>
              </router-link>
              <div v-if="!card.items || !card.items.length" class="rh-empty">暂无数据</div>
            </div>
          </div>
        </section>

        <!-- 热门品牌 -->
        <section v-if="data.hotBrands && data.hotBrands.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">热门品牌</h2>
            <router-link to="/brands" class="rh-section__more">查看全部 ›</router-link>
          </div>
          <div class="brand-grid">
            <router-link
              v-for="brand in data.hotBrands"
              :key="brand.id"
              :to="'/brand/' + brand.id"
              class="brand-grid__item"
            >
              <img :src="imageOf(brand.logo)" :alt="brand.name" loading="lazy" />
              <span class="rh-ellipsis">{{ brand.name }}</span>
            </router-link>
          </div>
        </section>

        <div class="rh-grid rh-grid--2 discovery-split">
          <!-- 热门讨论 -->
          <section v-if="data.hotPosts && data.hotPosts.length" class="rh-section">
            <div class="rh-section__head">
              <h2 class="rh-section__title">热门讨论</h2>
              <router-link to="/community" class="rh-section__more">查看全部 ›</router-link>
            </div>
            <div class="post-list">
              <router-link
                v-for="post in data.hotPosts"
                :key="post.id"
                :to="'/community/' + post.id"
                class="post-list__item"
              >
                <span class="post-list__title rh-ellipsis">{{ post.title }}</span>
                <span class="post-list__meta rh-text-light">{{ formatCount(post.likeCount) }} 赞 · {{ formatCount(post.commentCount) }} 评论</span>
              </router-link>
            </div>
          </section>

          <!-- 热门问答 -->
          <section v-if="data.hotQuestions && data.hotQuestions.length" class="rh-section">
            <div class="rh-section__head">
              <h2 class="rh-section__title">热门问答</h2>
              <router-link to="/qa" class="rh-section__more">查看全部 ›</router-link>
            </div>
            <div class="post-list">
              <router-link
                v-for="q in data.hotQuestions"
                :key="q.id"
                :to="'/qa/' + q.id"
                class="post-list__item"
              >
                <span class="post-list__title rh-ellipsis">{{ q.title }}</span>
                <span class="post-list__meta rh-text-light">{{ formatCount(q.answerCount) }} 回答 · {{ formatCount(q.viewCount) }} 浏览</span>
              </router-link>
            </div>
          </section>
        </div>
      </div>
      <div v-else class="rh-container"><div class="rh-empty">加载中…</div></div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import RobotCard from '@/components/RobotCard.vue'
import { discoveryApi } from '@/api'
import { imageOf, formatCount } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const data = ref(null)

async function load () {
  data.value = await discoveryApi.home('pc')
  setPageMeta({
    title: '发现 - 机器人之家',
    description: '发现热门机器人、品牌、排行榜和社区讨论。'
  })
}

onMounted(load)
</script>

<style scoped lang="scss">
.discovery-split {
  margin-top: 20px;
}

.rank-card {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 16px;
}
.rank-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.rank-card__title {
  font-size: 15px;
  font-weight: 600;
}
.rank-card__row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  color: var(--rh-text);
  text-decoration: none;
}
.rank-card__row:hover {
  color: var(--rh-primary);
}
.rank-card__no {
  width: 20px;
  text-align: center;
  font-weight: 700;
  color: var(--rh-text-sub);
}
.rank-card__no--top {
  color: var(--rh-primary);
}
.rank-card__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank-card__score {
  font-size: 12px;
  color: var(--rh-text-sub);
}

.brand-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 16px;
}
.brand-grid__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border: 1px solid var(--rh-border);
  border-radius: var(--rh-radius);
  font-size: 13px;
  max-width: 180px;
  text-decoration: none;
  color: var(--rh-text);
}
.brand-grid__item:hover {
  border-color: var(--rh-primary);
  color: var(--rh-primary);
}
.brand-grid__item img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.post-list {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 12px 16px;
}
.post-list__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  text-decoration: none;
  color: var(--rh-text);
  border-bottom: 1px solid var(--rh-border);
}
.post-list__item:last-child {
  border-bottom: none;
}
.post-list__item:hover {
  color: var(--rh-primary);
}
.post-list__title {
  flex: 1;
  font-size: 14px;
}
.post-list__meta {
  font-size: 12px;
  white-space: nowrap;
}
</style>