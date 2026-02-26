import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// 懒加载所有页面
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const SelectMember = () => import('../views/SelectMember.vue')
const Home = () => import('../views/Home.vue')
const FamilyHome = () => import('../views/FamilyHome.vue')
const MedicineHome = () => import('../views/MedicineHome.vue')
const HealthHome = () => import('../views/HealthHome.vue')
const AdminHome = () => import('../views/AdminHome.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 公开页面（无需 token）
    { path: '/login', name: 'login', component: Login, meta: { public: true } },
    { path: '/register', name: 'register', component: Register, meta: { public: true } },

    // 选人页（需要家庭级 token）
    { path: '/select-member', name: 'select-member', component: SelectMember, meta: { requireFamily: true } },

    // 业务页面（需要成员级 token）
    { path: '/', name: 'home', component: Home, meta: { requireMember: true } },
    { path: '/family', name: 'family', component: FamilyHome, meta: { requireMember: true } },
    { path: '/medicine', name: 'medicine', component: MedicineHome, meta: { requireMember: true } },
    { path: '/health', name: 'health', component: HealthHome, meta: { requireMember: true } },

    // 管理员页面（需要成员级 token + 管理员角色）
    { path: '/admin', name: 'admin', component: AdminHome, meta: { requireMember: true, requireAdmin: true } },
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  // 公开页面直接放行
  if (to.meta.public) return true

  // 需要家庭级 token（选人页）
  if (to.meta.requireFamily) {
    if (!userStore.isLoggedIn) return { name: 'login' }
    return true
  }

  // 需要成员级 token（业务页面）
  if (to.meta.requireMember) {
    if (!userStore.isLoggedIn) return { name: 'login' }
    if (!userStore.hasMember) return { name: 'select-member' }
    return true
  }

  return true
})

export default router
