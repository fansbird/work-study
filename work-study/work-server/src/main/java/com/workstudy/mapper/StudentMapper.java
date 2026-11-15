package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.StudentPageQueryDTO;
import com.workstudy.entity.Student;
import com.workstudy.enumeration.OperationType;
import com.workstudy.vo.StudentVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudentMapper {

    /**
     * 根据id查询学生
     *
     * @param id
     * @return
     */
    @Select("select * from student where id = #{id}")
    Student getById(Long id);

    /**
     * 根据关联账号id查询学生
     *
     * @param userId
     * @return
     */
    @Select("select * from student where user_id = #{userId}")
    Student getByUserId(Long userId);

    /**
     * 查询全部启用的学生
     *
     * @return
     */
    List<Student> list();

    /**
     * 分页查询学生（含登录用户名）
     *
     * @param studentPageQueryDTO
     * @return
     */
    Page<StudentVO> pageQuery(StudentPageQueryDTO studentPageQueryDTO);

    /**
     * 新增学生
     *
     * @param student
     */
    @AutoFill(OperationType.INSERT)
    void insert(Student student);

    /**
     * 更新学生（动态sql）
     *
     * @param student
     */
    @AutoFill(OperationType.UPDATE)
    void update(Student student);

    /**
     * 根据id删除学生
     *
     * @param id
     */
    @Delete("delete from student where id = #{id}")
    void deleteById(Long id);

}
