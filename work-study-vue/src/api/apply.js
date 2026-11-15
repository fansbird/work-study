import request from '@/utils/request'

/**
 * 岗位报名管理（契约第 5 节 PositionApplyController，根路径 /apply）
 * 约定：POST 里的"参数"按查询参数（query）传递，后端用 @RequestParam 接收。
 */

// 学生报名岗位：POST /apply，参数 positionId、applyRemark
export const applyApi = (positionId, applyRemark) => {
  return request.post('/apply', null, { params: { positionId, applyRemark } })
}

// 撤销报名：POST /apply/cancel/{id}  （状态 0 待审核 → 3 已撤销）
export const cancelApi = (id) => {
  return request.post(`/apply/cancel/${id}`)
}

// 报名审核：POST /apply/audit，参数 id、status(1 已录用 / 2 已驳回)、rejectReason
export const auditApi = (params) => {
  return request.post('/apply/audit', null, { params })
}

// 放弃录用：POST /apply/giveUp/{id}  （状态 1 已录用 → 4 已放弃，岗位 hired-1）
export const giveUpApi = (id) => {
  return request.post(`/apply/giveUp/${id}`)
}

// 分页查询报名（部门/中心端）：GET /apply/page，入参 PositionApplyPageQueryDTO{positionId,semesterId,status,studentId,page,pageSize}
export const pageApi = (params) => {
  return request.get('/apply/page', { params })
}

// 我的报名分页（学生端）：GET /apply/myPage
export const myPageApi = (params) => {
  return request.get('/apply/myPage', { params })
}

// 校验当前学生是否已报名某岗位：GET /apply/check/{positionId}
export const checkApi = (positionId) => {
  return request.get(`/apply/check/${positionId}`)
}
