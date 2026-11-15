package com.workstudy.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.dto.PositionDTO;
import com.workstudy.dto.PositionPageQueryDTO;
import com.workstudy.entity.Position;
import com.workstudy.entity.SysSemester;
import com.workstudy.exception.BaseException;
import com.workstudy.mapper.PositionMapper;
import com.workstudy.result.PageResult;
import com.workstudy.service.DataScopeService;
import com.workstudy.service.PositionService;
import com.workstudy.service.SemesterService;
import com.workstudy.vo.PositionVO;
import com.workstudy.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PositionServiceImpl implements PositionService {

    /**
     * 岗位状态：0 草稿
     */
    private static final int STATUS_DRAFT = 0;

    /**
     * 岗位状态：1 待审批
     */
    private static final int STATUS_PENDING = 1;

    /**
     * 岗位状态：2 已发布
     */
    private static final int STATUS_PUBLISHED = 2;

    /**
     * 岗位状态：3 已驳回
     */
    private static final int STATUS_REJECTED = 3;

    /**
     * 岗位状态：4 已下架
     */
    private static final int STATUS_OFFLINE = 4;

    /**
     * WebSocket 消息类型：1 岗位审批结果
     */
    private static final int WS_TYPE_POSITION = 1;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 新增岗位：一律落草稿(0)，状态只能通过 submit/audit 流转；
     * 数据归属：部门管理员只能为本部门建岗；多学期：未指定学期时默认取当前学期
     *
     * @param positionDTO
     */
    @Override
    @Transactional
    public void save(PositionDTO positionDTO) {
        Position position = new Position();
        BeanUtils.copyProperties(positionDTO, position);
        //状态、名额占用、发布时间、驳回原因均由系统维护，不接受外部传入
        position.setStatus(STATUS_DRAFT);
        position.setHired(0);
        position.setPublishTime(null);
        position.setRejectReason(null);
        //数据归属：部门管理员强制落到本部门
        if (dataScopeService.isDeptAdmin()) {
            position.setDeptId(dataScopeService.getCurrentDeptId());
        }
        if (position.getDeptId() == null) {
            throw new BaseException("用工部门不能为空");
        }
        //多学期：未指定时取当前学期
        if (position.getSemesterId() == null) {
            position.setSemesterId(currentSemesterId());
        }
        positionMapper.insert(position);
    }

    /**
     * 修改岗位：仅草稿(0)/已驳回(3)可改；状态、已录用人数、发布时间不通过本接口修改；
     * 部门管理员只能改本部门岗位
     *
     * @param positionDTO
     */
    @Override
    @Transactional
    public void update(PositionDTO positionDTO) {
        if (positionDTO.getId() == null) {
            throw new BaseException("岗位id不能为空");
        }
        Position exist = getExistPosition(positionDTO.getId());
        //权限与数据归属校验：部门管理员只能操作本部门岗位
        checkWriteScope(exist);
        //状态机：只有草稿与已驳回的岗位可以修改
        if (!isDraftOrRejected(exist.getStatus())) {
            throw new BaseException("仅草稿或已驳回的岗位可以修改");
        }
        Position position = new Position();
        BeanUtils.copyProperties(positionDTO, position);
        //这些字段只能由状态流转或录用流程维护
        position.setStatus(null);
        position.setHired(null);
        position.setPublishTime(null);
        position.setRejectReason(null);
        //部门管理员不得把岗位改到其它部门
        if (dataScopeService.isDeptAdmin()) {
            position.setDeptId(dataScopeService.getCurrentDeptId());
        }
        positionMapper.update(position);
    }

    /**
     * 分页查询岗位：数据归属——部门管理员只看本部门岗位
     *
     * @param positionPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(PositionPageQueryDTO positionPageQueryDTO) {
        if (dataScopeService.isDeptAdmin()) {
            Long deptId = dataScopeService.getCurrentDeptId();
            //未绑定部门的部门管理员看不到任何数据（越权返回空）
            positionPageQueryDTO.setDeptId(deptId == null ? -1L : deptId);
        }
        PageHelper.startPage(defaultPage(positionPageQueryDTO.getPage()), defaultPageSize(positionPageQueryDTO.getPageSize()));
        Page<PositionVO> page = positionMapper.pageQuery(positionPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 查询已发布岗位列表（学生浏览/下拉）
     *
     * @param positionPageQueryDTO
     * @return
     */
    @Override
    public List<PositionVO> list(PositionPageQueryDTO positionPageQueryDTO) {
        //SQL 已固定 status=2，这里不接收外部状态，避免穿透状态机
        return positionMapper.list(positionPageQueryDTO);
    }

    /**
     * 根据id查询岗位详情：非已发布岗位仅本部门管理员/中心管理员/超级管理员可见
     *
     * @param id
     * @return
     */
    @Override
    public PositionVO getById(Long id) {
        PositionVO positionVO = positionMapper.getVOById(id);
        if (positionVO == null) {
            throw new BaseException("岗位不存在");
        }
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(positionVO.getStatus())) {
            dataScopeService.checkDeptScope(positionVO.getDeptId());
        }
        return positionVO;
    }

    /**
     * 提交审批：0 草稿 / 3 已驳回 → 1 待审批，并清空历史驳回原因
     *
     * @param id
     */
    @Override
    @Transactional
    public void submit(Long id) {
        Position exist = getExistPosition(id);
        checkWriteScope(exist);
        if (!isDraftOrRejected(exist.getStatus())) {
            throw new BaseException("仅草稿或已驳回的岗位可以提交审批");
        }
        positionMapper.updateStatus(Position.builder()
                .id(id)
                .status(STATUS_PENDING)
                .rejectReason(null)
                .build());
    }

    /**
     * 审批岗位：1 待审批 →（2 已发布 / 3 已驳回），仅中心管理员/超级管理员可审批；
     * 审批通过记录发布时间，审批完成后 WebSocket 群发 type=1
     *
     * @param positionDTO 需含 id、status(2/3)、rejectReason
     */
    @Override
    @Transactional
    public void audit(PositionDTO positionDTO) {
        //岗位审批只允许中心管理员/超级管理员操作（部门管理员不能审批自己部门的岗位）
        if (!dataScopeService.isSuperAdmin() && !dataScopeService.isCenterAdmin()) {
            throw new BaseException("无权审批岗位");
        }
        if (positionDTO.getId() == null) {
            throw new BaseException("岗位id不能为空");
        }
        Integer status = positionDTO.getStatus();
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(status) && !Integer.valueOf(STATUS_REJECTED).equals(status)) {
            throw new BaseException("审批结果只能为已发布(2)或已驳回(3)");
        }
        Position exist = getExistPosition(positionDTO.getId());
        //状态机：只有待审批的岗位可以审批
        if (!Integer.valueOf(STATUS_PENDING).equals(exist.getStatus())) {
            throw new BaseException("仅待审批的岗位可以审批");
        }
        Position update = new Position();
        update.setId(exist.getId());
        update.setStatus(status);
        if (Integer.valueOf(STATUS_PUBLISHED).equals(status)) {
            //审批通过：清空驳回原因、记录发布时间
            update.setRejectReason(null);
            update.setPublishTime(LocalDateTime.now());
        } else {
            update.setRejectReason(positionDTO.getRejectReason());
        }
        positionMapper.updateStatus(update);

        //业务成功后群发岗位审批结果（type=1）
        if (Integer.valueOf(STATUS_PUBLISHED).equals(status)) {
            sendNotice("岗位「" + exist.getName() + "」审批通过，已发布");
        } else {
            String reason = positionDTO.getRejectReason();
            sendNotice("岗位「" + exist.getName() + "」审批未通过"
                    + (StringUtils.hasText(reason) ? "，驳回原因：" + reason : ""));
        }
    }

    /**
     * 下架岗位：2 已发布 → 4 已下架（保留原发布时间）
     *
     * @param id
     */
    @Override
    @Transactional
    public void offline(Long id) {
        Position exist = getExistPosition(id);
        checkWriteScope(exist);
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(exist.getStatus())) {
            throw new BaseException("仅已发布的岗位可以下架");
        }
        positionMapper.updateStatus(Position.builder()
                .id(id)
                .status(STATUS_OFFLINE)
                .rejectReason(null)
                .build());
    }

    /**
     * 删除岗位：仅草稿/已驳回可删，保留状态机语义避免误删已发布岗位
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        Position exist = getExistPosition(id);
        checkWriteScope(exist);
        if (!isDraftOrRejected(exist.getStatus())) {
            throw new BaseException("仅草稿或已驳回的岗位可以删除");
        }
        positionMapper.deleteById(id);
    }

    /**
     * 查询岗位，不存在抛业务异常
     */
    private Position getExistPosition(Long id) {
        if (id == null) {
            throw new BaseException("岗位id不能为空");
        }
        Position position = positionMapper.getById(id);
        if (position == null) {
            throw new BaseException("岗位不存在");
        }
        return position;
    }

    /**
     * 岗位写操作的权限与数据归属校验：
     * 财务/学生无权写岗位；部门管理员只能写本部门岗位（岗位表数据归属 dept_id）
     */
    private void checkWriteScope(Position position) {
        if (dataScopeService.isFinance() || dataScopeService.isStudent()) {
            throw new BaseException("无权操作岗位数据");
        }
        dataScopeService.checkDeptScope(position.getDeptId());
    }

    /**
     * 是否草稿或已驳回（这两个状态可以修改/提交/删除）
     */
    private boolean isDraftOrRejected(Integer status) {
        return Integer.valueOf(STATUS_DRAFT).equals(status) || Integer.valueOf(STATUS_REJECTED).equals(status);
    }

    /**
     * 当前学期id（多学期隔离：岗位必须挂到某个学期）
     */
    private Long currentSemesterId() {
        SysSemester semester = semesterService.getCurrent();
        if (semester == null) {
            throw new BaseException("当前没有启用的学期，请先设置当前学期");
        }
        return semester.getId();
    }

    /**
     * 分页参数兜底
     */
    private Integer defaultPage(Integer page) {
        return page == null ? 1 : page;
    }

    /**
     * 分页参数兜底
     */
    private Integer defaultPageSize(Integer pageSize) {
        return pageSize == null ? 10 : pageSize;
    }

    /**
     * WebSocket 群发岗位审批结果（type=1），通知失败不影响主业务
     */
    private void sendNotice(String content) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("type", WS_TYPE_POSITION);
            map.put("content", content);
            webSocketServer.sendToAllClient(JSON.toJSONString(map));
        } catch (Exception e) {
            log.error("岗位审批结果WebSocket通知发送失败：{}", e.getMessage());
        }
    }

}
