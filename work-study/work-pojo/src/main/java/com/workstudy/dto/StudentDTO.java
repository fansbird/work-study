package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生档案新增/修改传递的数据模型
 */
@Data
public class StudentDTO implements Serializable {

    private Long id;

    //关联账号id
    private Long userId;

    //学号
    private String studentNo;

    //姓名
    private String name;

    //性别
    private String gender;

    //手机号
    private String phone;

    //学院
    private String college;

    //专业
    private String major;

    //班级
    private String className;

    //年级
    private String grade;

    //困难等级
    private Integer difficultyLevel;

    //银行卡号
    private String bankCard;

    //状态
    private Integer status;

}
