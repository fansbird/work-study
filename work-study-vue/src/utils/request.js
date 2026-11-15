import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例，baseURL 为 /api，由 vite 代理到后端 localhost:8080（rewrite 去掉 /api）
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：从 localStorage 取出登录信息（LoginVO），把它里面的 token 放到请求头 token
request.interceptors.request.use(
  (config) => {
    let loginUser = null
    try {
      loginUser = JSON.parse(localStorage.getItem('token'))
    } catch (e) {
      loginUser = null
    }
    if (loginUser && loginUser.token) {
      config.headers.token = loginUser.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 登录过期提示只弹一次，避免同一个页面并发多个请求时弹很多遍
let expiredTipShown = false

// 响应拦截器：直接返回 response.data（即 Result{code,msg,data}）
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      if (!expiredTipShown) {
        expiredTipShown = true
        ElMessage.error('登录过期，请重新登录')
        // 清掉失效的登录信息，否则路由守卫会把用户又送回工作台
        localStorage.removeItem('token')
        router.push('/login')
        setTimeout(() => {
          expiredTipShown = false
        }, 1500)
      }
    }
    return Promise.reject(error)
  }
)

export default request
