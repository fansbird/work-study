package com.workstudy.service;

import com.workstudy.dto.WorkHourDTO;
import com.workstudy.dto.WorkHourPageQueryDTO;
import com.workstudy.result.PageResult;
import com.workstudy.vo.WorkHourVO;

/**
 * 工时管理服务（M4：规则校验 + 两级审核状态机）
 */
public interface WorkHourService {

    /**
     * 学生填报工时（直接落 status=0 待部门审核）
     *
     * @param workHourDTO
     */
    void save(WorkHourDTO workHourDTO);

    /**
     * 修改工时（仅 status=3 驳回重填可改）
     *
     * @param workHourDTO
     */
    void update(WorkHourDTO workHourDTO);

    /**
     * 驳回后重新提交（3 → 0，提交前重新执行全部规则校验）
     *
     * @param id
     */
    void submit(Long id);

    /**
     * 我的工时分页（学生本人）
     *
     * @param workHourPageQueryDTO
     * @return
     */
    PageResult myPage(WorkHourPageQueryDTO workHourPageQueryDTO);

    /**
     * 工时分页查询（部门管理员限本部门，中心/财务/超管可跨部门）
     *
     * @param workHourPageQueryDTO
     * @return
     */
    PageResult pageQuery(WorkHourPageQueryDTO workHourPageQueryDTO);

    /**
     * 根据id查询工时详情（带权限校验）
     *
     * @param id
     * @return
     */
    WorkHourVO getById(Long id);

    /**
     * 部门审核（限本部门，0 → 1 通过 / 3 驳回）
     *
     * @param id
     * @param status       审核结果 1=通过（转待中心复核） 3=驳回
     * @param rejectReason 驳回原因
     */
    void deptAudit(Long id, Integer status, String rejectReason);

    /**
     * 中心复核（1 → 2 通过锁定 / 3 驳回）
     *
     * @param id
     * @param status       复核结果 2=通过（锁定） 3=驳回
     * @param rejectReason 驳回原因
     */
    void centerAudit(Long id, Integer status, String rejectReason);

    /**
     * 删除工时（仅 0/3 状态且未结算可删）
     *
     * @param id
     */
    void deleteById(Long id);

}
