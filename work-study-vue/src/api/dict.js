import request from '@/utils/request'

/**
 * 数据字典（契约第 5 节 DictController，根路径 /dict）
 */

// 按字典编码查询字典项：GET /dict/list?dictCode=  （dictCode 可不传，表示查全部）
export const listApi = (dictCode) => {
  return request.get('/dict/list', { params: { dictCode } })
}

// 新增字典项：POST /dict，入参 SysDictDTO{id,dictName,dictCode,itemText,itemValue,sort,status}
export const addApi = (dict) => {
  return request.post('/dict', dict)
}

// 修改字典项：PUT /dict，入参 SysDictDTO
export const updateApi = (dict) => {
  return request.put('/dict', dict)
}

// 删除字典项：DELETE /dict/{id}
export const deleteApi = (id) => {
  return request.delete(`/dict/${id}`)
}
