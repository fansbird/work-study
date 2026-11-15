package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.SettlementPageQueryDTO;
import com.workstudy.entity.Position;
import com.workstudy.entity.Settlement;
import com.workstudy.entity.WorkHour;
import com.workstudy.enumeration.OperationType;
import com.workstudy.vo.SettlementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 结算单数据访问层（M5 薪酬结算）
 * <p>其中对 work_hour 的读取与回填、对 position 的读取，均按结算场景定制，避免与其他模块的 Mapper 交叉。</p>
 */
@Mapper
public interface SettlementMapper {

    /**
     * 幂等校验：统计同一「学期 + 月份 + 部门」下未作废的结算批次数量
     *
     * @param semesterId
     * @param month
     * @param deptId     可为空（空=全校）
     * @return
     */
    int countByScope(@Param("semesterId") Long semesterId,
                     @Param("month") String month,
                     @Param("deptId") Long deptId);

    /**
     * 统计批次号前缀已使用的数量（用于生成序号）
     *
     * @param prefix
     * @return
     */
    int countByBatchPrefix(@Param("prefix") String prefix);

    /**
     * 根据批次号查询结算单
     *
     * @param batchNo
     * @return
     */
    @Select("select * from settlement where batch_no = #{batchNo}")
    Settlement getByBatchNo(String batchNo);

    /**
     * 根据id查询结算单
     *
     * @param id
     * @return
     */
    @Select("select * from settlement where id = #{id}")
    Settlement getById(Long id);

    /**
     * 新增结算单
     *
     * @param settlement
     */
    @AutoFill(OperationType.INSERT)
    void insert(Settlement settlement);

    /**
     * 更新结算单（动态sql）
     *
     * @param settlement
     */
    @AutoFill(OperationType.UPDATE)
    void update(Settlement settlement);

    /**
     * 分页查询结算单（含部门名称、学期名称）
     *
     * @param settlementPageQueryDTO
     * @return
     */
    Page<SettlementVO> pageQuery(SettlementPageQueryDTO settlementPageQueryDTO);

    /**
     * 学生视角分页：仅返回「明细中包含该学生」的结算单（数据归属，支撑「我的薪酬」）
     *
     * @param settlementPageQueryDTO
     * @param studentId
     * @return
     */
    Page<SettlementVO> pageQueryForStudent(@Param("query") SettlementPageQueryDTO settlementPageQueryDTO,
                                           @Param("studentId") Long studentId);

    /**
     * 根据id查询结算单详情（含部门名称、学期名称）
     *
     * @param id
     * @return
     */
    SettlementVO getVoById(Long id);

    /**
     * 根据id查询岗位（用于取 category、hourly_wage、dept_id）
     *
     * @param id
     * @return
     */
    @Select("select * from position where id = #{id}")
    Position getPositionById(Long id);

    /**
     * 查询可结算工时：指定学期、指定月份区间内，status=2（已通过）且 settlement_id 为空
     *
     * @param semesterId
     * @param startDate
     * @param endDate
     * @param deptId     可为空（空=全校）
     * @return
     */
    List<WorkHour> listPendingWorkHours(@Param("semesterId") Long semesterId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("deptId") Long deptId);

    /**
     * 结算后回填 work_hour.settlement_id（锁定）
     *
     * @param settlementId
     * @param ids
     */
    void fillSettlementId(@Param("settlementId") Long settlementId, @Param("ids") List<Long> ids);

    /**
     * 结算单作废时解绑其下全部工时的 settlement_id，以便重新结算
     *
     * @param settlementId
     */
    void clearSettlementIdBySettlementId(@Param("settlementId") Long settlementId);

}
