import request from '@/utils/request'

/**
 * 薪酬规则（契约第 5 节 SalaryRuleController，根路径 /salaryRule）
 */

// 分页查询规则：GET /salaryRule/page，入参 SalaryRulePageQueryDTO{name,semesterId,category,page,pageSize}
export const pageApi = (params) => {
  return request.get('/salaryRule/page', { params })
}

// 查询规则列表：GET /salaryRule/list
export const listApi = (params) => {
  return request.get('/salaryRule/list', { params })
}

// 新增规则：POST /salaryRule，入参 SalaryRuleDTO{id,name,semesterId,category,difficultyLevel,baseWage,coefficient,maxMonthly,minMonthly,priority,status}
export const addApi = (salaryRule) => {
  return request.post('/salaryRule', salaryRule)
}

// 修改规则：PUT /salaryRule，入参 SalaryRuleDTO
export const updateApi = (salaryRule) => {
  return request.put('/salaryRule', salaryRule)
}

// 删除规则：DELETE /salaryRule/{id}
export const deleteApi = (id) => {
  return request.delete(`/salaryRule/${id}`)
}
