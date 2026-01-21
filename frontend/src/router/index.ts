import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

// Route definitions
const routes: RouteRecordRaw[] = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        name: 'Layout',
        component: () => import('@/views/Layout.vue'),
        meta: { requiresAuth: true },
        redirect: '/chat',
        children: [
            {
                path: 'chat',
                name: 'Chat',
                component: () => import('@/views/Chat/ChatView.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: 'documents',
                name: 'Documents',
                component: () => import('@/views/Doc/DocList.vue'),
                meta: { requiresAuth: true }
            }
        ]
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/chat'
    }
]

// Create router instance
const router = createRouter({
    history: createWebHistory(),
    routes
})

// Navigation guard - check authentication
router.beforeEach((to, _from, next) => {
    const userStore = useUserStore()
    const requiresAuth = to.meta.requiresAuth !== false // Default to true

    if (requiresAuth && !userStore.isLoggedIn) {
        // Redirect to login if not authenticated
        next('/login')
    } else if (to.path === '/login' && userStore.isLoggedIn) {
        // Redirect to home if already logged in
        next('/chat')
    } else {
        next()
    }
})

export default router
