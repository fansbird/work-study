/**
 * 角色码与角色文案（与后端 sys_user.role、JWT claim role 严格一致）
 *   0 超级管理员 / 1 学生 / 2 用工部门管理员 / 3 勤工助学中心管理员 / 4 财务
 * 后续所有页面（账号管理、岗位、工时、结算…）统一从这里取角色选项，避免各处硬编码。
 */

export const ROLE = {
  SUPER_ADMIN: 0,
  STUDENT: 1,
  DEPT_ADMIN: 2,
  CENTER_ADMIN: 3,
  FINANCE: 4
}

/**
 * 角色下拉选项（tagType 对应 el-tag 的 type）
 * 说明：只使用 el-tag 在各版本都合法的取值（success/info/warning/danger），避免出现
 *       "取值不在允许列表内" 的 props 校验告警。
 */
export const ROLE_OPTIONS = [
  { value: 0, label: '超级管理员', tagType: 'danger' },
  { value: 1, label: '学生', tagType: 'success' },
  { value: 2, label: '用工部门管理员', tagType: 'warning' },
  { value: 3, label: '勤工助学中心管理员', tagType: 'info' },
  { value: 4, label: '财务', tagType: 'success' }
]

/**
 * 角色码 → 中文名称
 * @param {Number|String} role 角色码
 */
export const roleLabel = (role) => {
  const item = ROLE_OPTIONS.find((option) => option.value === Number(role))
  return item ? item.label : '未知角色'
}

/**
 * 角色码 → el-tag 的 type
 * @param {Number|String} role 角色码
 */
export const roleTagType = (role) => {
  const item = ROLE_OPTIONS.find((option) => option.value === Number(role))
  return item ? item.tagType : 'info'
}
