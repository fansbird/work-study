package com.workstudy.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.context.BaseContext;
import com.workstudy.dto.WorkHourDTO;
import com.workstudy.dto.WorkHourPageQueryDTO;
import com.workstudy.entity.Position;
import com.workstudy.entity.SysSemester;
import com.workstudy.entity.WorkHour;
import com.workstudy.exception.BaseException;
import com.workstudy.mapper.WorkHourMapper;
import com.workstudy.properties.CheckProperties;
import com.workstudy.result.PageResult;
import com.workstudy.service.DataScopeService;
import com.workstudy.service.SemesterService;
import com.workstudy.service.WorkHourService;
import com.workstudy.vo.WorkHourVO;
import com.workstudy.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 工时管理服务实现（M4：规则校验 + 两级审核状态机）
 *   0 待部门审核 --部门审核通过--> 1 待中心复核 --中心复核通过--> 2 通过（锁定）
 *   0/1 --任一环节驳回--> 3 驳回重填 --修改后 submit--> 0
 * 填报/修改/提交三个入口统一执行 {@link #validate(WorkHour, Long)} 全部规则校验。
 */
@Service
@Slf4j
public class WorkHourServiceImpl implements WorkHourService {

    /**
     * 工时：待部门审核
     */
    private static final int STATUS_DEPT_PENDING = 0;

    /**
     * 工时：待中心复核
     */
    private static final int STATUS_CENTER_PENDING = 1;

    /**
     * 工时：通过（锁定）
     */
    private static final int STATUS_PASSED = 2;

    /**
     * 工时：驳回重填
     */
    private static final int STATUS_REJECTED = 3;

    /**
     * 工时最小计量单位：30 分钟（半小时）
     */
    private static final int MIN_UNIT_MINUTES = 30;

    /**
     * 分页默认页码/每页条数
     */
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    private WorkHourMapper workHourMapper;

    @Autowired
    private CheckProperties checkProperties;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 学生填报工时：岗位须已录用该学生，dept_id/semester_id 从岗位带出，落 status=0
     *
     * @param workHourDTO
     */
    @Override
    @Transactional
    public void save(WorkHourDTO workHourDTO) {
        Long studentId = dataScopeService.getCurrentStudentId();
        if (studentId == null) {
            throw new BaseException("当前账号未关联学生档案，无法填报工时");
        }
        if (workHourDTO.getPositionId() == null) {
            throw new BaseException("请选择填报的岗位");
        }
        Position position = getPosition(workHourDTO.getPositionId());

        WorkHour workHour = new WorkHour();
        BeanUtils.copyProperties(workHourDTO, workHour);
        //由服务端确定归属与初始状态，忽略客户端传入
        workHour.setId(null);
        workHour.setStudentId(studentId);
        workHour.setDeptId(position.getDeptId());
        workHour.setSemesterId(position.getSemesterId());
        workHour.setStatus(STATUS_DEPT_PENDING);
        workHour.setRejectReason(null);
        workHour.setSettlementId(null);

        //规则校验（含 hours 计算）
        validate(workHour, null);

        workHourMapper.insert(workHour);
        log.info("工时填报成功，学生id：{}，岗位id：{}，日期：{}，工时：{}", studentId, position.getId(),
                workHour.getWorkDate(), workHour.getHours());
    }

    /**
     * 修改工时：仅 status=3 驳回重填可改，修改后仍为 3，需再调用 submit 回到 0
     *
     * @param workHourDTO
     */
    @Override
    @Transactional
    public void update(WorkHourDTO workHourDTO) {
        if (workHourDTO.getId() == null) {
            throw new BaseException("工时id不能为空");
        }
        WorkHour exist = workHourMapper.getById(workHourDTO.getId());
        if (exist == null) {
            throw new BaseException("工时记录不存在");
        }
        if (!Integer.valueOf(STATUS_REJECTED).equals(exist.getStatus())) {
            throw new BaseException("仅驳回重填状态的工时可以修改");
        }
        //数据归属：仅学生本人（或超管）
        checkStudentOwner(exist);

        WorkHour workHour = new WorkHour();
        BeanUtils.copyProperties(workHourDTO, workHour);
        workHour.setId(exist.getId());
        //学生、状态、结算单不可通过修改接口变更
        workHour.setStudentId(exist.getStudentId());
        workHour.setStatus(null);
        workHour.setRejectReason(null);
        workHour.setSettlementId(null);

        //岗位变更时重新带出部门/学期，并重新校验录用关系
        if (workHourDTO.getPositionId() != null && !workHourDTO.getPositionId().equals(exist.getPositionId())) {
            Position position = getPosition(workHourDTO.getPositionId());
            workHour.setDeptId(position.getDeptId());
            workHour.setSemesterId(position.getSemesterId());
        } else {
            workHour.setPositionId(exist.getPositionId());
            workHour.setDeptId(exist.getDeptId());
            workHour.setSemesterId(exist.getSemesterId());
        }
        //未传的字段补齐为原值，保证校验完整
        if (workHour.getWorkDate() == null) {
            workHour.setWorkDate(exist.getWorkDate());
        }
        if (workHour.getStartTime() == null) {
            workHour.setStartTime(exist.getStartTime());
        }
        if (workHour.getEndTime() == null) {
            workHour.setEndTime(exist.getEndTime());
        }
        if (workHour.getWorkContent() == null) {
            workHour.setWorkContent(exist.getWorkContent());
        }

        validate(workHour, exist.getId());

        workHourMapper.update(workHour);
        log.info("工时修改成功，id：{}，工时：{}", workHour.getId(), workHour.getHours());
    }

    /**
     * 驳回后重新提交：3 → 0，提交前重新执行全部规则校验
     *
     * @param id
     */
    @Override
    @Transactional
    public void submit(Long id) {
        WorkHour exist = workHourMapper.getById(id);
        if (exist == null) {
            throw new BaseException("工时记录不存在");
        }
        if (!Integer.valueOf(STATUS_REJECTED).equals(exist.getStatus())) {
            throw new BaseException("仅驳回重填状态的工时可以重新提交");
        }
        checkStudentOwner(exist);

        //重新校验：确保修改后的数据依旧满足全部工时规则
        validate(exist, exist.getId());

        WorkHour update = new WorkHour();
        update.setId(id);
        workHourMapper.submit(update);
        log.info("工时重新提交成功，id：{}，已回到待部门审核", id);
    }

    /**
     * 我的工时分页（强制 student_id = 当前登录学生）
     *
     * @param workHourPageQueryDTO
     * @return
     */
    @Override
    public PageResult myPage(WorkHourPageQueryDTO workHourPageQueryDTO) {
        Long studentId = dataScopeService.getCurrentStudentId();
        if (studentId == null) {
            //非学生或无关联学生档案：返回空页
            return new PageResult(0, Collections.emptyList());
        }
        workHourPageQueryDTO.setStudentId(studentId);
        workHourPageQueryDTO.setDeptId(null);
        return doPageQuery(workHourPageQueryDTO);
    }

    /**
     * 工时分页查询（部门管理员强制本部门，学生强制本人）
     *
     * @param workHourPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(WorkHourPageQueryDTO workHourPageQueryDTO) {
        if (dataScopeService.isDeptAdmin()) {
            //数据归属：部门管理员只能看本部门；账号未绑定部门时返回空页（不能退化为全库可见）
            Long deptId = dataScopeService.getCurrentDeptId();
            if (deptId == null) {
                return new PageResult(0, Collections.emptyList());
            }
            workHourPageQueryDTO.setDeptId(deptId);
        } else if (dataScopeService.isStudent()) {
            //数据归属：学生只能看本人；账号未关联学生档案时返回空页
            Long studentId = dataScopeService.getCurrentStudentId();
            if (studentId == null) {
                return new PageResult(0, Collections.emptyList());
            }
            workHourPageQueryDTO.setStudentId(studentId);
        } else if (workHourPageQueryDTO.getDeptId() != null) {
            dataScopeService.checkDeptScope(workHourPageQueryDTO.getDeptId());
        }
        return doPageQuery(workHourPageQueryDTO);
    }

    /**
     * 根据id查询工时详情（带数据归属校验）
     *
     * @param id
     * @return
     */
    @Override
    public WorkHourVO getById(Long id) {
        WorkHourVO workHourVO = workHourMapper.getVoById(id);
        if (workHourVO == null) {
            throw new BaseException("工时记录不存在");
        }
        //数据归属校验
        if (dataScopeService.isSuperAdmin() || dataScopeService.isCenterAdmin() || dataScopeService.isFinance()) {
            return workHourVO;
        }
        if (dataScopeService.isDeptAdmin()) {
            checkDeptScopeStrict(workHourVO.getDeptId());
            return workHourVO;
        }
        Long studentId = dataScopeService.getCurrentStudentId();
        if (studentId != null && studentId.equals(workHourVO.getStudentId())) {
            return workHourVO;
        }
        throw new BaseException("无权查看该工时记录");
    }

    /**
     * 部门审核：限本部门，0 → 1（通过，转待中心复核）/ 0 → 3（驳回）
     *
     * @param id
     * @param status       1=通过 3=驳回
     * @param rejectReason 驳回原因
     */
    @Override
    @Transactional
    public void deptAudit(Long id, Integer status, String rejectReason) {
        if (!(dataScopeService.isDeptAdmin() || dataScopeService.isSuperAdmin())) {
            throw new BaseException("仅用工部门管理员可进行部门审核");
        }
        WorkHour exist = workHourMapper.getById(id);
        if (exist == null) {
            throw new BaseException("工时记录不存在");
        }
        if (!Integer.valueOf(STATUS_DEPT_PENDING).equals(exist.getStatus())) {
            throw new BaseException("当前工时状态不可进行部门审核");
        }
        //数据归属：部门管理员仅能审核本部门工时
        if (dataScopeService.isDeptAdmin()) {
            checkDeptScopeStrict(exist.getDeptId());
        }

        int targetStatus;
        if (Integer.valueOf(STATUS_CENTER_PENDING).equals(status)) {
            targetStatus = STATUS_CENTER_PENDING;
        } else if (Integer.valueOf(STATUS_REJECTED).equals(status)) {
            targetStatus = STATUS_REJECTED;
        } else {
            throw new BaseException("部门审核结果参数不合法（1=通过，3=驳回）");
        }
        checkRejectReason(targetStatus, rejectReason);

        WorkHour update = new WorkHour();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setRejectReason(targetStatus == STATUS_REJECTED ? rejectReason : null);
        update.setDeptAuditUser(BaseContext.getCurrentId());
        update.setDeptAuditTime(LocalDateTime.now());
        workHourMapper.updateAudit(update);

        //业务节点：工时审核结果 WebSocket 群发（type=3）
        pushWorkHourNotice(exist, targetStatus, "部门审核");
        log.info("部门审核完成，工时id：{}，目标状态：{}", id, targetStatus);
    }

    /**
     * 中心复核：1 → 2（通过锁定）/ 1 → 3（驳回）
     *
     * @param id
     * @param status       2=通过（锁定） 3=驳回
     * @param rejectReason 驳回原因
     */
    @Override
    @Transactional
    public void centerAudit(Long id, Integer status, String rejectReason) {
        if (!(dataScopeService.isCenterAdmin() || dataScopeService.isSuperAdmin())) {
            throw new BaseException("仅勤工助学中心管理员可进行中心复核");
        }
        WorkHour exist = workHourMapper.getById(id);
        if (exist == null) {
            throw new BaseException("工时记录不存在");
        }
        if (!Integer.valueOf(STATUS_CENTER_PENDING).equals(exist.getStatus())) {
            throw new BaseException("当前工时状态不可进行中心复核");
        }

        int targetStatus;
        if (Integer.valueOf(STATUS_PASSED).equals(status)) {
            targetStatus = STATUS_PASSED;
        } else if (Integer.valueOf(STATUS_REJECTED).equals(status)) {
            targetStatus = STATUS_REJECTED;
        } else {
            throw new BaseException("中心复核结果参数不合法（2=通过，3=驳回）");
        }
        checkRejectReason(targetStatus, rejectReason);

        WorkHour update = new WorkHour();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setRejectReason(targetStatus == STATUS_REJECTED ? rejectReason : null);
        update.setCenterAuditUser(BaseContext.getCurrentId());
        update.setCenterAuditTime(LocalDateTime.now());
        workHourMapper.updateAudit(update);

        pushWorkHourNotice(exist, targetStatus, "中心复核");
        log.info("中心复核完成，工时id：{}，目标状态：{}", id, targetStatus);
    }

    /**
     * 删除工时：仅 0/3 状态且未进入结算可删
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        WorkHour exist = workHourMapper.getById(id);
        if (exist == null) {
            throw new BaseException("工时记录不存在");
        }
        if (!(Integer.valueOf(STATUS_DEPT_PENDING).equals(exist.getStatus())
                || Integer.valueOf(STATUS_REJECTED).equals(exist.getStatus()))) {
            throw new BaseException("仅待部门审核或驳回重填状态的工时可以删除");
        }
        if (exist.getSettlementId() != null) {
            throw new BaseException("该工时已进入结算，不能删除");
        }
        //数据归属：学生本人 / 本部门管理员 / 超管
        checkDeletePermission(exist);
        workHourMapper.deleteById(id);
        log.info("工时删除成功，id：{}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 分页公共实现：默认当前学期（多学期隔离），并补齐分页默认值
     */
    private PageResult doPageQuery(WorkHourPageQueryDTO workHourPageQueryDTO) {
        //学期隔离：未指定学期时默认当前学期
        if (workHourPageQueryDTO.getSemesterId() == null) {
            SysSemester current = semesterService.getCurrent();
            if (current != null) {
                workHourPageQueryDTO.setSemesterId(current.getId());
            }
        }
        int page = workHourPageQueryDTO.getPage() == null ? DEFAULT_PAGE : workHourPageQueryDTO.getPage();
        int pageSize = workHourPageQueryDTO.getPageSize() == null ? DEFAULT_PAGE_SIZE
                : workHourPageQueryDTO.getPageSize();
        PageHelper.startPage(page, pageSize);
        Page<WorkHourVO> p = workHourMapper.pageQuery(workHourPageQueryDTO);
        return new PageResult(p.getTotal(), p.getResult());
    }

    /**
     * 岗位查询（不存在直接抛业务异常）
     */
    private Position getPosition(Long positionId) {
        Position position = workHourMapper.getPositionById(positionId);
        if (position == null) {
            throw new BaseException("岗位不存在");
        }
        return position;
    }

    /**
     * 工时规则全量校验（填报/修改/提交共用）
     * <p>
     * (a) end &gt; start，且跨度须为半小时（30 分钟）的整数倍；hours = 秒差 / 3600，保留 2 位小数（HALF_UP）
     * (b) 同学生同日期时段不得与已有记录（status≠3）重叠：start &lt; exist.end AND end &gt; exist.start
     * (c) work_date 不得为未来日期
     * (d) work_date 须落在当前学期 start_date ~ end_date 内
     * (e) 日/周（周一为始）/月累计 + 本次 不得超过 work.check.* 阈值
     * </p>
     *
     * @param workHour 待校验工时（hours 会被重算覆盖）
     * @param excludeId 修改/提交时排除自身，新增传 null
     */
    private void validate(WorkHour workHour, Long excludeId) {
        if (workHour.getStudentId() == null) {
            throw new BaseException("工时缺少学生信息");
        }
        LocalDate workDate = workHour.getWorkDate();
        LocalTime startTime = workHour.getStartTime();
        LocalTime endTime = workHour.getEndTime();
        if (workDate == null || startTime == null || endTime == null) {
            throw new BaseException("工作日期、开始时间、结束时间为必填项");
        }

        // (a) 时段与工时数
        int startSecond = startTime.toSecondOfDay();
        int endSecond = endTime.toSecondOfDay();
        if (endSecond <= startSecond) {
            throw new BaseException("结束时间必须晚于开始时间");
        }
        int spanMinutes = (endSecond - startSecond) / 60;
        if (spanMinutes % MIN_UNIT_MINUTES != 0) {
            throw new BaseException("工时以半小时为最小单位，起止时间跨度须为 30 分钟的整数倍");
        }
        //秒差 / 3600，保留 2 位小数（HALF_UP）
        BigDecimal hours = BigDecimal.valueOf(endSecond - startSecond)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
        workHour.setHours(hours);

        // (c) 禁未来日期
        if (workDate.isAfter(LocalDate.now())) {
            throw new BaseException("不能填报未来日期的工时");
        }

        // (d) 须落在当前学期内
        SysSemester semester = semesterService.getCurrent();
        if (semester == null) {
            throw new BaseException("当前未设置学期，无法填报工时");
        }
        if (semester.getStartDate() != null && workDate.isBefore(semester.getStartDate())) {
            throw new BaseException("工作日期须在当前学期内（" + semester.getStartDate() + " 至 " + semester.getEndDate() + "）");
        }
        if (semester.getEndDate() != null && workDate.isAfter(semester.getEndDate())) {
            throw new BaseException("工作日期须在当前学期内（" + semester.getStartDate() + " 至 " + semester.getEndDate() + "）");
        }

        // (b) 时段重叠
        int overlap = workHourMapper.countOverlap(workHour.getStudentId(), workDate, startTime, endTime, excludeId);
        if (overlap > 0) {
            throw new BaseException("该学生在 " + workDate + " 的该时段已存在工时记录（时段重叠）");
        }

        // (e) 日 / 周 / 月 上限
        BigDecimal daily = nz(workHourMapper.sumHours(workHour.getStudentId(), workDate, workDate, excludeId));
        checkMaxHours("当日", daily.add(hours), checkProperties.getDailyMaxHours());

        //自然周，周一为起始日
        LocalDate weekStart = workDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        BigDecimal weekly = nz(workHourMapper.sumHours(workHour.getStudentId(), weekStart, weekEnd, excludeId));
        checkMaxHours("本周", weekly.add(hours), checkProperties.getWeeklyMaxHours());

        LocalDate monthStart = workDate.withDayOfMonth(1);
        LocalDate monthEnd = workDate.withDayOfMonth(workDate.lengthOfMonth());
        BigDecimal monthly = nz(workHourMapper.sumHours(workHour.getStudentId(), monthStart, monthEnd, excludeId));
        checkMaxHours("本月", monthly.add(hours), checkProperties.getMonthlyMaxHours());
    }

    /**
     * 上限比较：total（累计+本次）不得超过 max
     */
    private void checkMaxHours(String label, BigDecimal total, BigDecimal max) {
        if (max != null && total.compareTo(max) > 0) {
            throw new BaseException("本次填报后" + label + "累计工时为 " + total + " 小时，超过上限 " + max + " 小时");
        }
    }

    /**
     * 驳回必须填写驳回原因
     */
    private void checkRejectReason(int targetStatus, String rejectReason) {
        if (targetStatus == STATUS_REJECTED && (rejectReason == null || rejectReason.trim().isEmpty())) {
            throw new BaseException("驳回时必须填写驳回原因");
        }
    }

    /**
     * 数据归属：仅学生本人（或超管）可操作
     */
    private void checkStudentOwner(WorkHour workHour) {
        if (dataScopeService.isSuperAdmin()) {
            return;
        }
        Long studentId = dataScopeService.getCurrentStudentId();
        if (studentId == null || !studentId.equals(workHour.getStudentId())) {
            throw new BaseException("无权操作他人的工时记录");
        }
    }

    /**
     * 删除权限：学生本人 / 本部门管理员 / 超管
     */
    private void checkDeletePermission(WorkHour workHour) {
        if (dataScopeService.isSuperAdmin()) {
            return;
        }
        if (dataScopeService.isDeptAdmin()) {
            checkDeptScopeStrict(workHour.getDeptId());
            return;
        }
        Long studentId = dataScopeService.getCurrentStudentId();
        if (studentId != null && studentId.equals(workHour.getStudentId())) {
            return;
        }
        throw new BaseException("无权删除该工时记录");
    }

    /**
     * 部门数据归属严格校验：记录未归属部门时同样拒绝
     * <p>注意 DataScopeService.checkDeptScope(null) 会直接放行，故此处先拦截 null，避免越权。</p>
     */
    private void checkDeptScopeStrict(Long deptId) {
        if (deptId == null) {
            throw new BaseException("该工时未归属用工部门，无权访问");
        }
        dataScopeService.checkDeptScope(deptId);
    }

    /**
     * 空值转 0
     */
    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 工时审核结果 WebSocket 群发（type=3），失败不影响主业务
     */
    private void pushWorkHourNotice(WorkHour workHour, int targetStatus, String stage) {
        try {
            String result;
            if (targetStatus == STATUS_PASSED) {
                result = "复核通过（工时已锁定）";
            } else if (targetStatus == STATUS_REJECTED) {
                result = "已驳回";
            } else {
                result = "部门审核通过，待中心复核";
            }
            Map<String, Object> message = new HashMap<>();
            message.put("type", 3);
            message.put("content", "工时" + stage + "通知：工时单 #" + workHour.getId()
                    + "（日期 " + workHour.getWorkDate() + " " + workHour.getStartTime()
                    + "-" + workHour.getEndTime() + "，工时 " + workHour.getHours() + " 小时）" + result);
            webSocketServer.sendToAllClient(JSON.toJSONString(message));
        } catch (Exception e) {
            log.error("工时审核 WebSocket 推送失败：{}", e.getMessage());
        }
    }

}
