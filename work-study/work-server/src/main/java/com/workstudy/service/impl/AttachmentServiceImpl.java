package com.workstudy.service.impl;

import com.workstudy.context.BaseContext;
import com.workstudy.entity.Attachment;
import com.workstudy.mapper.AttachmentMapper;
import com.workstudy.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 附件元数据服务实现
 */
@Service
public class AttachmentServiceImpl implements AttachmentService {

    @Autowired
    private AttachmentMapper attachmentMapper;

    /**
     * 保存附件元数据：上传人取当前登录用户，创建时间取服务端时间
     *
     * @param attachment
     */
    @Override
    @Transactional
    public void save(Attachment attachment) {
        attachment.setUploadUser(BaseContext.getCurrentId());
        attachment.setCreateTime(LocalDateTime.now());
        attachmentMapper.insert(attachment);
    }

    /**
     * 按业务类型与业务id查询附件列表
     *
     * @param bizType
     * @param bizId
     * @return
     */
    @Override
    public List<Attachment> list(String bizType, Long bizId) {
        return attachmentMapper.list(bizType, bizId);
    }

    /**
     * 根据id删除附件
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        attachmentMapper.deleteById(id);
    }

}
