import request from '@/utils/request'

/**
 * 结算单管理（契约第 5 节 SettlementController，根路径 /settlement）
 */

// 生成结算单：POST /settlement/generate，入参 SettlementGenerateDTO{semesterId,month,deptId}
export const generateApi = (settlementGenerateDTO) => {
  return request.post('/settlement/generate', settlementGenerateDTO)
}

// 分页查询结算单：GET /settlement/page，入参 SettlementPageQueryDTO{semesterId,month,deptId,status,page,pageSize}
export const pageApi = (params) => {
  return request.get('/settlement/page', { params })
}

// 根据 id 查询结算单：GET /settlement/{id}
export const getByIdApi = (id) => {
  return request.get(`/settlement/${id}`)
}

// 查询结算明细：GET /settlement/detail/{settlementId}
export const detailApi = (settlementId) => {
  return request.get(`/settlement/detail/${settlementId}`)
}

// 确认结算单：POST /settlement/confirm/{id}  （状态 0 待确认 → 1 已确认）
export const confirmApi = (id) => {
  return request.post(`/settlement/confirm/${id}`)
}

// 发放结算单：POST /settlement/pay/{id}  （状态 1 已确认 → 2 已发放，之后工时与结算单锁定）
export const payApi = (id) => {
  return request.post(`/settlement/pay/${id}`)
}

// 作废结算单：POST /settlement/void/{id}  （状态 0/1 → 3 已作废）
export const voidApi = (id) => {
  return request.post(`/settlement/void/${id}`)
}

// 结算调整（补发/扣减）：POST /settlement/adjust，入参 SettlementAdjustDTO{settlementId,studentId,adjustType,amount,reason}
export const adjustApi = (settlementAdjustDTO) => {
  return request.post('/settlement/adjust', settlementAdjustDTO)
}

// 导出结算单 xlsx：GET /settlement/export/{id}（后端 POI 生成，前端按二进制流接收）
export const exportApi = (id) => {
  return request.get(`/settlement/export/${id}`, { responseType: 'blob' })
}
