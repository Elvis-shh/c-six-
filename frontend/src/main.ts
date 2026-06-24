import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { getMe } from './api'
import { useAuthStore } from './stores/authStore'
import './styles/global.scss'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

const auth = useAuthStore(pinia)
if (auth.accessToken && !auth.user) {
  getMe()
    .then(res => {
      const me = res.data.data
      if (me) {
        auth.setUser({ userId: me.userId, email: me.email, nickname: me.nickname })
      }
    })
    .catch(() => {
      auth.logout()
    })
}

app.mount('#app')
