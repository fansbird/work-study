package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.PositionPageQueryDTO;
import com.workstudy.entity.Position;
import com.workstudy.enumeration.OperationType;
import com.workstudy.vo.PositionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PositionMapper {

    /**
     * 新增岗位（默认落草稿，由 Service 置状态）
     *
     * @param position
     */
    @AutoFill(OperationType.INSERT)
    void insert(Position position);

    /**
     * 更新岗位（动态sql，仅用于草稿/已驳回的修改，不写状态字段，状态只能走状态机流转）
     *
     * @param position
     */
    @AutoFill(OperationType.UPDATE)
    void update(Position position);

    /**
     * 岗位状态流转（提交审批/审批通过/审批驳回/下架）：
     * 显式写 status 与 reject_reason，因此审批通过、重新提交时可以清空历史驳回原因
     *
     * @param position
     */
    @AutoFill(OperationType.UPDATE)
    void updateStatus(Position position);

    /**
     * 根据id查询岗位
     *
     * @param id
     * @return
     */
    @Select("select * from position where id = #{id}")
    Position getById(Long id);

    /**
     * 根据id查询岗位详情（含部门名称、类别名称、剩余名额）
     *
     * @param id
     * @return
     */
    PositionVO getVOById(Long id);

    /**
     * 分页查询岗位（含部门名称、类别名称、剩余名额）
     *
     * @param positionPageQueryDTO
     * @return
     */
    Page<PositionVO> pageQuery(PositionPageQueryDTO positionPageQueryDTO);

    /**
     * 查询已发布岗位列表（供学生浏览/下拉，含部门名称、类别名称、剩余名额）
     *
     * @param positionPageQueryDTO
     * @return
     */
    List<PositionVO> list(PositionPageQueryDTO positionPageQueryDTO);

    /**
     * 根据id删除岗位
     *
     * @param id
     */
    void deleteById(Long id);

    /**
     * 名额占用：已录用人数 +1
     * 条件更新（乐观思想），仅当 hired < quota 时才占用成功；
     * 返回影响行数，=0 即表示名额已满，由 Service 抛业务异常
     *
     * @param id
     * @return 影响行数
     */
    @Update("update position set hired = hired + 1 where id = #{id} and hired < quota")
    int incrHired(Long id);

    /**
     * 名额释放：已录用人数 -1
     * 条件更新，仅当 hired > 0 时才释放成功
     *
     * @param id
     * @return 影响行数
     */
    @Update("update position set hired = hired - 1 where id = #{id} and hired > 0")
    int decrHired(Long id);

}
