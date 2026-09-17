<template>
  <MainLayout>
    <div class="rh-container">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>社区</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="community-layout">
        <div class="community-main">
          <div class="rh-card">
            <div class="community-toolbar">
              <div class="sorts">
                <span class="chip" :class="{ 'chip--active': sort === 'latest' }" @click="setSort('latest')">最新</span>
                <span class="chip" :class="{ 'chip--active': sort === 'hot' }" @click="setSort('hot')">热门</span>
              </div>
              <div class="community-toolbar__right">
                <el-input
                  v-model="keyword"
                  placeholder="搜索帖子"
                  clearable
                  style="width: 200px"
                  @keyup.enter="reload"
                  @clear="reload"
                />
                <router-link to="/community/create">
                  <el-button type="primary">发帖</el-button>
                </router-link>
              </div>
            </div>

            <div v-if="loading" class="rh-empty">加载中…</div>
            <div v-else-if="!posts.length" class="rh-empty">暂无帖子</div>
            <div v-else class="post-list">
              <router-link
                v-for="post in posts"
                :key="post.id"
                :to="'/community/' + post.id"
                class="post-item"
              >
                <div class="post-item__head">
                  <el-avatar :size="36" :src="post.author && post.author.avatar">
                    {{ ((post.author && post.author.nickname) || '?').charAt(0) }}
                  </el-avatar>
                  <div>
                    <div class="post-item__author">{{ post.author && post.author.nickname }}</div>
                    <div class="rh-text-light">{{ fromNow(post.createTime) }} · {{ post.circleName }}</div>
                  </div>
                  <span v-if="post.isTop" class="post-item__top">置顶</span>
                </div>
                <div class="post-item__title">{{ post.title }}</div>
                <div class="post-item__content rh-clamp-2">{{ post.content }}</div>
                <div v-if="post.images && post.images.length" class="post-item__images">
                  <img
                    v-for="(img, idx) in post.images.slice(0, 3)"
                    :key="idx"
                    :src="imageOf(img)"
                    alt=""
                  />
                </div>
                <div class="post-item__meta rh-text-light">
                  <span v-if="post.topic">#{{ post.topic }}</span>
                  <span>{{ formatCount(post.likeCount) }} 赞</span>
                  <span>{{ formatCount(post.commentCount) }} 评论</span>
                  <span>{{ formatCount(post.viewCount) }} 浏览</span>
                </div>
              </router-link>
            </div>

            <el-pagination
              v-if="total > pageSize"
              background
              layout="total, prev, pager, next"
              :current-page="pageNum"
              :page-size="pageSize"
              :total="total"
              @current-change="changePage"
            />
          </div>
        </div>

        <aside class="community-side">
          <section class="rh-card rh-section">
            <h2 class="rh-section__title">圈子</h2>
            <div
              v-for="circle in circles"
              :key="circle.id"
              class="circle-item"
              :class="{ 'circle-item--active': circleId === circle.id }"
              @click="setCircle(circle.id)"
            >
              <img :src="imageOf(circle.logo)" :alt="circle.name" loading="lazy" />
              <div>
                <div class="circle-item__name">{{ circle.name }}</div>
                <div class="rh-text-light">{{ circle.postCount || 0 }} 帖</div>
              </div>
            </div>
            <div
              class="circle-item"
              :class="{ 'circle-item--active': !circleId }"
              @click="setCircle(null)"
            >
              <div class="circle-item__all">全部</div>
            </div>
          </section>

          <section v-if="topics.length" class="rh-card rh-section">
            <h2 class="rh-section__title">热门话题</h2>
            <div class="topic-tags">
              <span
                v-for="topic in topics"
                :key="topic"
                class="chip"
                :class="{ 'chip--active': activeTopic === topic }"
                @click="setTopic(topic)"
              >
                #{{ topic }}
              </span>
            </div>
          </section>
        </aside>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import MainLayout from '@/layout/MainLayout.vue'
import { communityApi } from '@/api'
import { formatCount, fromNow, imageOf } from '@/utils/format'
import { setPageMeta } from '@/utils/seo'

const circles = ref([])
const topics = ref([])
const posts = ref([])
const circleId = ref(null)
const activeTopic = ref(null)
const keyword = ref('')
const sort = ref('latest')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

async function loadSide () {
  const [c, t] = await Promise.all([
    communityApi.circles(),
    communityApi.topics(12)
  ])
  circles.value = c || []
  topics.value = t || []
}

async function load () {
  loading.value = true
  try {
    const data = await communityApi.posts({
      circleId: circleId.value || undefined,
      topic: activeTopic.value || undefined,
      keyword: keyword.value || undefined,
      sort: sort.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    posts.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload () {
  pageNum.value = 1
  load()
}

function setCircle (id) {
  circleId.value = id
  reload()
}

function setTopic (topic) {
  activeTopic.value = activeTopic.value === topic ? null : topic
  reload()
}

function setSort (s) {
  sort.value = s
  reload()
}

function changePage (p) {
  pageNum.value = p
  load()
}

onMounted(async () => {
  setPageMeta({
    title: '机器人社区 - 机器人之家',
    description: '机器人之家社区，讨论选型、评测与行业话题。'
  })
  await loadSide()
  await load()
})
</script>

<style scoped lang="scss">
.page-crumb {
  margin-bottom: 14px;
}

.community-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 16px;
  align-items: start;
}

.community-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.community-toolbar__right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.sorts,
.topic-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.chip {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: var(--rh-text-sub);
}

.chip:hover {
  color: var(--rh-primary);
}

.chip--active {
  background: var(--rh-primary);
  color: #fff;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-item {
  display: block;
  padding: 14px;
  border: 1px solid var(--rh-border-light);
  border-radius: var(--rh-radius);
}

.post-item:hover {
  border-color: var(--rh-primary);
}

.post-item__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.post-item__author {
  font-weight: 600;
  font-size: 13px;
}

.post-item__top {
  margin-left: auto;
  font-size: 12px;
  color: var(--rh-danger);
  background: #fdecee;
  padding: 2px 8px;
  border-radius: 4px;
}

.post-item__title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
}

.post-item__content {
  font-size: 13px;
  color: var(--rh-text-sub);
}

.post-item__images {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.post-item__images img {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.post-item__meta {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
}

.circle-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 8px;
  border-radius: var(--rh-radius);
  cursor: pointer;
  margin-bottom: 4px;
}

.circle-item:hover,
.circle-item--active {
  background: var(--rh-primary-light);
}

.circle-item img {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  background: var(--rh-surface-sub);
}

.circle-item__name {
  font-weight: 600;
  font-size: 13px;
}

.circle-item__all {
  font-size: 13px;
  padding: 4px 0;
}
</style>
