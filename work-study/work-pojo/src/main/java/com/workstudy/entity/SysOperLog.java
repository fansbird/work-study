package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //模块标题
    private String title;

    //方法名称
    private String method;

    //请求方式
    private String requestMethod;

    //请求url
    private String requestUrl;

    //请求参数
    private String requestParam;

    //操作人id
    private Long operUserId;

    //操作人姓名
    private String operUserName;

    //操作ip
    private String operIp;

    //操作时间
    private LocalDateTime operTime;

    //耗时（毫秒）
    private Long costTime;

    //状态 1正常 0异常
    private Integer status;

    //错误信息
    private String errorMsg;

}
