import request from '@/utils/request'

/**
 * 操作日志（契约第 5 节 OperLogController，根路径 /operLog）
 */

// 分页查询操作日志：GET /operLog/page（后端 AOP 自动写入，前端只读）
export const pageApi = (params) => {
  return request.get('/operLog/page', { params })
}

// 根据 id 查询日志详情：GET /operLog/{id}
export const getByIdApi = (id) => {
  return request.get(`/operLog/${id}`)
}
