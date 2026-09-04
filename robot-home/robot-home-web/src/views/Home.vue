<template>
  <MainLayout>
    <div class="rh-container">
      <!-- Banner -->
      <section v-if="data.banners && data.banners.length" class="home-banner">
        <el-carousel height="360px" indicator-position="outside" arrow="hover">
          <el-carousel-item v-for="banner in data.banners" :key="banner.id">
            <a :href="banner.url || 'javascript:void(0)'" class="home-banner__item">
              <img :src="imageOf(banner.image)" :alt="banner.title" />
              <div v-if="banner.title" class="home-banner__title">{{ banner.title }}</div>
            </a>
          </el-carousel-item>
        </el-carousel>
      </section>

      <!-- 快捷导航 -->
      <section class="rh-section quick-nav">
        <router-link
          v-for="cat in data.quickNav"
          :key="cat.id"
          :to="{ name: 'robots', query: { categoryId: cat.id } }"
          class="quick-nav__item"
        >
          <span class="quick-nav__name">{{ cat.name }}</span>
          <span class="quick-nav__children">
            <template v-for="(child, idx) in (cat.children || []).slice(0, 3)" :key="child.id">
              <span v-if="idx > 0">·</span>{{ child.name }}
            </template>
          </span>
        </router-link>
      </section>

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

      <!-- 排行榜 -->
      <section v-if="data.rankings && data.rankings.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">排行榜</h2>
          <router-link to="/rankings" class="rh-section__more">查看全部 ›</router-link>
        </div>
        <div class="rh-grid rh-grid--4">
          <div v-for="block in data.rankings" :key="block.code" class="rank-block">
            <div class="rank-block__head">
              <span class="rank-block__title">{{ block.name }}</span>
              <router-link :to="{ name: 'rankings', query: { type: block.code } }" class="rh-text-light">
                更多 ›
              </router-link>
            </div>
            <router-link
              v-for="(robot, index) in block.robots.slice(0, 10)"
              :key="robot.id"
              :to="'/robot/' + robot.id"
              class="rank-block__row"
            >
              <span class="rank-block__no" :class="{ 'rank-block__no--top': index < 3 }">
                {{ index + 1 }}
              </span>
              <span class="rank-block__name rh-ellipsis">{{ robot.name }}</span>
              <span class="rank-block__price">{{ formatPrice(robot.guidePrice) }}</span>
            </router-link>
            <div v-if="!block.robots.length" class="rh-empty">暂无数据</div>
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
            <img :src="imageOf(brand.logo)" :alt="brand.name" />
            <span class="rh-ellipsis">{{ brand.name }}</span>
          </router-link>
        </div>
      </section>

      <div class="rh-grid rh-grid--2 home-split">
        <!-- 资讯 -->
        <section v-if="data.articles && data.articles.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">行业资讯</h2>
            <router-link to="/articles" class="rh-section__more">查看全部 ›</router-link>
          </div>
          <div class="article-list">
            <router-link
              v-for="article in data.articles.slice(0, 6)"
              :key="article.id"
              :to="'/article/' + article.id"
              class="article-list__item"
            >
              <img :src="imageOf(article.cover)" :alt="article.title" />
              <div class="article-list__body">
                <div class="article-list__title rh-clamp-2">{{ article.title }}</div>
                <div class="article-list__meta rh-text-light">
                  {{ formatDate(article.publishTime) }} · {{ formatCount(article.viewCount) }} 阅读
                </div>
              </div>
            </router-link>
          </div>
        </section>

        <!-- 视频 -->
        <section v-if="data.videos && data.videos.length" class="rh-section">
          <div class="rh-section__head">
            <h2 class="rh-section__title">视频</h2>
            <router-link to="/videos" class="rh-section__more">查看全部 ›</router-link>
          </div>
          <div class="rh-grid rh-grid--2">
            <router-link
              v-for="video in data.videos.slice(0, 4)"
              :key="video.id"
              :to="'/video/' + video.id"
              class="video-mini"
            >
              <img :src="imageOf(video.cover)" :alt="video.title" />
              <span class="video-mini__title rh-clamp-2">{{ video.title }}</span>
            </router-link>
          </div>
        </section>
      </div>

      <!-- 热门讨论 -->
      <section v-if="data.hotPosts && data.hotPosts.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">热门讨论</h2>
          <router-link to="/community" class="rh-section__more">进入社区 ›</router-link>
        </div>
        <div class="rh-card post-list">
          <router-link
            v-for="post in data.hotPosts"
            :key="post.id"
            :to="'/community/' + post.id"
            class="post-list__item"
          >
            <span class="post-list__circle">{{ post.circleName || '讨论' }}</span>
            <span class="post-list__title rh-ellipsis">
              {{ post.title || post.content }}
            </span>
            <span class="rh-text-light">{{ post.likeCount || 0 }} 赞</span>
          </router-link>
        </div>
      </section>

      <!-- 企业推荐 -->
      <section v-if="data.companies && data.companies.length" class="rh-section">
        <div class="rh-section__head">
          <h2 class="rh-section__title">推荐企业</h2>
          <router-link to="/companies" class="rh-section__more">查看全部 ›</router-link>
        </div>
        <div class="company-grid">
          <router-link
            v-for="company in data.companies"
            :key="company.id"
            :to="'/company/' + company.id"
            class="company-grid__item"
          >
            <img :src="imageOf(company.logo)" :alt="company.name" />
            <div class="company-grid__name rh-ellipsis">{{ company.name }}</div>
            <div class="company-grid__meta rh-text-light">
              {{ company.region }} · {{ company.productCount || 0 }} 款产品
            </div>
          </router-link>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import RobotCard from '@/components/RobotCard.vue'
import { homeApi } from '@/api'
import { setPageMeta, DEFAULT_META } from '@/utils/seo'
import { formatPrice, formatCount, formatDate, imageOf } from '@/utils/format'

const data = reactive({
  banners: [],
  quickNav: [],
  hotRobots: [],
  newRobots: [],
  hotBrands: [],
  articles: [],
  videos: [],
  hotPosts: [],
  companies: [],
  rankings: []
})

onMounted(async () => {
  setPageMeta(DEFAULT_META)
  try {
    const res = await homeApi.index('pc')
    Object.keys(data).forEach((key) => {
      data[key] = res[key] || []
    })
  } catch (e) {
    // 错误已由请求拦截器提示
  }
})
</script>

<style scoped lang="scss">
.home-banner {
  margin-bottom: 24px;
  border-radius: var(--rh-radius-lg);
  overflow: hidden;
  background: #fff;
}

.home-banner__item {
  position: relative;
  display: block;
  height: 360px;
}

.home-banner__item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-banner__title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 40px 32px 20px;
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.6), transparent);
}

.quick-nav {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 18px;
}

.quick-nav__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: var(--rh-radius);
  background: var(--rh-surface-sub);
  transition: background 0.18s ease;
}

.quick-nav__item:hover {
  background: var(--rh-primary-light);
}

.quick-nav__name {
  font-size: 16px;
  font-weight: 600;
}

.quick-nav__children {
  font-size: 12px;
  color: var(--rh-text-light);
}

.rank-block {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 16px;
}

.rank-block__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--rh-border-light);
}

.rank-block__title {
  font-size: 15px;
  font-weight: 600;
}

.rank-block__row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
}

.rank-block__row:hover .rank-block__name {
  color: var(--rh-primary);
}

.rank-block__no {
  width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 3px;
  background: var(--rh-surface-sub);
  color: var(--rh-text-light);
  font-size: 12px;
  flex-shrink: 0;
}

.rank-block__no--top {
  background: var(--rh-primary);
  color: #fff;
}

.rank-block__name {
  flex: 1;
  min-width: 0;
}

.rank-block__price {
  color: var(--rh-danger);
  flex-shrink: 0;
}

.brand-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.brand-grid__item {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 16px 10px;
  text-align: center;
  font-size: 13px;
}

.brand-grid__item:hover {
  color: var(--rh-primary);
}

.brand-grid__item img {
  width: 48px;
  height: 48px;
  object-fit: contain;
  margin: 0 auto 8px;
}

.home-split {
  align-items: start;
}

.article-list__item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--rh-border-light);
}

.article-list__item:last-child {
  border-bottom: none;
}

.article-list__item img {
  width: 120px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.article-list__body {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.article-list__title {
  font-size: 14px;
  line-height: 1.5;
}

.article-list__item:hover .article-list__title {
  color: var(--rh-primary);
}

.article-list__meta {
  font-size: 12px;
}

.video-mini {
  background: #fff;
  border-radius: var(--rh-radius);
  overflow: hidden;
  display: block;
}

.video-mini img {
  width: 100%;
  height: 120px;
  object-fit: cover;
}

.video-mini__title {
  display: block;
  padding: 8px 10px 12px;
  font-size: 13px;
  height: 46px;
}

.video-mini:hover .video-mini__title {
  color: var(--rh-primary);
}

.post-list__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--rh-border-light);
  font-size: 14px;
}

.post-list__item:last-child {
  border-bottom: none;
}

.post-list__circle {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--rh-primary);
  background: var(--rh-primary-light);
  padding: 2px 8px;
  border-radius: 4px;
}

.post-list__title {
  flex: 1;
  min-width: 0;
}

.post-list__item:hover .post-list__title {
  color: var(--rh-primary);
}

.company-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.company-grid__item {
  background: #fff;
  border-radius: var(--rh-radius);
  padding: 16px;
  text-align: center;
}

.company-grid__item img {
  width: 56px;
  height: 56px;
  object-fit: contain;
  margin: 0 auto 10px;
  border-radius: 8px;
}

.company-grid__name {
  font-size: 14px;
  font-weight: 600;
}

.company-grid__meta {
  font-size: 12px;
  margin-top: 2px;
}

.company-grid__item:hover .company-grid__name {
  color: var(--rh-primary);
}
</style>
