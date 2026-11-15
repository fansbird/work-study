package com.workstudy.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.context.BaseContext;
import com.workstudy.dto.PositionApplyPageQueryDTO;
import com.workstudy.entity.Position;
import com.workstudy.entity.PositionApply;
import com.workstudy.entity.SysSemester;
import com.workstudy.exception.BaseException;
import com.workstudy.mapper.PositionApplyMapper;
import com.workstudy.mapper.PositionMapper;
import com.workstudy.result.PageResult;
import com.workstudy.service.DataScopeService;
import com.workstudy.service.PositionApplyService;
import com.workstudy.service.SemesterService;
import com.workstudy.vo.PositionApplyVO;
import com.workstudy.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PositionApplyServiceImpl implements PositionApplyService {

    /**
     * 报名状态：0 待审核
     */
    private static final int APPLY_PENDING = 0;

    /**
     * 报名状态：1 已录用
     */
    private static final int APPLY_HIRED = 1;

    /**
     * 报名状态：2 已驳回
     */
    private static final int APPLY_REJECTED = 2;

    /**
     * 报名状态：3 已撤销
     */
    private static final int APPLY_CANCELED = 3;

    /**
     * 报名状态：4 已放弃
     */
    private static final int APPLY_GIVEN_UP = 4;

    /**
     * 岗位状态：2 已发布
     */
    private static final int POSITION_PUBLISHED = 2;

    /**
     * WebSocket 消息类型：2 报名审核/录用结果
     */
    private static final int WS_TYPE_APPLY = 2;

    @Autowired
    private PositionApplyMapper positionApplyMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 学生报名岗位：只有已发布(2)的岗位可报名；学期取岗位所属学期；
     * 唯一键 uk_position_student 保证同一岗位同一学生只有一条记录：
     * 已撤销(3)/已放弃(4) 的记录复用原记录重新报名，其余状态视为重复报名
     *
     * @param positionId
     * @param applyRemark
     */
    @Override
    @Transactional
    public void apply(Long positionId, String applyRemark) {
        if (positionId == null) {
            throw new BaseException("岗位id不能为空");
        }
        //数据归属：学生只能为自己报名
        Long studentId = currentStudentId();
        Position position = positionMapper.getById(positionId);
        if (position == null) {
            throw new BaseException("岗位不存在");
        }
        if (!Integer.valueOf(POSITION_PUBLISHED).equals(position.getStatus())) {
            throw new BaseException("该岗位未发布，不能报名");
        }
        //多学期：报名与岗位同属一个学期
        Long semesterId = position.getSemesterId() == null ? currentSemesterId() : position.getSemesterId();

        PositionApply exist = positionApplyMapper.getByPositionAndStudent(positionId, studentId);
        if (exist != null && isActiveApply(exist.getStatus())) {
            throw new BaseException("您已报名该岗位，请勿重复报名");
        }
        if (exist != null) {
            //已撤销/已放弃：复位原记录为待审核（唯一键限制，不能新增第二条）
            positionApplyMapper.resetApply(PositionApply.builder()
                    .id(exist.getId())
                    .semesterId(semesterId)
                    .applyRemark(applyRemark)
                    .status(APPLY_PENDING)
                    .build());
            return;
        }
        PositionApply positionApply = PositionApply.builder()
                .positionId(positionId)
                .studentId(studentId)
                .semesterId(semesterId)
                .applyRemark(applyRemark)
                .status(APPLY_PENDING)
                .build();
        positionApplyMapper.insert(positionApply);
    }

    /**
     * 撤销报名：0 待审核 → 3 已撤销，仅本人可撤销
     *
     * @param id
     */
    @Override
    @Transactional
    public void cancel(Long id) {
        PositionApply exist = getExistApply(id);
        //数据归属：学生只能撤销本人的报名
        Long studentId = dataScopeService.getCurrentStudentId();
        if (!dataScopeService.isStudent() || studentId == null || !studentId.equals(exist.getStudentId())) {
            throw new BaseException("只能撤销本人的报名记录");
        }
        if (!Integer.valueOf(APPLY_PENDING).equals(exist.getStatus())) {
            throw new BaseException("仅待审核的报名可以撤销");
        }
        PositionApply update = new PositionApply();
        update.setId(id);
        update.setStatus(APPLY_CANCELED);
        positionApplyMapper.update(update);
    }

    /**
     * 报名审核：0 待审核 →（1 已录用 / 2 已驳回）
     * 名额并发（核心）：录用时先做条件更新 UPDATE position SET hired=hired+1 WHERE id=? AND hired<quota，(CAS乐观锁)
     * 影响行数=0 说明名额已满，抛业务异常（报名保持待审核，事务回滚）；
     * 录用与名额更新、报名状态更新在同一事务内。
     * 数据归属：部门管理员只能审核本部门岗位下的报名（岗位 dept_id 校验）
     *
     * @param id
     * @param status        1 已录用 / 2 已驳回
     * @param rejectReason
     */
    @Override
    @Transactional
    public void audit(Long id, Integer status, String rejectReason) {
        if (id == null) {
            throw new BaseException("报名id不能为空");
        }
        if (!Integer.valueOf(APPLY_HIRED).equals(status) && !Integer.valueOf(APPLY_REJECTED).equals(status)) {
            throw new BaseException("审核结果只能为已录用(1)或已驳回(2)");
        }
        PositionApply exist = getExistApply(id);
        Position position = positionMapper.getById(exist.getPositionId());
        if (position == null) {
            throw new BaseException("岗位不存在");
        }
        //权限与数据归属校验：部门管理员只能审核本部门岗位下的报名
        checkManageScope(position.getDeptId());
        //状态机：只有待审核的报名可以审核
        if (!Integer.valueOf(APPLY_PENDING).equals(exist.getStatus())) {
            throw new BaseException("仅待审核的报名可以审核");
        }

        Long auditUser = BaseContext.getCurrentId();
        if (Integer.valueOf(APPLY_HIRED).equals(status)) {
            //名额并发：条件更新占用名额，返回 0 表示 hired 已达到 quota，名额已满
            int rows = positionMapper.incrHired(position.getId());
            if (rows == 0) {
                throw new BaseException("该岗位名额已满，无法录用");
            }
            positionApplyMapper.update(PositionApply.builder()
                    .id(id)
                    .status(APPLY_HIRED)
                    .auditUser(auditUser)
                    .auditTime(LocalDateTime.now())
                    .build());
            sendNotice("学生报名岗位「" + position.getName() + "」已录用");
        } else {
            positionApplyMapper.update(PositionApply.builder()
                    .id(id)
                    .status(APPLY_REJECTED)
                    .rejectReason(rejectReason)
                    .auditUser(auditUser)
                    .auditTime(LocalDateTime.now())
                    .build());
            sendNotice("学生报名岗位「" + position.getName() + "」已驳回");
        }
    }

    /**
     * 放弃录用：1 已录用 → 4 已放弃，并释放名额 hired-1，两个更新在同一事务内；
     * 数据归属：学生只能放弃本人的录用，管理端须在所属部门范围内
     *
     * @param id
     */
    @Override
    @Transactional
    public void giveUp(Long id) {
        PositionApply exist = getExistApply(id);
        Position position = positionMapper.getById(exist.getPositionId());
        if (position == null) {
            throw new BaseException("岗位不存在");
        }
        if (dataScopeService.isStudent()) {
            Long studentId = dataScopeService.getCurrentStudentId();
            if (studentId == null || !studentId.equals(exist.getStudentId())) {
                throw new BaseException("只能放弃本人的报名记录");
            }
        } else {
            checkManageScope(position.getDeptId());
        }
        //状态机：只有已录用的报名可以放弃
        if (!Integer.valueOf(APPLY_HIRED).equals(exist.getStatus())) {
            throw new BaseException("仅已录用的报名可以放弃");
        }
        //名额释放：条件更新 hired-1（hired>0），同一事务内与报名状态一起提交
        int rows = positionMapper.decrHired(position.getId());
        if (rows == 0) {
            throw new BaseException("岗位已录用人数异常，名额释放失败");
        }
        PositionApply update = new PositionApply();
        update.setId(id);
        update.setStatus(APPLY_GIVEN_UP);
        positionApplyMapper.update(update);

        sendNotice("学生已放弃岗位「" + position.getName() + "」的录用，名额已释放");
    }

    /**
     * 分页查询报名（管理端）：数据归属——部门管理员只看本部门岗位下的报名；
     * 学生调用时只返回本人报名，防止越权
     *
     * @param positionApplyPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(PositionApplyPageQueryDTO positionApplyPageQueryDTO) {
        Long deptId = null;
        if (dataScopeService.isDeptAdmin()) {
            deptId = dataScopeService.getCurrentDeptId();
            //未绑定部门的部门管理员看不到任何数据（越权返回空）
            if (deptId == null) {
                return new PageResult(0L, new ArrayList<>());
            }
        } else if (dataScopeService.isStudent()) {
            positionApplyPageQueryDTO.setStudentId(currentStudentId());
        }
        PageHelper.startPage(defaultPage(positionApplyPageQueryDTO.getPage()),
                defaultPageSize(positionApplyPageQueryDTO.getPageSize()));
        Page<PositionApplyVO> page = positionApplyMapper.pageQuery(positionApplyPageQueryDTO, deptId);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 我的报名分页（学生端）：数据归属——强制只查本人
     *
     * @param positionApplyPageQueryDTO
     * @return
     */
    @Override
    public PageResult myPage(PositionApplyPageQueryDTO positionApplyPageQueryDTO) {
        positionApplyPageQueryDTO.setStudentId(currentStudentId());
        PageHelper.startPage(defaultPage(positionApplyPageQueryDTO.getPage()),
                defaultPageSize(positionApplyPageQueryDTO.getPageSize()));
        Page<PositionApplyVO> page = positionApplyMapper.pageQuery(positionApplyPageQueryDTO, null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 校验当前学生是否已报名某岗位：
     * 待审核(0)/已录用(1)/已驳回(2) 视为已报名；已撤销(3)/已放弃(4) 可重新报名
     *
     * @param positionId
     * @return
     */
    @Override
    public Boolean check(Long positionId) {
        if (positionId == null) {
            throw new BaseException("岗位id不能为空");
        }
        PositionApply exist = positionApplyMapper.getByPositionAndStudent(positionId, currentStudentId());
        return exist != null && isActiveApply(exist.getStatus());
    }

    /**
     * 查询报名，不存在抛业务异常
     */
    private PositionApply getExistApply(Long id) {
        if (id == null) {
            throw new BaseException("报名id不能为空");
        }
        PositionApply positionApply = positionApplyMapper.getById(id);
        if (positionApply == null) {
            throw new BaseException("报名记录不存在");
        }
        return positionApply;
    }

    /**
     * 报名管理端（审核/驳回/管理端放弃录用）的权限与数据归属校验：
     * 学生与财务无权操作报名审核；部门管理员只能操作本部门岗位下的报名（岗位表数据归属 dept_id）
     */
    private void checkManageScope(Long deptId) {
        if (dataScopeService.isStudent() || dataScopeService.isFinance()) {
            throw new BaseException("无权操作报名数据");
        }
        dataScopeService.checkDeptScope(deptId);
    }

    /**
     * 是否处于"已报名"的有效状态（待审核/已录用/已驳回）
     */
    private boolean isActiveApply(Integer status) {
        return Integer.valueOf(APPLY_PENDING).equals(status)
                || Integer.valueOf(APPLY_HIRED).equals(status)
                || Integer.valueOf(APPLY_REJECTED).equals(status);
    }

    /**
     * 当前登录学生关联的学生id
     */
    private Long currentStudentId() {
        Long studentId = dataScopeService.getCurrentStudentId();
        if (studentId == null) {
            throw new BaseException("当前登录账号未关联学生档案，无法报名");
        }
        return studentId;
    }

    /**
     * 当前学期id（多学期隔离）
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
     * WebSocket 群发报名审核/录用结果（type=2），通知失败不影响主业务
     */
    private void sendNotice(String content) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("type", WS_TYPE_APPLY);
            map.put("content", content);
            webSocketServer.sendToAllClient(JSON.toJSONString(map));
        } catch (Exception e) {
            log.error("报名审核结果WebSocket通知发送失败：{}", e.getMessage());
        }
    }

}
