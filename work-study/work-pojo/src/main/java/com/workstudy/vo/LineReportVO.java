package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 折线图报表数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //日期，以逗号分隔，例如：2026-10-01,2026-10-02
    private String dateList;

    //数值，以逗号分隔，例如：8.0,16.0
    private String valueList;

}
