package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典新增/修改传递的数据模型
 */
@Data
public class SysDictDTO implements Serializable {

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

    //状态
    private Integer status;

}
