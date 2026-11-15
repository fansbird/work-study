package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算调整传递的数据模型
 */
@Data
public class SettlementAdjustDTO implements Serializable {

    //结算单id
    private Long settlementId;

    //学生id
    private Long studentId;

    //调整类型 1补发 2扣减
    private Integer adjustType;

    //调整金额
    private BigDecimal amount;

    //调整原因
    private String reason;

}
