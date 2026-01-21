import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import './style.css'

// Create Vue app
const app = createApp(App)

// Register plugins
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// Mount app
app.mount('#app')

