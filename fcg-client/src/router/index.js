import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// 懒加载所有页面
const Landing = () => import('../views/Landing.vue')
const SelectMember = () => import('../views/SelectMember.vue')
const HomeEntry = () => import('../views/HomeEntry.vue')
const FamilyHome = () => import('../views/FamilyHome.vue')
const MedicineHome = () => import('../views/MedicineHome.vue')
const HealthEntry = () => import('../views/HealthEntry.vue')
const ProfileEntry = () => import('../views/ProfileEntry.vue')
const AdminLayout = () => import('../components/common/AdminLayout.vue')
const AdminDashboard = () => import('../views/AdminHome.vue')
const AdminMembers = () => import('../views/admin/AdminMembers.vue')
const AdminMedicines = () => import('../views/admin/AdminMedicines.vue')
const AdminSystem = () => import('../views/admin/AdminSystem.vue')
const AdminData = () => import('../views/admin/AdminData.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 公开页面（无需 token）
    { path: '/', name: 'landing', component: Landing, meta: { public: true } },

    // 选人页（需要家庭级 token）
    { path: '/select-member', name: 'select-member', component: SelectMember, meta: { requireFamily: true } },

    // 业务页面（需要成员级 token）
    { path: '/home', name: 'home', component: HomeEntry, meta: { requireMember: true } },
    { path: '/medicine', name: 'medicine', component: MedicineHome, meta: { requireMember: true } },
    { path: '/health', name: 'health', component: HealthEntry, meta: { requireMember: true } },
    { path: '/ai', name: 'ai', component: () => import('../views/AiHome.vue'), meta: { requireMember: true } },
    { path: '/profile', name: 'profile', component: ProfileEntry, meta: { requireMember: true } },

    // 管理界面路由（/admin 子路由）
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requireMember: true, requireAdmin: true },
      children: [
        { path: '', name: 'admin', component: AdminDashboard },
        { path: 'members', name: 'admin-members', component: AdminMembers },
        { path: 'medicines', name: 'admin-medicines', component: AdminMedicines },
        { path: 'system', name: 'admin-system', component: AdminSystem },
        { path: 'data', name: 'admin-data', component: AdminData },
      ]
    },

    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 初始化时清理残留状态，防止"游客"状态
  if (to.name === 'landing') {
    userStore.initCleanup()
  }

  // 调试信息
  console.log('[Router Guard]', {
    to: to.name,
    isLoggedIn: userStore.isLoggedIn,
    hasMember: userStore.hasMember,
    member: userStore.member ? `${userStore.member.nickname}(${userStore.member.userId})` : null
  })

  // 公开页面直接放行
  if (to.meta.public) return true

  // 需要家庭级 token（选人页）
  if (to.meta.requireFamily) {
    if (!userStore.isLoggedIn) {
      console.log('[Router Guard] 未登录，跳转登录页')
      return { name: 'landing' }
    }
    return true
  }

  // 需要成员级 token（业务页面）
  if (to.meta.requireMember) {
    if (!userStore.isLoggedIn) {
      console.log('[Router Guard] 未登录，跳转登录页')
      return { name: 'landing' }
    }
    if (!userStore.hasMember) {
      console.log('[Router Guard] 未选择成员，跳转选人页')
      return { name: 'select-member' }
    }
    
    // 如果有 token 但没有 member 信息，先加载
    if (!userStore.member) {
      console.log('[Router Guard] 正在加载成员信息...')
      try {
        await userStore.fetchProfile()
      } catch (err) {
        console.log('[Router Guard] 加载失败，清除状态')
        userStore.logout()
        return { name: 'landing' }
      }
    }
    
    // 管理员页面权限检查
    if (to.meta.requireAdmin && !userStore.isAdmin) {
      console.log('[Router Guard] 非管理员，跳转首页')
      return { name: 'home' }
    }
    
    return true
  }

  return true
})

export default router
