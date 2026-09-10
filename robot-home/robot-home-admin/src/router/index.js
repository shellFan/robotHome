import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'
import Layout from '@/layout/index.vue'

/**
 * 静态路由表：所有业务页面均在此注册。
 * 左侧菜单默认由后端 GET /api/admin/auth/info 返回的 menus 树驱动，
 * 若接口不可用则回退到 defaultMenus（见 layout/components/Sidebar.vue）。
 */
export const defaultMenus = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    menuName: 'Dashboard',
    icon: 'Odometer',
    component: () => import('@/views/dashboard/index.vue')
  },
  {
    path: '/user',
    name: 'User',
    menuName: '用户管理',
    icon: 'User',
    component: () => import('@/views/user/index.vue')
  },
  {
    path: '/robot',
    name: 'Robot',
    menuName: '机器人管理',
    icon: 'Cpu',
    redirect: '/robot/model',
    children: [
      {
        path: 'category',
        name: 'RobotCategory',
        menuName: '分类管理',
        icon: 'Menu',
        component: () => import('@/views/robot/category.vue')
      },
      {
        path: 'series',
        name: 'RobotSeries',
        menuName: '系列管理',
        icon: 'Files',
        component: () => import('@/views/robot/series.vue')
      },
      {
        path: 'model',
        name: 'RobotModel',
        menuName: '型号管理',
        icon: 'Aim',
        component: () => import('@/views/robot/model.vue')
      },
      {
        path: 'template',
        name: 'RobotTemplate',
        menuName: '参数模板',
        icon: 'Tickets',
        component: () => import('@/views/robot/template.vue')
      },
      {
        path: 'image',
        name: 'RobotImage',
        menuName: '图片管理',
        icon: 'Picture',
        component: () => import('@/views/robot/image.vue')
      },
      {
        path: 'video',
        name: 'RobotVideo',
        menuName: '视频管理',
        icon: 'VideoCamera',
        component: () => import('@/views/robot/video.vue')
      }
    ]
  },
  {
    path: '/brand',
    name: 'Brand',
    menuName: '品牌管理',
    icon: 'Star',
    component: () => import('@/views/brand/index.vue')
  },
  {
    path: '/company',
    name: 'Company',
    menuName: '企业管理',
    icon: 'OfficeBuilding',
    component: () => import('@/views/company/index.vue')
  },
  {
    path: '/article',
    name: 'Article',
    menuName: '资讯管理',
    icon: 'Document',
    redirect: '/article/list',
    children: [
      {
        path: 'category',
        name: 'ArticleCategory',
        menuName: '栏目管理',
        icon: 'Menu',
        component: () => import('@/views/article/category.vue')
      },
      {
        path: 'list',
        name: 'ArticleList',
        menuName: '文章管理',
        icon: 'Document',
        component: () => import('@/views/article/list.vue')
      }
    ]
  },
  {
    path: '/video',
    name: 'Video',
    menuName: '视频管理',
    icon: 'VideoCamera',
    redirect: '/video/list',
    children: [
      {
        path: 'category',
        name: 'VideoCategory',
        menuName: '频道管理',
        icon: 'Menu',
        component: () => import('@/views/video/category.vue')
      },
      {
        path: 'list',
        name: 'VideoList',
        menuName: '视频管理',
        icon: 'VideoCamera',
        component: () => import('@/views/video/list.vue')
      }
    ]
  },
  {
    path: '/community',
    name: 'Community',
    menuName: '社区管理',
    icon: 'ChatDotRound',
    redirect: '/community/post',
    children: [
      {
        path: 'circle',
        name: 'CommunityCircle',
        menuName: '圈子管理',
        icon: 'Coin',
        component: () => import('@/views/community/circle.vue')
      },
      {
        path: 'post',
        name: 'CommunityPost',
        menuName: '帖子管理',
        icon: 'ChatLineSquare',
        component: () => import('@/views/community/post.vue')
      },
      {
        path: 'comment',
        name: 'CommunityComment',
        menuName: '评论管理',
        icon: 'ChatDotSquare',
        component: () => import('@/views/community/comment.vue')
      }
    ]
  },
  {
    path: '/tutorial',
    name: 'Tutorial',
    menuName: '教程管理',
    icon: 'Reading',
    redirect: '/tutorial/list',
    children: [
      {
        path: 'category',
        name: 'TutorialCategory',
        menuName: '分类管理',
        icon: 'Menu',
        component: () => import('@/views/tutorial/category.vue')
      },
      {
        path: 'list',
        name: 'TutorialList',
        menuName: '教程管理',
        icon: 'Reading',
        component: () => import('@/views/tutorial/list.vue')
      }
    ]
  },
  {
    path: '/inquiry',
    name: 'Inquiry',
    menuName: '询价管理',
    icon: 'Tickets',
    component: () => import('@/views/inquiry/index.vue')
  },
  {
    path: '/operation',
    name: 'Operation',
    menuName: '运营配置',
    icon: 'Operation',
    redirect: '/operation/banner',
    children: [
      {
        path: 'banner',
        name: 'OperationBanner',
        menuName: 'Banner管理',
        icon: 'Picture',
        component: () => import('@/views/operation/banner.vue')
      },
      {
        path: 'recommend',
        name: 'OperationRecommend',
        menuName: '推荐位管理',
        icon: 'Star',
        component: () => import('@/views/operation/recommend.vue')
      }
    ]
  },
  {
    path: '/system',
    name: 'System',
    menuName: '系统管理',
    icon: 'Setting',
    redirect: '/system/admin',
    children: [
      {
        path: 'admin',
        name: 'SystemAdmin',
        menuName: '管理员',
        icon: 'UserFilled',
        component: () => import('@/views/system/admin.vue')
      },
      {
        path: 'role',
        name: 'SystemRole',
        menuName: '角色',
        icon: 'Avatar',
        component: () => import('@/views/system/role.vue')
      },
      {
        path: 'menu',
        name: 'SystemMenu',
        menuName: '菜单',
        icon: 'Menu',
        component: () => import('@/views/system/menu.vue')
      },
      {
        path: 'permission',
        name: 'SystemPermission',
        menuName: '权限',
        icon: 'Key',
        component: () => import('@/views/system/permission.vue')
      },
      {
        path: 'log',
        name: 'SystemLog',
        menuName: '日志',
        icon: 'Document',
        component: () => import('@/views/system/log.vue')
      },
      {
        path: 'dict',
        name: 'SystemDict',
        menuName: '字典',
        icon: 'Collection',
        component: () => import('@/views/system/dict.vue')
      },
      {
        path: 'config',
        name: 'SystemConfig',
        menuName: '配置',
        icon: 'Tools',
        component: () => import('@/views/system/config.vue')
      }
    ]
  },
  {
    path: '/crawler',
    name: 'Crawler',
    menuName: '采集管理',
    icon: 'Download',
    redirect: '/crawler/source',
    children: [
      {
        path: 'source',
        name: 'CrawlerSource',
        menuName: '数据源',
        icon: 'Link',
        component: () => import('@/views/crawler/source.vue')
      },
      {
        path: 'task',
        name: 'CrawlerTask',
        menuName: '采集任务',
        icon: 'List',
        component: () => import('@/views/crawler/task.vue')
      },
      {
        path: 'content',
        name: 'CrawlerContent',
        menuName: '内容审核',
        icon: 'DocumentChecked',
        component: () => import('@/views/crawler/content.vue')
      }
    ]
  }
]

/** 拼接完整路径：子菜单可能是相对路径（category），也可能是绝对路径（/robot/category） */
function joinPath(parentPath, path) {
  if (!path) return parentPath
  if (path.startsWith('/')) return path
  return `${parentPath.replace(/\/$/, '')}/${path}`
}

/** 把菜单树拍平为可注册路由；目录节点注册为 redirect */
function toRoutes(menus, parentPath = '') {
  const routes = []
  ;(menus || []).forEach((item) => {
    const fullPath = joinPath(parentPath, item.path)
    if (item.children && item.children.length) {
      const childRoutes = toRoutes(item.children, fullPath)
      const firstChild = childRoutes.find((r) => r.path)
      routes.push({
        path: fullPath,
        redirect: item.redirect || (firstChild ? firstChild.path : undefined)
      })
      routes.push(...childRoutes)
    } else if (item.component) {
      routes.push({
        path: fullPath,
        name: item.name,
        component: item.component,
        meta: { title: item.menuName, icon: item.icon }
      })
    }
  })
  return routes
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: toRoutes(defaultMenus)
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to, from, next) => {
  const token = getToken()
  if (to.path === '/login') {
    if (token) {
      next({ path: '/dashboard' })
    } else {
      next()
    }
    return
  }
  if (!token) {
    next({ path: '/login', query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : undefined })
    return
  }
  // 已登录：确保管理员信息（含菜单）已加载
  const userStore = useUserStore()
  if (!userStore.loaded) {
    try {
      await userStore.fetchInfo()
    } catch (e) {
      userStore.resetState()
      ElMessage.error('获取管理员信息失败，请重新登录')
      next({ path: '/login' })
      return
    }
  }
  next()
})

export default router
