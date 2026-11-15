package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生档案信息（含登录用户名）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentVO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

    //登录用户名
    private String username;

}
