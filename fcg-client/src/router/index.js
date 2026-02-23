import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const Login = () => import('../views/Login.vue')
const Home = () => import('../views/Home.vue')
const FamilyHome = () => import('../views/FamilyHome.vue')
const MedicineHome = () => import('../views/MedicineHome.vue')
const HealthHome = () => import('../views/HealthHome.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: Login, meta: { public: true } },
    { path: '/', name: 'home', component: Home },
    { path: '/family', name: 'family', component: FamilyHome },
    { path: '/medicine', name: 'medicine', component: MedicineHome },
    { path: '/health', name: 'health', component: HealthHome }
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.public) return true
  if (userStore.token) return true
  return { name: 'login', query: { redirect: to.fullPath } }
})

export default router
