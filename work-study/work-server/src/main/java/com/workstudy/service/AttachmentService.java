package com.workstudy.service;

import com.workstudy.entity.Attachment;

import java.util.List;

/**
 * 附件元数据服务
 */
public interface AttachmentService {

    /**
     * 保存附件元数据
     *
     * @param attachment
     */
    void save(Attachment attachment);

    /**
     * 按业务类型与业务id查询附件列表
     *
     * @param bizType
     * @param bizId
     * @return
     */
    List<Attachment> list(String bizType, Long bizId);

    /**
     * 根据id删除附件
     *
     * @param id
     */
    void deleteById(Long id);

}
