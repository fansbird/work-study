package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算调整
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementAdjust implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

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

    private Long createUser;

    private LocalDateTime createTime;

}
