package com.workstudy.service;

import com.workstudy.dto.SettlementAdjustDTO;
import com.workstudy.dto.SettlementGenerateDTO;
import com.workstudy.dto.SettlementPageQueryDTO;
import com.workstudy.entity.Settlement;
import com.workstudy.result.PageResult;
import com.workstudy.vo.SettlementDetailVO;
import com.workstudy.vo.SettlementVO;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 薪酬结算服务（M5：BigDecimal 对账 + 批次幂等 + 发放锁定）
 */
public interface SettlementService {

    /**
     * 生成月度结算批次（批处理 + 规则匹配 + 封顶保底 + 对账 + 幂等）
     *
     * @param settlementGenerateDTO
     * @return 生成的结算单
     */
    Settlement generate(SettlementGenerateDTO settlementGenerateDTO);

    /**
     * 分页查询结算单（部门管理员限本部门，学生仅可看含本人明细的结算单）
     *
     * @param settlementPageQueryDTO
     * @return
     */
    PageResult pageQuery(SettlementPageQueryDTO settlementPageQueryDTO);

    /**
     * 根据id查询结算单
     *
     * @param id
     * @return
     */
    SettlementVO getById(Long id);

    /**
     * 查询结算明细
     *
     * @param settlementId
     * @return
     */
    List<SettlementDetailVO> listDetail(Long settlementId);

    /**
     * 确认结算单（0 → 1，中心）
     *
     * @param id
     */
    void confirm(Long id);

    /**
     * 发放结算单（1 → 2，财务，发放后锁定）
     *
     * @param id
     */
    void pay(Long id);

    /**
     * 作废结算单（0/1 → 3，并解绑其下工时的 settlement_id 以便重算）
     *
     * @param id
     */
    void voidSettlement(Long id);

    /**
     * 结算调整（补发/扣减，写入 settlement_adjust 并调整结算单总额）
     *
     * @param settlementAdjustDTO
     */
    void adjust(SettlementAdjustDTO settlementAdjustDTO);

    /**
     * 导出结算明细（Apache POI，xlsx）
     *
     * @param id
     * @param response
     */
    void export(Long id, HttpServletResponse response);

}
