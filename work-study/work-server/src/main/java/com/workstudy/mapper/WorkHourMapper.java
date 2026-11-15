package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.WorkHourPageQueryDTO;
import com.workstudy.entity.Position;
import com.workstudy.entity.WorkHour;
import com.workstudy.enumeration.OperationType;
import com.workstudy.vo.WorkHourVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 工时数据访问层（M4 工时管理）
 */
@Mapper
public interface WorkHourMapper {

    /**
     * 根据id查询工时
     *
     * @param id
     * @return
     */
    @Select("select * from work_hour where id = #{id}")
    WorkHour getById(Long id);

    /**
     * 根据id查询工时详情（含岗位名称、学生姓名/学号、部门名称）
     *
     * @param id
     * @return
     */
    WorkHourVO getVoById(Long id);

    /**
     * 根据id查询岗位（用于带出 dept_id/semester_id 与录用校验）
     *
     * @param id
     * @return
     */
    @Select("select * from position where id = #{id}")
    Position getPositionById(Long id);

    /**
     * 统计该学生在该岗位的已录用报名数（status=1 已录用）
     *
     * @param positionId
     * @param studentId
     * @return
     */
    @Select("select count(1) from position_apply where position_id = #{positionId} and student_id = #{studentId} and status = 1")
    int countHiredApply(@Param("positionId") Long positionId, @Param("studentId") Long studentId);

    /**
     * 统计同一学生同一日期与该时段重叠的工时数（已有记录 status≠3 驳回）
     * 重叠判定：start &lt; exist.end AND end &gt; exist.start
     *
     * @param studentId
     * @param workDate
     * @param startTime
     * @param endTime
     * @param excludeId 修改时排除自身，新增传 null
     * @return
     */
    int countOverlap(@Param("studentId") Long studentId,
                     @Param("workDate") LocalDate workDate,
                     @Param("startTime") LocalTime startTime,
                     @Param("endTime") LocalTime endTime,
                     @Param("excludeId") Long excludeId);

    /**
     * 统计同一学生在指定日期区间内的有效工时合计（status≠3），用于日/周/月上限聚合校验
     *
     * @param studentId
     * @param startDate
     * @param endDate
     * @param excludeId 修改时排除自身，新增传 null
     * @return
     */
    BigDecimal sumHours(@Param("studentId") Long studentId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("excludeId") Long excludeId);

    /**
     * 分页查询工时（含岗位/学生/部门名称）
     *
     * @param workHourPageQueryDTO
     * @return
     */
    Page<WorkHourVO> pageQuery(WorkHourPageQueryDTO workHourPageQueryDTO);

    /**
     * 新增工时
     *
     * @param workHour
     */
    @AutoFill(OperationType.INSERT)
    void insert(WorkHour workHour);

    /**
     * 更新工时（驳回重填修改，动态sql）
     *
     * @param workHour
     */
    @AutoFill(OperationType.UPDATE)
    void update(WorkHour workHour);

    /**
     * 审核更新（部门审核/中心复核：status、reject_reason 与审核人/审核时间）
     *
     * @param workHour
     */
    @AutoFill(OperationType.UPDATE)
    void updateAudit(WorkHour workHour);

    /**
     * 驳回后重新提交（3→0，清空驳回原因）
     *
     * @param workHour
     */
    @AutoFill(OperationType.UPDATE)
    void submit(WorkHour workHour);

    /**
     * 根据id删除工时
     *
     * @param id
     */
    @Delete("delete from work_hour where id = #{id}")
    void deleteById(Long id);

}
