import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/host/:code', component: () => import('../views/HostView.vue') },
    { path: '/play/:code', component: () => import('../views/PlayerView.vue') },
    { path: '/stats/:code', component: () => import('../views/StatsView.vue') }
  ]
})

export default router
