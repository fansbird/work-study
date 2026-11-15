package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 附件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //文件名
    private String fileName;

    //文件访问地址
    private String fileUrl;

    //文件大小（字节）
    private Long fileSize;

    //文件类型
    private String fileType;

    //业务类型
    private String bizType;

    //业务id
    private Long bizId;

    //上传人
    private Long uploadUser;

    private LocalDateTime createTime;

}
