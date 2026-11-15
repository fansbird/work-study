package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.constant.MessageConstant;
import com.workstudy.result.Result;
import com.workstudy.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口（文件上传，M9）
 */
@RestController
@RequestMapping("/common")
@Slf4j
@Tag(name = "通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传：UUID 文件名 + 原扩展名，返回 OSS 访问地址
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传")
    @Log(title = "文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);
        try {
            //获取文件原始名称
            String originalFilename = file.getOriginalFilename();
            //获取文件扩展名（含点，无扩展名时为空串）
            int index = originalFilename == null ? -1 : originalFilename.lastIndexOf(".");
            String extension = index >= 0 ? originalFilename.substring(index) : "";
            //生成随机文件名
            String objectName = UUID.randomUUID().toString() + extension;
            //上传文件到OSS
            String url = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

}
