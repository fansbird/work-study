package com.workstudy.service;

import com.workstudy.dto.PositionDTO;
import com.workstudy.dto.PositionPageQueryDTO;
import com.workstudy.result.PageResult;
import com.workstudy.vo.PositionVO;

import java.util.List;

/**
 * 岗位管理服务
 * 状态机：0 草稿 → 1 待审批 →（2 已发布 / 3 已驳回）；2 已发布 → 4 已下架；3 已驳回修改后可重新提交回 1
 */
public interface PositionService {

    /**
     * 新增岗位（落草稿 status=0）
     *
     * @param positionDTO
     */
    void save(PositionDTO positionDTO);

    /**
     * 修改岗位（仅草稿/已驳回可改）
     *
     * @param positionDTO
     */
    void update(PositionDTO positionDTO);

    /**
     * 分页查询岗位（部门管理员只能看本部门岗位）
     *
     * @param positionPageQueryDTO
     * @return
     */
    PageResult pageQuery(PositionPageQueryDTO positionPageQueryDTO);

    /**
     * 查询已发布岗位列表（0 草稿/1 待审批/3 已驳回/4 已下架均不返回）
     *
     * @param positionPageQueryDTO
     * @return
     */
    List<PositionVO> list(PositionPageQueryDTO positionPageQueryDTO);

    /**
     * 根据id查询岗位详情
     *
     * @param id
     * @return
     */
    PositionVO getById(Long id);

    /**
     * 提交审批：0 草稿/3 已驳回 → 1 待审批
     *
     * @param id
     */
    void submit(Long id);

    /**
     * 审批岗位：1 待审批 →（2 已发布 / 3 已驳回）
     *
     * @param positionDTO 需含 id、status(2/3)、rejectReason
     */
    void audit(PositionDTO positionDTO);

    /**
     * 下架岗位：2 已发布 → 4 已下架
     *
     * @param id
     */
    void offline(Long id);

    /**
     * 根据id删除岗位（仅草稿/已驳回可删）
     *
     * @param id
     */
    void deleteById(Long id);

}
