<!--
  个人中心
  ====================================================================
  路由：/profile/index    可见角色：全部角色(0/1/2/3/4)
  契约依据：第 5 节 SysUserController（/sysUser）、StudentController（/student）、CommonController（/common）与
            AttachmentController（/attachment）、第 3 节 sys_user 表、契约 6.12 OSS 文件

  功能：基本信息展示 + 修改姓名/手机号/头像（头像走 api/common.js 上传）。
  接口对齐：
    GET  /sysUser/{id}                 取当前登录账号的最新信息（id 来自 localStorage 里的 LoginVO）
    PUT  /sysUser                      修改 SysUserDTO{id,username,name,phone,role,deptId,studentId,avatar,status}
    GET  /student/getByUserId/{userId} 学生角色额外展示自己的学生档案（只读）
    GET  /dept/list                    部门管理员的"所属部门"名称映射
    POST /common/upload                上传头像（api/common.js 的 uploadApi）
    POST /attachment                   上传成功后保存附件元数据（bizType=avatar，bizId=当前账号 id）
    GET  /dict/list?dictCode=difficulty_level  学生档案里困难等级的中文映射

  说明：PUT /sysUser 走的是动态 SQL（非空字段才更新），这里仍把从 GET 拿到的完整 DTO 回传，
        避免任何字段被意外清空；password 不在 SysUserDTO 里，不会被带出去。
  ====================================================================
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getByIdApi, updateApi } from '@/api/user'
import { listApi as deptListApi } from '@/api/dept'
import { getByUserIdApi } from '@/api/student'
import { listApi as dictListApi } from '@/api/dict'
import { uploadApi, addAttachmentApi } from '@/api/common'
import { getLoginUser } from '@/router'
import { ROLE, roleLabel, roleTagType } from '@/utils/role'

// ==================== 当前登录人 ====================
const loginUser = getLoginUser() || {}
const userId = loginUser.id
const role = Number(loginUser.role)
const isStudent = role === ROLE.STUDENT
const isDeptAdmin = role === ROLE.DEPT_ADMIN

// ==================== 基本信息表单（SysUserDTO） ====================
// role / name / avatar 先用登录信息兜底，避免首屏短暂显示错误的角色文案
const userForm = ref({
  id: null,
  username: loginUser.username || '',
  name: loginUser.name || '',
  phone: '',
  role: loginUser.role === undefined || loginUser.role === null ? null : Number(loginUser.role),
  deptId: null,
  studentId: null,
  avatar: loginUser.avatar || '',
  status: 1
})

// 角色是否有效：roleLabel/roleTagType 内部用 Number(role) 换算，null 会被当成 0（超级管理员），
// 所以展示角色标签前必须先判断 role 是否有值
const hasRole = computed(
  () => userForm.value.role !== null && userForm.value.role !== undefined && userForm.value.role !== ''
)
const formRef = ref()

const rules = ref({
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名不能超过50个字符', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }]
})

// 只挑 SysUserDTO 需要的字段，避免把 password 等无关字段回传
const pickUserDto = (data) => ({
  id: data.id,
  username: data.username,
  name: data.name,
  phone: data.phone,
  role: data.role,
  deptId: data.deptId,
  studentId: data.studentId,
  avatar: data.avatar,
  status: data.status
})

// ==================== 辅助数据 ====================
const deptList = ref([])
const student = ref(null)
const avatarUploading = ref(false)

// 所属部门名称（部门管理员）
const deptName = computed(() => {
  if (!userForm.value.deptId) return '--'
  const item = deptList.value.find((dept) => dept.id === userForm.value.deptId)
  return item ? item.name : '部门ID：' + userForm.value.deptId
})

// 困难等级（契约 3 student.difficulty_level：0 非困难 / 1 一般困难 / 2 特别困难）
const FALLBACK_DIFFICULTY = ['非困难', '一般困难', '特别困难']
const difficultyText = computed(() => {
  if (!student.value) return '--'
  const level = Number(student.value.difficultyLevel)
  if (Number.isNaN(level)) return '--'
  return FALLBACK_DIFFICULTY[level] || '--'
})

// ==================== 钩子函数 ====================
onMounted(async () => {
  loadUserInfo()
  if (isStudent) {
    // 先拿档案再查字典：困难等级文案要按档案里的等级去字典里匹配
    await loadStudentInfo()
    loadDifficultyOptions()
  }
  if (isDeptAdmin) {
    loadDeptList()
  }
})

// ==================== 基本信息 ====================
const loadUserInfo = async () => {
  if (!userId) {
    ElMessage.error('未获取到登录信息，请重新登录')
    return
  }
  try {
    const res = await getByIdApi(userId)
    if (res.code && res.data) {
      userForm.value = { ...userForm.value, ...pickUserDto(res.data) }
    } else {
      ElMessage.error(res.msg || '查询账号信息失败')
    }
  } catch (e) {
    ElMessage.error('查询账号信息失败')
  }
}

// ==================== 学生档案（只读） ====================
const loadStudentInfo = async () => {
  try {
    const res = await getByUserIdApi(userId)
    if (res.code) {
      student.value = res.data || null
    }
  } catch (e) {
    console.warn('[个人中心] 学生档案加载失败', e)
  }
}

// 困难等级中文以数据字典为准，拉取失败时用内置兜底（computed 里已内置换算）
const difficultyFromDict = ref('')
const loadDifficultyOptions = async () => {
  try {
    const res = await dictListApi('difficulty_level')
    if (res.code && res.data && res.data.length > 0 && student.value) {
      const hit = res.data.find((item) => Number(item.itemValue) === Number(student.value.difficultyLevel))
      if (hit) {
        difficultyFromDict.value = hit.itemText
      }
    }
  } catch (e) {
    console.warn('[个人中心] 困难等级字典加载失败，使用内置文案', e)
  }
}
// 优先使用字典文案
const difficultyLabel = computed(() => difficultyFromDict.value || difficultyText.value)

// ==================== 部门（部门管理员） ====================
const loadDeptList = async () => {
  try {
    const res = await deptListApi()
    if (res.code) {
      deptList.value = res.data || []
    }
  } catch (e) {
    console.warn('[个人中心] 部门列表加载失败', e)
  }
}

// ==================== 头像上传 ====================
// 上传前校验：仅 jpg/png，且不超过 5MB
const beforeAvatarUpload = (file) => {
  if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
    ElMessage.error('头像只支持 jpg/png 格式的图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('头像大小不能超过 5MB')
    return false
  }
  return true
}

// el-upload 自定义上传：走 api/common.js 的 uploadApi（FormData + multipart/form-data）
// 契约 6.12：上传成功后由前端调用 attachment 接口保存元数据
const handleAvatarRequest = async (options) => {
  const file = options.file
  if (!beforeAvatarUpload(file)) {
    return
  }
  avatarUploading.value = true
  try {
    const res = await uploadApi(file)
    if (!res.code) {
      ElMessage.error(res.msg || '头像上传失败')
      return
    }
    userForm.value.avatar = res.data
    // 附件元数据落库失败不阻塞头像使用，按可降级处理
    try {
      const attachRes = await addAttachmentApi({
        fileName: file.name,
        fileUrl: res.data,
        fileSize: file.size,
        fileType: file.type,
        bizType: 'avatar',
        bizId: userId,
        uploadUser: userId
      })
      if (!attachRes.code) {
        console.warn('[个人中心] 附件元数据保存失败：' + (attachRes.msg || ''))
      }
    } catch (e) {
      console.warn('[个人中心] 附件元数据保存失败', e)
    }
    ElMessage.success('头像已上传，点击「保存」后生效')
  } catch (e) {
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

// ==================== 保存 ====================
const save = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表单校验不通过')
      return
    }
    try {
      const res = await updateApi(userForm.value)
      if (res.code) {
        // 顶部栏的用户名/头像读取自 localStorage 的登录信息（LoginVO），同步一下
        const loginInfo = getLoginUser() || {}
        localStorage.setItem(
          'token',
          JSON.stringify({ ...loginInfo, name: userForm.value.name, avatar: userForm.value.avatar })
        )
        ElMessage.success('保存成功')
        // 顶部栏在 Layout 初始化时读取了一次登录信息，刷新页面让展示同步
        setTimeout(() => window.location.reload(), 600)
      } else {
        ElMessage.error(res.msg || '保存失败')
      }
    } catch (e) {
      ElMessage.error('保存失败')
    }
  })
}
</script>

<template>
  <div class="profile-page">
    <!-- ==================== 头像 + 身份概要 ==================== -->
    <div class="page-card profile-head">
      <el-upload
        class="avatar-uploader"
        action="#"
        accept="image/png,image/jpeg"
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
        :http-request="handleAvatarRequest"
      >
        <img v-if="userForm.avatar" :src="userForm.avatar" class="avatar" alt="头像" />
        <div v-else class="avatar avatar-empty">
          <el-icon><Plus /></el-icon>
        </div>
        <div v-if="avatarUploading" class="avatar-loading">上传中…</div>
      </el-upload>
      <div class="head-info">
        <div class="head-name">{{ userForm.name || userForm.username || '未登录' }}</div>
        <div class="head-tags">
          <el-tag v-if="hasRole" :type="roleTagType(userForm.role)" size="small">{{ roleLabel(userForm.role) }}</el-tag>
          <span v-else class="head-tip">--</span>
          <el-tag v-if="userForm.status === 0" type="info" size="small">账号已禁用</el-tag>
        </div>
        <div class="head-tip">点击左侧头像可上传新头像（支持 jpg/png，5MB 以内），保存后生效</div>
      </div>
    </div>

    <!-- ==================== 基本信息（可修改姓名 / 手机号 / 头像） ==================== -->
    <div class="page-card">
      <div class="panel-title">基本信息</div>
      <el-form :model="userForm" :rules="rules" ref="formRef" label-width="100px" class="info-form">
        <el-form-item label="登录账号">
          <!-- 登录账号即用户名，不允许本人修改 -->
          <el-input :model-value="userForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag v-if="hasRole" :type="roleTagType(userForm.role)" size="small">{{ roleLabel(userForm.role) }}</el-tag>
          <span v-else>--</span>
        </el-form-item>
        <el-form-item v-if="isDeptAdmin" label="所属部门">
          <span>{{ deptName }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
          <el-button @click="loadUserInfo">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ==================== 学生档案（学生角色只读展示） ==================== -->
    <div v-if="isStudent" class="page-card">
      <div class="panel-title">我的学生档案（只读，如需修改请联系勤工助学中心）</div>
      <el-empty v-if="!student" description="暂无学生档案数据" :image-size="80" />
      <el-descriptions v-else :column="3" border>
        <el-descriptions-item label="学号">{{ student.studentNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ student.name || '--' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ student.gender || '--' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ student.phone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ student.college || '--' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ student.major || '--' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ student.className || '--' }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ student.grade || '--' }}</el-descriptions-item>
        <el-descriptions-item label="困难等级">{{ difficultyLabel }}</el-descriptions-item>
        <el-descriptions-item label="银行卡号" :span="3">{{ student.bankCard || '--' }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 头像 + 身份概要 ==================== */
.profile-head {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
  flex: none;
}

.avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.avatar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: #8c939d;
  background-color: #f5f7fa;
  border: 1px dashed #d9d9d9;
}

.avatar-loading {
  position: absolute;
  left: 0;
  top: 0;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.45);
  color: #ffffff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.head-name {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.head-tags {
  margin: 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-tip {
  font-size: 12px;
  color: #909399;
}

/* ==================== 表单 / 区块标题 ==================== */
.info-form {
  max-width: 520px;
}

.panel-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16px;
}
</style>
