package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 饼图报表数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PieReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //名称，以逗号分隔，例如：行政助理,图书整理
    private String nameList;

    //数值，以逗号分隔，例如：3,5
    private String valueList;

}
