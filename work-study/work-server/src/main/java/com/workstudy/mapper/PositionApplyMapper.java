package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.PositionApplyPageQueryDTO;
import com.workstudy.entity.PositionApply;
import com.workstudy.enumeration.OperationType;
import com.workstudy.vo.PositionApplyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PositionApplyMapper {

    /**
     * 新增报名（status=0 待审核）
     *
     * @param positionApply
     */
    @AutoFill(OperationType.INSERT)
    void insert(PositionApply positionApply);

    /**
     * 更新报名（动态sql：撤销报名、审核录用/驳回）
     *
     * @param positionApply
     */
    @AutoFill(OperationType.UPDATE)
    void update(PositionApply positionApply);

    /**
     * 重新报名：把已撤销(3)/已放弃(4)的记录复位为待审核(0)，
     * 同时清空历史驳回原因与审核信息（唯一键 uk_position_student 限制同一岗位同一学生只有一条记录，只能复位不能新增）
     *
     * @param positionApply
     */
    @AutoFill(OperationType.UPDATE)
    void resetApply(PositionApply positionApply);

    /**
     * 根据id查询报名
     *
     * @param id
     * @return
     */
    @Select("select * from position_apply where id = #{id}")
    PositionApply getById(Long id);

    /**
     * 根据岗位id与学生id查询报名（唯一键 uk_position_student）
     *
     * @param positionId
     * @param studentId
     * @return
     */
    @Select("select * from position_apply where position_id = #{positionId} and student_id = #{studentId}")
    PositionApply getByPositionAndStudent(@Param("positionId") Long positionId, @Param("studentId") Long studentId);

    /**
     * 分页查询报名（关联岗位名称/学生姓名与学号/岗位所属部门id）
     * 数据归属：deptId 不为空时按岗位所属部门过滤（部门管理员只能看本部门岗位下的报名）
     *
     * @param query
     * @param deptId 部门id，为空表示不限制部门
     * @return
     */
    Page<PositionApplyVO> pageQuery(@Param("query") PositionApplyPageQueryDTO query, @Param("deptId") Long deptId);

}
