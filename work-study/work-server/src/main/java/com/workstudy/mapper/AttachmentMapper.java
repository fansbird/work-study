package com.workstudy.mapper;

import com.workstudy.entity.Attachment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    /**
     * 新增附件元数据
     *
     * @param attachment
     */
    void insert(Attachment attachment);

    /**
     * 按业务类型与业务id查询附件列表
     *
     * @param bizType
     * @param bizId
     * @return
     */
    List<Attachment> list(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    /**
     * 根据id删除附件
     *
     * @param id
     */
    @Delete("delete from attachment where id = #{id}")
    void deleteById(Long id);

}
