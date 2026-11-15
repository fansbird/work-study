import request from '@/utils/request'

/**
 * 公共接口（契约第 5 节）
 *   CommonController     根路径 /common     —— 文件上传
 *   AttachmentController 根路径 /attachment —— 附件元数据的保存 / 查询 / 删除
 *
 * 供各业务分片复用的用法说明：
 *   1) 上传与"保存附件元数据"是两步：先 uploadApi(file) 拿到文件访问地址，
 *      再 addAttachmentApi({ ... }) 把元数据落库（契约 6.12：上传后由前端调用 attachment 保存元数据）；
 *      需要一步到位时直接用 uploadAndSaveAttachmentApi(file, bizType, bizId)。
 *   2) 所有函数返回 Result{code,msg,data}（已由 utils/request 的响应拦截器解包成 response.data）。
 *   3) 上传接口返回 Result<String>，data 即文件访问地址，直接写进业务对象的 url 字段即可。
 */

/* ==================== 文件上传 ==================== */

/**
 * 上传文件：POST /common/upload
 * @param {File} file 浏览器 File 对象（el-upload 里取 options.file / file.raw）
 * @returns {Promise<Result<String>>} data = 文件访问地址
 */
export const uploadApi = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/common/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 语义化别名，与 uploadApi 完全等价（统一写法时可直接 import { upload }） */
export const upload = uploadApi

/* ==================== 附件元数据 ==================== */

/**
 * 保存附件元数据：POST /attachment
 * @param {Object} attachment Attachment{fileName,fileUrl,fileSize,fileType,bizType,bizId,uploadUser}
 *   bizType 业务类型（如 avatar / workhour / position），bizId 业务主键
 */
export const addAttachmentApi = (attachment) => {
  return request.post('/attachment', attachment)
}

/**
 * 查询业务附件列表：GET /attachment/list?bizType=&bizId=
 * @param {Object} params { bizType, bizId }，两者可只传其一
 */
export const listAttachmentApi = (params) => {
  return request.get('/attachment/list', { params })
}

/**
 * 查询业务附件列表（位置参数写法，等价于 listAttachmentApi({ bizType, bizId })）
 * @param {String} bizType 业务类型
 * @param {Number|String} bizId 业务主键
 */
export const listAttachmentByBizApi = (bizType, bizId) => {
  return request.get('/attachment/list', { params: { bizType, bizId } })
}

/**
 * 删除附件：DELETE /attachment/{id}
 * @param {Number|String} id 附件 id
 */
export const deleteAttachmentApi = (id) => {
  return request.delete(`/attachment/${id}`)
}

/* ==================== 上传 + 落库（便捷组合） ==================== */

/**
 * 上传文件并保存附件元数据：先 POST /common/upload，再 POST /attachment
 * 说明：文件名/大小/类型直接取浏览器 File 对象自带属性；create_time 由数据库默认值负责，这里不传。
 * @param {File} file 浏览器 File 对象
 * @param {String} bizType 业务类型（如 avatar）
 * @param {Number|String} bizId 业务主键（如账号 id）
 * @param {Number|String} uploadUser 上传人（一般为当前登录账号 id）
 * @returns {Promise<{fileUrl:String, attachment:Object}>} 上传后的文件地址与附件元数据
 */
export const uploadAndSaveAttachmentApi = async (file, bizType, bizId, uploadUser) => {
  const uploadRes = await uploadApi(file)
  if (!uploadRes || !uploadRes.code) {
    throw new Error((uploadRes && uploadRes.msg) || '文件上传失败')
  }
  const fileUrl = uploadRes.data
  const attachment = {
    fileName: file.name,
    fileUrl,
    fileSize: file.size,
    fileType: file.type,
    bizType,
    bizId,
    uploadUser
  }
  const saveRes = await addAttachmentApi(attachment)
  if (!saveRes || !saveRes.code) {
    throw new Error((saveRes && saveRes.msg) || '附件元数据保存失败')
  }
  return { fileUrl, attachment }
}
