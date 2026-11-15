package com.workstudy.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.constant.PasswordConstant;
import com.workstudy.constant.StatusConstant;
import com.workstudy.dto.StudentDTO;
import com.workstudy.dto.StudentPageQueryDTO;
import com.workstudy.entity.Student;
import com.workstudy.entity.SysUser;
import com.workstudy.mapper.StudentMapper;
import com.workstudy.mapper.SysUserMapper;
import com.workstudy.result.PageResult;
import com.workstudy.service.DataScopeService;
import com.workstudy.service.StudentService;
import com.workstudy.vo.StudentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 新增学生（同时联动创建学生登录账号：username=学号，role=1，初始密码 123456）
     *
     * @param studentDTO
     */
    @Override
    @Transactional
    public void save(StudentDTO studentDTO) {
        Student student = new Student();
        BeanUtils.copyProperties(studentDTO, student);
        //账号id由系统联动创建，忽略外部传入值
        student.setUserId(null);
        if (student.getStatus() == null) {
            student.setStatus(StatusConstant.ENABLE);
        }
        if (student.getDifficultyLevel() == null) {
            student.setDifficultyLevel(0);
        }
        //1、先落学生档案
        studentMapper.insert(student);

        //2、联动创建学生登录账号
        SysUser sysUser = new SysUser();
        sysUser.setUsername(student.getStudentNo());
        sysUser.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes(StandardCharsets.UTF_8)));
        sysUser.setName(student.getName());
        sysUser.setPhone(student.getPhone());
        sysUser.setRole(DataScopeService.ROLE_STUDENT);
        sysUser.setStatus(StatusConstant.ENABLE);
        sysUserMapper.insert(sysUser);

        //3、回填学生档案的关联账号id
        Student updateStudent = new Student();
        updateStudent.setId(student.getId());
        updateStudent.setUserId(sysUser.getId());
        studentMapper.update(updateStudent);

        //4、保险：同步回填 sys_user.student_id，使账号侧也能直接关联到学生档案（与 student.user_id 双向一致）
        SysUser accountOfStudent = new SysUser();
        accountOfStudent.setId(sysUser.getId());
        accountOfStudent.setStudentId(student.getId());
        sysUserMapper.update(accountOfStudent);

        log.info("新增学生成功，学号：{}，已联动创建登录账号id：{}", student.getStudentNo(), sysUser.getId());
    }

    /**
     * 更新学生档案
     *
     * @param studentDTO
     */
    @Override
    @Transactional
    public void update(StudentDTO studentDTO) {
        Student student = new Student();
        BeanUtils.copyProperties(studentDTO, student);
        //关联账号id由系统维护，不允许通过修改接口重定向
        student.setUserId(null);
        studentMapper.update(student);

        //保险：若该学生已有登录账号，保证 sys_user.student_id 与本学生档案一致
        syncUserStudentId(student.getId());
    }

    /**
     * 保险同步：保证 sys_user.student_id 与 student.id 双向一致。
     * 学生档案存在且已关联账号时，将账号的 student_id 回填为本学生id。
     *
     * @param studentId
     */
    private void syncUserStudentId(Long studentId) {
        if (studentId == null) {
            return;
        }
        Student current = studentMapper.getById(studentId);
        if (current == null || current.getUserId() == null) {
            return;
        }
        SysUser sysUser = new SysUser();
        sysUser.setId(current.getUserId());
        sysUser.setStudentId(current.getId());
        sysUserMapper.update(sysUser);
    }

    /**
     * 分页查询学生（含登录用户名）
     *
     * @param studentPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(StudentPageQueryDTO studentPageQueryDTO) {
        PageHelper.startPage(studentPageQueryDTO.getPage(), studentPageQueryDTO.getPageSize());
        Page<StudentVO> page = studentMapper.pageQuery(studentPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 查询启用中的学生列表
     *
     * @return
     */
    @Override
    public List<Student> list() {
        return studentMapper.list();
    }

    /**
     * 根据id查询学生
     *
     * @param id
     * @return
     */
    @Override
    public Student getById(Long id) {
        return studentMapper.getById(id);
    }

    /**
     * 根据关联账号id查询学生
     *
     * @param userId
     * @return
     */
    @Override
    public Student getByUserId(Long userId) {
        return studentMapper.getByUserId(userId);
    }

    /**
     * 根据id删除学生
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        studentMapper.deleteById(id);
    }
}
