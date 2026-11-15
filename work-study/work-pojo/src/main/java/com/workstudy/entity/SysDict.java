package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysDict implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //字典名称
    private String dictName;

    //字典编码
    private String dictCode;

    //字典项文本
    private String itemText;

    //字典项值
    private String itemValue;

    //排序
    private Integer sort;

    //状态 1启用 0禁用
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
