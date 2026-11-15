import request from '@/utils/request'

/**
 * 岗位管理（契约第 5 节 PositionController，根路径 /position）
 */

// 分页查询岗位：GET /position/page，入参 PositionPageQueryDTO{name,deptId,category,semesterId,status,page,pageSize}
export const pageApi = (params) => {
  return request.get('/position/page', { params })
}

// 查询已发布岗位列表（学生端浏览 / 下拉）：GET /position/list
export const listApi = (params) => {
  return request.get('/position/list', { params })
}

// 新增岗位（存草稿）：POST /position，入参 PositionDTO{id,name,deptId,category,semesterId,quota,hourlyWage,workContent,workLocation,contactPerson,contactPhone,status,rejectReason}
export const addApi = (position) => {
  return request.post('/position', position)
}

// 修改岗位（仅草稿 / 驳回可改）：PUT /position，入参 PositionDTO
export const updateApi = (position) => {
  return request.put('/position', position)
}

// 根据 id 查询岗位：GET /position/{id}
export const getByIdApi = (id) => {
  return request.get(`/position/${id}`)
}

// 提交审批：POST /position/submit/{id}  （状态 0 草稿/3 驳回 → 1 待审批）
export const submitApi = (id) => {
  return request.post(`/position/submit/${id}`)
}

// 岗位审批：POST /position/audit，参数 id、status(2 已发布 / 3 已驳回)、rejectReason
export const auditApi = (params) => {
  return request.post('/position/audit', null, { params })
}

// 下架岗位：POST /position/offline/{id}  （状态 2 已发布 → 4 已下架）
export const offlineApi = (id) => {
  return request.post(`/position/offline/${id}`)
}

// 删除岗位：DELETE /position/{id}
export const deleteApi = (id) => {
  return request.delete(`/position/${id}`)
}
