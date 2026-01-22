<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside width="200px" class="layout-aside">
      <div class="logo-section">
        <h3>LLM Assistant</h3>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        router
      >
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 对话</span>
        </el-menu-item>
        
        <el-menu-item index="/documents">
          <el-icon><Document /></el-icon>
          <span>文档管理</span>
        </el-menu-item>

        <el-menu-item index="/analyze">
          <el-icon><DataLine /></el-icon>
          <span>研读分析</span>
        </el-menu-item>

        <el-menu-item index="/write">
          <el-icon><EditPen /></el-icon>
          <span>写作助手</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- Main content area -->
    <el-container>
      <!-- Header -->
      <el-header class="layout-header">
        <div class="header-title">
          <span>{{ pageTitle }}</span>
        </div>
        
        <div class="header-actions">
          <el-dropdown>
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              <span>{{ userStore.username || '用户' }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Main content -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { 
  ChatDotRound, 
  Document, 
  User, 
  ArrowDown, 
  SwitchButton,
  DataLine,
  EditPen
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// Computed properties
const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/chat': 'AI 智能对话',
    '/documents': '文档管理',
    '/analyze': '智能研读分析',
    '/write': '学术写作助手'
  }
  return titles[route.path] || 'LLM Research Assistant'
})

// Handle logout
const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background: #001529;
  color: #fff;
}

.logo-section {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-section h3 {
  margin: 0;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.el-menu-vertical {
  border-right: none;
  background: #001529;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.85);
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

:deep(.el-menu-item.is-active) {
  background-color: #1890ff;
  color: #fff;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-dropdown:hover {
  background-color: #f5f5f5;
}

.layout-main {
  background: #f5f5f5;
  padding: 20px;
  overflow: auto;
}
</style>
