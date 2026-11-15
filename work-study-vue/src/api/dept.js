import request from '@/utils/request'

/**
 * 用工部门管理（契约第 5 节 DeptController，根路径 /dept）
 */

// 查询全部部门（下拉用）：GET /dept/list
export const listApi = () => {
  return request.get('/dept/list')
}

// 分页查询部门：GET /dept/page，入参 DeptPageQueryDTO{name,page,pageSize}
export const pageApi = (params) => {
  return request.get('/dept/page', { params })
}

// 新增部门：POST /dept，入参 DeptDTO{id,name,contactPerson,contactPhone,status}
export const addApi = (dept) => {
  return request.post('/dept', dept)
}

// 修改部门：PUT /dept，入参 DeptDTO
export const updateApi = (dept) => {
  return request.put('/dept', dept)
}

// 根据 id 查询部门：GET /dept/{id}
export const getByIdApi = (id) => {
  return request.get(`/dept/${id}`)
}

// 删除部门：DELETE /dept/{id}
export const deleteApi = (id) => {
  return request.delete(`/dept/${id}`)
}

// 启用/禁用部门：POST /dept/status/{status}?id=  （status：1 启用、0 禁用）
export const startOrStopApi = (status, id) => {
  return request.post(`/dept/status/${status}?id=${id}`)
}
