package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.entity.Attachment;
import com.workstudy.result.Result;
import com.workstudy.service.AttachmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 附件元数据管理（M9，文件本体存 OSS，此处只存元数据）
 */
@RestController
@RequestMapping("/attachment")
@Slf4j
@Tag(name = "附件管理接口")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    /**
     * 保存附件元数据（上传人取当前登录用户）
     *
     * @param attachment
     * @return
     */
    @PostMapping
    @Operation(summary = "保存附件元数据")
    @Log(title = "附件管理")
    public Result save(@RequestBody Attachment attachment) {
        log.info("保存附件元数据：{}", attachment);
        attachmentService.save(attachment);
        return Result.success();
    }

    /**
     * 按业务类型与业务id查询附件列表
     *
     * @param bizType
     * @param bizId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询附件列表")
    public Result<List<Attachment>> list(String bizType, Long bizId) {
        log.info("查询附件列表：bizType={}, bizId={}", bizType, bizId);
        List<Attachment> list = attachmentService.list(bizType, bizId);
        return Result.success(list);
    }

    /**
     * 根据id删除附件元数据
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除附件")
    @Log(title = "附件管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除附件：{}", id);
        attachmentService.deleteById(id);
        return Result.success();
    }

}
