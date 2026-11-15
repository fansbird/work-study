import request from '@/utils/request'

/**
 * 学生档案管理（契约第 5 节 StudentController，根路径 /student）
 */

// 分页查询学生：GET /student/page，入参 StudentPageQueryDTO{name,studentNo,college,page,pageSize}
export const pageApi = (params) => {
  return request.get('/student/page', { params })
}

// 查询全部学生（下拉用）：GET /student/list
export const listApi = () => {
  return request.get('/student/list')
}

// 新增学生：POST /student，入参 StudentDTO{id,userId,studentNo,name,gender,phone,college,major,className,grade,difficultyLevel,bankCard,status}
export const addApi = (student) => {
  return request.post('/student', student)
}

// 修改学生：PUT /student，入参 StudentDTO
export const updateApi = (student) => {
  return request.put('/student', student)
}

// 根据 id 查询学生：GET /student/{id}
export const getByIdApi = (id) => {
  return request.get(`/student/${id}`)
}

// 根据登录账号 id 查询学生档案：GET /student/getByUserId/{userId}
export const getByUserIdApi = (userId) => {
  return request.get(`/student/getByUserId/${userId}`)
}

// 删除学生：DELETE /student/{id}
export const deleteApi = (id) => {
  return request.delete(`/student/${id}`)
}
