package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.DeptPageQueryDTO;
import com.workstudy.entity.Dept;
import com.workstudy.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptMapper {

    /**
     * 根据id查询部门
     *
     * @param id
     * @return
     */
    @Select("select * from dept where id = #{id}")
    Dept getById(Long id);

    /**
     * 查询全部启用的部门
     *
     * @return
     */
    List<Dept> list();

    /**
     * 分页查询部门
     *
     * @param deptPageQueryDTO
     * @return
     */
    Page<Dept> pageQuery(DeptPageQueryDTO deptPageQueryDTO);

    /**
     * 新增部门
     *
     * @param dept
     */
    @AutoFill(OperationType.INSERT)
    void insert(Dept dept);

    /**
     * 更新部门（动态sql）
     *
     * @param dept
     */
    @AutoFill(OperationType.UPDATE)
    void update(Dept dept);

    /**
     * 根据id删除部门
     *
     * @param id
     */
    @Delete("delete from dept where id = #{id}")
    void deleteById(Long id);

}
