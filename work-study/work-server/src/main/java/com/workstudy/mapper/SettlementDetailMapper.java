package com.workstudy.mapper;

import com.workstudy.entity.SettlementDetail;
import com.workstudy.vo.SettlementDetailVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 结算明细数据访问层
 */
@Mapper
public interface SettlementDetailMapper {

    /**
     * 批量新增结算明细
     *
     * @param details
     */
    void insertBatch(@Param("details") List<SettlementDetail> details);

    /**
     * 查询某结算单的明细（含学生姓名/学号、岗位名称）
     *
     * @param settlementId
     * @return
     */
    List<SettlementDetailVO> listBySettlementId(Long settlementId);

    /**
     * 对账用：从数据库汇总某结算单的明细金额/工时/条数
     *
     * @param settlementId
     * @return keys: totalAmount(BigDecimal)、totalHours(BigDecimal)、detailCount(Long)
     */
    Map<String, Object> sumBySettlementId(Long settlementId);

    /**
     * 统计某结算单中是否含有指定学生的明细（学生数据归属校验）
     *
     * @param settlementId
     * @param studentId
     * @return
     */
    int countBySettlementIdAndStudentId(@Param("settlementId") Long settlementId,
                                        @Param("studentId") Long studentId);

    /**
     * 删除某结算单的明细（结算单作废时释放 uk_work_hour_id，保证可重算）
     *
     * @param settlementId
     */
    @Delete("delete from settlement_detail where settlement_id = #{settlementId}")
    void deleteBySettlementId(Long settlementId);

}
