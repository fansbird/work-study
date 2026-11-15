import request from '@/utils/request'

/**
 * 学期管理（契约第 5 节 SemesterController，根路径 /semester）
 */

// 查询全部学期：GET /semester/list（status=1 的那条是当前学期）
export const listApi = () => {
  return request.get('/semester/list')
}

// 新增学期：POST /semester，入参 SysSemesterDTO{id,name,startDate,endDate,status}
export const addApi = (semester) => {
  return request.post('/semester', semester)
}

// 修改学期：PUT /semester，入参 SysSemesterDTO
export const updateApi = (semester) => {
  return request.put('/semester', semester)
}

// 设为当前学期：POST /semester/setCurrent/{id}
export const setCurrentApi = (id) => {
  return request.post(`/semester/setCurrent/${id}`)
}

// 删除学期：DELETE /semester/{id}
export const deleteApi = (id) => {
  return request.delete(`/semester/${id}`)
}
