package com.workstudy.mapper;

import com.workstudy.entity.SettlementAdjust;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 结算调整数据访问层
 */
@Mapper
public interface SettlementAdjustMapper {

    /**
     * 新增结算调整记录
     *
     * @param settlementAdjust
     */
    void insert(SettlementAdjust settlementAdjust);

    /**
     * 查询某结算单的调整记录
     *
     * @param settlementId
     * @return
     */
    @Select("select * from settlement_adjust where settlement_id = #{settlementId} order by id asc")
    List<SettlementAdjust> listBySettlementId(Long settlementId);

}
