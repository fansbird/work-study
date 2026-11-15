import request from '@/utils/request'

/**
 * 统一账号管理（契约第 5 节 SysUserController，根路径 /sysUser）
 */

// 分页查询账号：GET /sysUser/page，入参 SysUserPageQueryDTO{name,role,page,pageSize}
export const pageApi = (params) => {
  return request.get('/sysUser/page', { params })
}

// 新增账号：POST /sysUser，入参 SysUserDTO{id,username,name,phone,role,deptId,studentId,avatar,status}
export const addApi = (sysUser) => {
  return request.post('/sysUser', sysUser)
}

// 修改账号：PUT /sysUser，入参 SysUserDTO
export const updateApi = (sysUser) => {
  return request.put('/sysUser', sysUser)
}

// 根据 id 查询账号：GET /sysUser/{id}
export const getByIdApi = (id) => {
  return request.get(`/sysUser/${id}`)
}

// 启用/禁用账号：POST /sysUser/status/{status}?id=  （status：1 启用、0 禁用）
export const startOrStopApi = (status, id) => {
  return request.post(`/sysUser/status/${status}?id=${id}`)
}

// 重置密码：POST /sysUser/resetPassword，入参 PasswordEditDTO{id,newPassword}
export const resetPasswordApi = (passwordEditDTO) => {
  return request.post('/sysUser/resetPassword', passwordEditDTO)
}
