import request from '@/utils/request'

/**
 * 工时管理（契约第 5 节 WorkHourController，根路径 /workHour）
 * 约定：POST audit 系列接口的参数按查询参数（query）传递，后端用 @RequestParam 接收。
 */

// 填报工时：POST /workHour，入参 WorkHourDTO{id,positionId,workDate,startTime,endTime,hours,workContent,status,rejectReason}（后端直接置 status=0）
export const addApi = (workHour) => {
  return request.post('/workHour', workHour)
}

// 修改工时（仅 status=3 驳回重填可改）：PUT /workHour，入参 WorkHourDTO
export const updateApi = (workHour) => {
  return request.put('/workHour', workHour)
}

// 重新提交：POST /workHour/submit/{id}  （状态 3 驳回重填 → 0 待部门审核）
export const submitApi = (id) => {
  return request.post(`/workHour/submit/${id}`)
}

// 我的工时分页（学生端）：GET /workHour/myPage，入参 WorkHourPageQueryDTO{positionId,deptId,semesterId,status,studentId,startDate,endDate,page,pageSize}
export const myPageApi = (params) => {
  return request.get('/workHour/myPage', { params })
}

// 工时分页（部门/中心/管理端）：GET /workHour/page
export const pageApi = (params) => {
  return request.get('/workHour/page', { params })
}

// 根据 id 查询工时：GET /workHour/{id}
export const getByIdApi = (id) => {
  return request.get(`/workHour/${id}`)
}

// 部门审核：POST /workHour/deptAudit，参数 id、status(1 提交中心复核 / 3 驳回)、rejectReason
export const deptAuditApi = (params) => {
  return request.post('/workHour/deptAudit', null, { params })
}

// 中心复核：POST /workHour/centerAudit，参数 id、status(2 通过锁定 / 3 驳回)、rejectReason
export const centerAuditApi = (params) => {
  return request.post('/workHour/centerAudit', null, { params })
}

// 删除工时：DELETE /workHour/{id}
export const deleteApi = (id) => {
  return request.delete(`/workHour/${id}`)
}
