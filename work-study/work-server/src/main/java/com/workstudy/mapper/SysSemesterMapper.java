package com.workstudy.mapper;

import com.workstudy.annotation.AutoFill;
import com.workstudy.entity.SysSemester;
import com.workstudy.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysSemesterMapper {

    /**
     * 根据id查询学期
     *
     * @param id
     * @return
     */
    @Select("select * from sys_semester where id = #{id}")
    SysSemester getById(Long id);

    /**
     * 查询学期列表
     *
     * @return
     */
    List<SysSemester> list();

    /**
     * 查询当前学期（status=1）
     *
     * @return
     */
    @Select("select * from sys_semester where status = 1 order by id desc limit 1")
    SysSemester getCurrent();

    /**
     * 将全部学期置为非当前学期
     */
    void clearCurrent();

    /**
     * 新增学期
     *
     * @param sysSemester
     */
    @AutoFill(OperationType.INSERT)
    void insert(SysSemester sysSemester);

    /**
     * 更新学期（动态sql）
     *
     * @param sysSemester
     */
    @AutoFill(OperationType.UPDATE)
    void update(SysSemester sysSemester);

    /**
     * 根据id删除学期
     *
     * @param id
     */
    @Delete("delete from sys_semester where id = #{id}")
    void deleteById(Long id);

}
