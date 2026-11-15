package com.workstudy.service;

import com.workstudy.dto.PositionApplyPageQueryDTO;
import com.workstudy.result.PageResult;

/**
 * 岗位报名服务
 * 状态机：0 待审核 →（1 已录用 / 2 已驳回）；0 → 3 已撤销；1 已录用 → 4 已放弃（释放名额 hired-1）
 */
public interface PositionApplyService {

    /**
     * 学生报名岗位（落 status=0 待审核，学期取岗位所属学期）
     *
     * @param positionId
     * @param applyRemark
     */
    void apply(Long positionId, String applyRemark);

    /**
     * 撤销报名：0 待审核 → 3 已撤销（仅本人）
     *
     * @param id
     */
    void cancel(Long id);

    /**
     * 报名审核：0 待审核 →（1 已录用 / 2 已驳回）
     * 录用时先做名额条件更新（hired=hired+1 where hired<quota），名额已满则抛业务异常；
     * 录用与名额更新在同一事务内
     *
     * @param id
     * @param status        1 已录用 / 2 已驳回
     * @param rejectReason
     */
    void audit(Long id, Integer status, String rejectReason);

    /**
     * 放弃录用：1 已录用 → 4 已放弃，并释放名额（hired-1），同一事务
     *
     * @param id
     */
    void giveUp(Long id);

    /**
     * 分页查询报名（管理端；部门管理员只能看本部门岗位下的报名）
     *
     * @param positionApplyPageQueryDTO
     * @return
     */
    PageResult pageQuery(PositionApplyPageQueryDTO positionApplyPageQueryDTO);

    /**
     * 我的报名分页（学生端，只返回本人报名）
     *
     * @param positionApplyPageQueryDTO
     * @return
     */
    PageResult myPage(PositionApplyPageQueryDTO positionApplyPageQueryDTO);

    /**
     * 校验当前学生是否已报名某岗位（待审核/已录用/已驳回视为已报名；已撤销/已放弃可重新报名）
     *
     * @param positionId
     * @return
     */
    Boolean check(Long positionId);

}
