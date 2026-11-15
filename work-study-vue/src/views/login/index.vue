<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { loginApi } from '@/api/login'

const router = useRouter()

// 登录表单：LoginDTO{username,password}
const loginForm = ref({ username: '', password: '' })

// 登录成功后统一进入工作台，再由路由守卫按 LoginVO.role 动态注入该角色可见的菜单
const HOME_PATH = '/dashboard'

// 登录
const login = async () => {
  if (!loginForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  try {
    const res = await loginApi(loginForm.value)
    if (res.code) {
      // 把 LoginVO{id,username,name,role,token,deptId,studentId,avatar} 整体存入 localStorage（key = token）
      // 请求拦截器会取其中的 .token 放到请求头 token；路由守卫会取 .role 过滤菜单
      localStorage.setItem('token', JSON.stringify(res.data))
      ElMessage.success('登录成功')
      router.push(HOME_PATH)
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 左侧品牌区 -->
      <div class="login-left">
        <el-icon class="left-icon"><School /></el-icon>
        <div class="left-title">校园勤工助学</div>
        <div class="left-subtitle">岗位 · 工时 · 薪酬结算</div>
      </div>
      <!-- 右侧登录表单 -->
      <div class="login-right">
        <div class="brand">
          <el-icon class="brand-icon"><School /></el-icon>
          <div>
            <div class="brand-title">校园勤工助学管理系统</div>
            <div class="brand-subtitle">CAMPUS WORK-STUDY MANAGEMENT</div>
          </div>
        </div>
        <el-form @submit.prevent>
          <el-form-item>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="login"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" class="login-btn" size="large" @click="login">登 录</el-button>
          </el-form-item>
        </el-form>
        <div class="login-tip">不同角色的账号登录后，左侧菜单会按角色自动变化</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: 100vh;
  background-color: #3a3a3a;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-card {
  width: 820px;
  height: 430px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

/* 左侧品牌区 */
.login-left {
  width: 50%;
  background: linear-gradient(135deg, #2c3e50, #4a6278);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
}

.left-icon {
  font-size: 96px;
  color: rgba(255, 255, 255, 0.35);
  margin-bottom: 16px;
}

.left-title {
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 2px;
}

.left-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8px;
  letter-spacing: 1px;
}

/* 右侧表单区 */
.login-right {
  width: 50%;
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 30px;
  justify-content: center;
}

.brand-icon {
  font-size: 34px;
  color: #ffb400;
}

.brand-title {
  font-size: 19px;
  font-weight: bold;
  color: #333;
}

.brand-subtitle {
  font-size: 9px;
  color: #999;
  letter-spacing: 1px;
}

.login-btn {
  width: 100%;
  background-color: #ffb400;
  border-color: #ffb400;
  color: #fff;
  font-size: 16px;
  letter-spacing: 8px;
}

.login-btn:hover {
  background-color: #ffc733;
  border-color: #ffc733;
}

.login-tip {
  font-size: 12px;
  color: #999;
  text-align: center;
  margin-top: 4px;
}
</style>
