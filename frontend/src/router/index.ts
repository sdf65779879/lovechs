import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useUserStore } from '@/stores/user'

/** 用户端页面（布局内） */
const userRoutes: RouteRecordRaw = {
  path: '/',
  component: () => import('@/layouts/UserLayout.vue'),
  children: [
    { path: '', redirect: '/home' },
    {
      path: 'home',
      name: 'home',
      meta: { title: '今天想吃点什么？' },
      component: () => import('@/views/user/HomeView.vue'),
    },
    {
      path: 'cart',
      name: 'cart',
      meta: { title: '购物车' },
      component: () => import('@/views/user/CartView.vue'),
    },
    {
      path: 'order-confirm',
      name: 'order-confirm',
      meta: { title: '确认订单' },
      component: () => import('@/views/user/OrderConfirmView.vue'),
    },
    {
      path: 'orders',
      name: 'orders',
      meta: { title: '我的订单' },
      component: () => import('@/views/user/OrderListView.vue'),
    },
    {
      path: 'orders/:id',
      name: 'order-detail',
      meta: { title: '订单详情' },
      component: () => import('@/views/user/OrderDetailView.vue'),
    },
    {
      path: 'me',
      name: 'me',
      meta: { title: '我的' },
      component: () => import('@/views/user/MeView.vue'),
    },
    {
      path: 'me/favorites',
      name: 'my-favorites',
      meta: { title: '我的收藏' },
      component: () => import('@/views/user/FavoritesView.vue'),
    },
    {
      path: 'me/calendar',
      name: 'my-calendar',
      meta: { title: '吃饭日历' },
      component: () => import('@/views/user/CalendarView.vue'),
    },
    {
      path: 'me/password',
      name: 'my-password',
      meta: { title: '修改密码' },
      component: () => import('@/views/user/PasswordView.vue'),
    },
  ],
}

/** 管理端页面（布局内，PC 优先） */
const adminRoutes: RouteRecordRaw = {
  path: '/admin',
  component: () => import('@/layouts/AdminLayout.vue'),
  children: [
    { path: '', redirect: '/admin/dashboard' },
    {
      path: 'dashboard',
      name: 'admin-dashboard',
      meta: { title: '数据看板', admin: true },
      component: () => import('@/views/admin/DashboardView.vue'),
    },
    {
      path: 'dishes',
      name: 'admin-dishes',
      meta: { title: '菜品管理', admin: true },
      component: () => import('@/views/admin/DishAdminView.vue'),
    },
    {
      path: 'categories',
      name: 'admin-categories',
      meta: { title: '分类管理', admin: true },
      component: () => import('@/views/admin/CategoryAdminView.vue'),
    },
    {
      path: 'orders',
      name: 'admin-orders',
      meta: { title: '订单管理', admin: true },
      component: () => import('@/views/admin/OrderAdminView.vue'),
    },
    {
      path: 'users',
      name: 'admin-users',
      meta: { title: '用户管理', admin: true },
      component: () => import('@/views/admin/UserAdminView.vue'),
    },
    {
      path: 'settings',
      name: 'admin-settings',
      meta: { title: '系统设置', admin: true },
      component: () => import('@/views/admin/SettingAdminView.vue'),
    },
  ],
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    meta: { title: '登录' },
    component: () => import('@/views/user/LoginView.vue'),
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    meta: { title: '大厨登录' },
    component: () => import('@/views/admin/AdminLoginView.vue'),
  },
  userRoutes,
  adminRoutes,
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  const isAdminArea = to.path.startsWith('/admin')
  const isAdminLogin = to.path === '/admin/login'

  // 登录页直接放行
  if (to.path === '/login' || isAdminLogin) {
    // 已登录还访问登录页：直接送回家/工作台
    if (userStore.isLogin && to.path === '/login') {
      return userStore.isAdmin ? '/admin/dashboard' : '/home'
    }
    if (userStore.isLogin && isAdminLogin) {
      return userStore.isAdmin ? '/admin/dashboard' : '/home'
    }
    document.title = (to.meta.title as string) || '登录'
    return true
  }

  // 未登录：管理端跳管理登录，用户端跳用户登录
  if (!userStore.isLogin) {
    if (isAdminArea) {
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 管理端需 ADMIN 角色
  if (isAdminArea && !userStore.isAdmin) {
    return { path: '/home' }
  }

  document.title = (to.meta.title as string) || '今天想吃点什么？'
  return true
})

export default router
