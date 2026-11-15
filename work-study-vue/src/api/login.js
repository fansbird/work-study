import request from '@/utils/request'

/**
 * 认证相关接口（契约第 5 节 AuthController，根路径）
 */

// 登录：POST /login，入参 LoginDTO{username,password}，返回 LoginVO{id,username,name,role,token,deptId,studentId,avatar}
export const loginApi = (loginDTO) => {
  return request.post('/login', loginDTO)
}

// 退出登录：POST /logout
export const logoutApi = () => {
  return request.post('/logout')
}
