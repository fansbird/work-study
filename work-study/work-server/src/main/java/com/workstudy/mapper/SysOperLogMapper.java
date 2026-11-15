package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 操作日志 Mapper（写入侧由 OperLogAspect 调用，查询侧供 OperLogController 使用）
 */
@Mapper
public interface SysOperLogMapper {

    /**
     * 新增操作日志
     *
     * @param sysOperLog
     */
    void insert(SysOperLog sysOperLog);

    /**
     * 分页条件查询操作日志
     *
     * @param title        模块标题（模糊）
     * @param operUserName 操作人姓名（模糊）
     * @param status       状态 1正常 0异常
     * @param begin        操作时间起
     * @param end          操作时间止
     * @return
     */
    Page<SysOperLog> pageQuery(@Param("title") String title,
                               @Param("operUserName") String operUserName,
                               @Param("status") Integer status,
                               @Param("begin") LocalDateTime begin,
                               @Param("end") LocalDateTime end);

    /**
     * 根据id查询操作日志
     *
     * @param id
     * @return
     */
    SysOperLog getById(Long id);

}
