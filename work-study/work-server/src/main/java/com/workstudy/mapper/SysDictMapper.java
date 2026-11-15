package com.workstudy.mapper;

import com.workstudy.annotation.AutoFill;
import com.workstudy.entity.SysDict;
import com.workstudy.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDictMapper {

    /**
     * 根据id查询字典项
     *
     * @param id
     * @return
     */
    @Select("select * from sys_dict where id = #{id}")
    SysDict getById(Long id);

    /**
     * 根据字典编码查询字典项（dictCode 为空时查询全部）
     *
     * @param dictCode
     * @return
     */
    List<SysDict> list(@Param("dictCode") String dictCode);

    /**
     * 新增字典项
     *
     * @param sysDict
     */
    @AutoFill(OperationType.INSERT)
    void insert(SysDict sysDict);

    /**
     * 更新字典项（动态sql）
     *
     * @param sysDict
     */
    @AutoFill(OperationType.UPDATE)
    void update(SysDict sysDict);

    /**
     * 根据id删除字典项
     *
     * @param id
     */
    @Delete("delete from sys_dict where id = #{id}")
    void deleteById(Long id);

}
