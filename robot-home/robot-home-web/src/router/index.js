import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '机器人之家 - 机器人行业的懂车帝 + 汽车之家' } },
  { path: '/search', name: 'search', component: () => import('@/views/Search.vue'), meta: { title: '搜索 - 机器人之家' } },
  { path: '/robots', name: 'robots', component: () => import('@/views/RobotList.vue'), meta: { title: '机器人库 - 机器人之家' } },
  { path: '/robot/:id', name: 'robot-detail', component: () => import('@/views/RobotDetail.vue'), meta: { title: '机器人详情 - 机器人之家' } },
  { path: '/robot/:id/params', name: 'robot-params', component: () => import('@/views/RobotParams.vue'), meta: { title: '机器人参数 - 机器人之家' } },
  { path: '/robot/:id/images', name: 'robot-images', component: () => import('@/views/RobotImages.vue'), meta: { title: '机器人图片 - 机器人之家' } },
  { path: '/robot/:id/videos', name: 'robot-videos', component: () => import('@/views/RobotVideos.vue'), meta: { title: '机器人视频 - 机器人之家' } },
  { path: '/robot/:id/reviews', name: 'robot-reviews', component: () => import('@/views/RobotReviews.vue'), meta: { title: '机器人口碑 - 机器人之家' } },
  { path: '/compare', name: 'compare', component: () => import('@/views/Compare.vue'), meta: { title: '参数对比 - 机器人之家' } },
  { path: '/rankings', name: 'rankings', component: () => import('@/views/Rankings.vue'), meta: { title: '排行榜 - 机器人之家' } },
  { path: '/brands', name: 'brands', component: () => import('@/views/BrandList.vue'), meta: { title: '品牌库 - 机器人之家' } },
  { path: '/brand/:id', name: 'brand-detail', component: () => import('@/views/BrandDetail.vue'), meta: { title: '品牌详情 - 机器人之家' } },
  { path: '/companies', name: 'companies', component: () => import('@/views/CompanyList.vue'), meta: { title: '企业库 - 机器人之家' } },
  { path: '/company/:id', name: 'company-detail', component: () => import('@/views/CompanyDetail.vue'), meta: { title: '企业详情 - 机器人之家' } },
  { path: '/articles', name: 'articles', component: () => import('@/views/ArticleList.vue'), meta: { title: '资讯 - 机器人之家' } },
  { path: '/article/:id', name: 'article-detail', component: () => import('@/views/ArticleDetail.vue'), meta: { title: '资讯详情 - 机器人之家' } },
  { path: '/videos', name: 'videos', component: () => import('@/views/VideoList.vue'), meta: { title: '视频 - 机器人之家' } },
  { path: '/video/:id', name: 'video-detail', component: () => import('@/views/VideoDetail.vue'), meta: { title: '视频详情 - 机器人之家' } },
  { path: '/tutorials', name: 'tutorials', component: () => import('@/views/TutorialList.vue'), meta: { title: '教程 - 机器人之家' } },
  { path: '/tutorial/:id', name: 'tutorial-detail', component: () => import('@/views/TutorialDetail.vue'), meta: { title: '教程详情 - 机器人之家' } },
  { path: '/community', name: 'community', component: () => import('@/views/Community.vue'), meta: { title: '社区 - 机器人之家' } },
  { path: '/community/create', name: 'post-create', component: () => import('@/views/PostCreate.vue'), meta: { title: '发帖 - 机器人之家', auth: true } },
  { path: '/community/:id', name: 'post-detail', component: () => import('@/views/PostDetail.vue'), meta: { title: '帖子详情 - 机器人之家' } },
  { path: '/inquiry/:robotId', name: 'inquiry', component: () => import('@/views/Inquiry.vue'), meta: { title: '获取报价 - 机器人之家' } },
  { path: '/feedback', name: 'feedback', component: () => import('@/views/Feedback.vue'), meta: { title: '意见反馈 - 机器人之家' } },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { title: '登录 - 机器人之家' } },
  {
    path: '/user',
    component: () => import('@/views/user/UserLayout.vue'),
    meta: { auth: true },
    redirect: '/user/profile',
    children: [
      { path: 'profile', name: 'user-profile', component: () => import('@/views/user/Profile.vue'), meta: { title: '个人资料 - 机器人之家' } },
      { path: 'favorites', name: 'user-favorites', component: () => import('@/views/user/Favorites.vue'), meta: { title: '我的收藏 - 机器人之家' } },
      { path: 'history', name: 'user-history', component: () => import('@/views/user/History.vue'), meta: { title: '浏览历史 - 机器人之家' } },
      { path: 'posts', name: 'user-posts', component: () => import('@/views/user/MyPosts.vue'), meta: { title: '我的帖子 - 机器人之家' } },
      { path: 'comments', name: 'user-comments', component: () => import('@/views/user/MyComments.vue'), meta: { title: '我的评论 - 机器人之家' } },
      { path: 'follows', name: 'user-follows', component: () => import('@/views/user/Follows.vue'), meta: { title: '我的关注 - 机器人之家' } },
      { path: 'inquiries', name: 'user-inquiries', component: () => import('@/views/user/Inquiries.vue'), meta: { title: '我的询价 - 机器人之家' } },
      { path: 'messages', name: 'user-messages', component: () => import('@/views/user/Messages.vue'), meta: { title: '我的消息 - 机器人之家' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue'), meta: { title: '页面不存在 - 机器人之家' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior () {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  if (to.meta && to.meta.auth) {
    const store = useUserStore()
    if (!store.isLogin) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (!store.profile) {
      await store.loadProfile()
      if (!store.isLogin) {
        return { name: 'login', query: { redirect: to.fullPath } }
      }
    }
  }
  return true
})

router.afterEach((to) => {
  if (to.meta && to.meta.title) {
    document.title = to.meta.title
  }
})

export default router
