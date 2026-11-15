package com.workstudy.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.context.BaseContext;
import com.workstudy.dto.SettlementAdjustDTO;
import com.workstudy.dto.SettlementGenerateDTO;
import com.workstudy.dto.SettlementPageQueryDTO;
import com.workstudy.entity.Position;
import com.workstudy.entity.SalaryRule;
import com.workstudy.entity.Settlement;
import com.workstudy.entity.SettlementAdjust;
import com.workstudy.entity.SettlementDetail;
import com.workstudy.entity.Student;
import com.workstudy.entity.SysSemester;
import com.workstudy.entity.WorkHour;
import com.workstudy.exception.BaseException;
import com.workstudy.mapper.SalaryRuleMapper;
import com.workstudy.mapper.SettlementAdjustMapper;
import com.workstudy.mapper.SettlementDetailMapper;
import com.workstudy.mapper.SettlementMapper;
import com.workstudy.mapper.StudentMapper;
import com.workstudy.result.PageResult;
import com.workstudy.service.DataScopeService;
import com.workstudy.service.SemesterService;
import com.workstudy.service.SettlementService;
import com.workstudy.vo.SettlementDetailVO;
import com.workstudy.vo.SettlementVO;
import com.workstudy.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 薪酬结算服务实现（M5：BigDecimal 对账 + 批次幂等 + 发放锁定）
 *
 * <p>结算状态机（契约第 3 节）：</p>
 * <pre>
 *   0 待确认 --确认(中心)--> 1 已确认 --发放(财务)--> 2 已发放（锁定，不可再改）
 *   0/1 --作废--> 3 已作废（解绑其下工时的 settlement_id，可重新生成）
 * </pre>
 */
@Service
@Slf4j
public class SettlementServiceImpl implements SettlementService {

    /**
     * 结算单：待确认
     */
    private static final int SETTLE_PENDING = 0;

    /**
     * 结算单：已确认
     */
    private static final int SETTLE_CONFIRMED = 1;

    /**
     * 结算单：已发放（锁定）
     */
    private static final int SETTLE_PAID = 2;

    /**
     * 结算单：已作废
     */
    private static final int SETTLE_VOID = 3;

    /**
     * 调整类型：补发
     */
    private static final int ADJUST_ADD = 1;

    /**
     * 调整类型：扣减
     */
    private static final int ADJUST_SUBTRACT = 2;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    private SettlementMapper settlementMapper;

    @Autowired
    private SettlementDetailMapper settlementDetailMapper;

    @Autowired
    private SettlementAdjustMapper settlementAdjustMapper;

    @Autowired
    private SalaryRuleMapper salaryRuleMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 生成月度结算批次。
     * <p>核心流程：权限 → 幂等校验 → 取可结算工时（status=2 且未结算）→ 逐条匹配薪酬规则并计算金额
     * → 按学生+月封顶/保底 → 生成批次号 → 写 settlement + settlement_detail → 回填 work_hour.settlement_id
     * → 读库对账（∑明细 == 结算单总额/总工时/条数），不一致抛异常整体回滚。</p>
     *
     * @param settlementGenerateDTO
     * @return
     */
    @Override
    @Transactional
    public Settlement generate(SettlementGenerateDTO settlementGenerateDTO) {
        checkManagePermission();
        if (settlementGenerateDTO == null) {
            throw new BaseException("结算生成参数不能为空");
        }

        //1、学期：未指定时取当前学期
        Long semesterId = settlementGenerateDTO.getSemesterId();
        if (semesterId == null) {
            SysSemester current = semesterService.getCurrent();
            if (current == null) {
                throw new BaseException("当前未设置学期，无法生成结算单");
            }
            semesterId = current.getId();
        }

        //2、月份格式校验
        String month = settlementGenerateDTO.getMonth();
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BaseException("结算月份格式不正确，应为 yyyy-MM，如 2026-10");
        }
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (Exception e) {
            throw new BaseException("结算月份格式不正确，应为 yyyy-MM，如 2026-10");
        }

        //3、数据归属：指定部门时校验越权
        Long deptId = settlementGenerateDTO.getDeptId();
        if (deptId != null) {
            dataScopeService.checkDeptScope(deptId);
        }

        //4、幂等：同 semester + month + dept 已存在 status≠3 的批次则拒绝
        if (settlementMapper.countByScope(semesterId, month, deptId) > 0) {
            throw new BaseException("该学期 " + month + " 月的结算批次已存在（未作废），请勿重复生成");
        }

        //5、取可结算工时
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        List<WorkHour> candidates = settlementMapper.listPendingWorkHours(semesterId, startDate, endDate, deptId);
        if (candidates == null || candidates.isEmpty()) {
            throw new BaseException("该范围内没有可结算的工时（需为审核通过且未结算的工时）");
        }

        //6、薪酬规则（启用中，按 priority 升序：数字小者优先）
        List<SalaryRule> rules = salaryRuleMapper.list(semesterId);

        //7、逐条匹配规则并用 BigDecimal 计算金额
        Map<Long, Position> positionCache = new HashMap<>();
        Map<Long, Student> studentCache = new HashMap<>();
        Map<Long, SalaryRule> studentMonthlyRule = new HashMap<>();
        Map<Long, Integer> studentRuleScore = new HashMap<>();
        Map<Long, List<SettlementDetail>> detailsByStudent = new LinkedHashMap<>();
        List<SettlementDetail> details = new ArrayList<>();

        for (WorkHour workHour : candidates) {
            Position position = positionCache.computeIfAbsent(workHour.getPositionId(),
                    settlementMapper::getPositionById);
            Student student = studentCache.computeIfAbsent(workHour.getStudentId(), studentMapper::getById);
            String category = position == null ? null : position.getCategory();
            Integer difficulty = student == null ? null : student.getDifficultyLevel();

            SalaryRule rule = matchRule(rules, workHour.getSemesterId(), category, difficulty);
            if (rule != null) {
                int score = ruleScore(rule);
                Integer oldScore = studentRuleScore.get(workHour.getStudentId());
                if (oldScore == null || score > oldScore) {
                    studentRuleScore.put(workHour.getStudentId(), score);
                    studentMonthlyRule.put(workHour.getStudentId(), rule);
                }
            }

            //命中规则用 base_wage/coefficient；未命中用岗位 hourly_wage、coefficient=1
            BigDecimal wage;
            BigDecimal coefficient;
            if (rule != null) {
                wage = rule.getBaseWage();
                coefficient = rule.getCoefficient() == null ? BigDecimal.ONE : rule.getCoefficient();
            } else {
                wage = position == null ? null : position.getHourlyWage();
                coefficient = BigDecimal.ONE;
            }
            if (wage == null) {
                wage = BigDecimal.ZERO;
            }

            BigDecimal hours = workHour.getHours() == null ? BigDecimal.ZERO : workHour.getHours();
            hours = hours.setScale(2, RoundingMode.HALF_UP);
            wage = wage.setScale(2, RoundingMode.HALF_UP);
            //amount = hours × 时薪 × 系数，保留 2 位小数（HALF_UP）
            BigDecimal amount = hours.multiply(wage).multiply(coefficient).setScale(2, RoundingMode.HALF_UP);

            SettlementDetail detail = SettlementDetail.builder()
                    .workHourId(workHour.getId())
                    .studentId(workHour.getStudentId())
                    .positionId(workHour.getPositionId())
                    .workDate(workHour.getWorkDate())
                    .hours(hours)
                    .hourlyWage(wage)
                    .coefficient(coefficient)
                    .amount(amount)
                    .build();
            details.add(detail);
            detailsByStudent.computeIfAbsent(workHour.getStudentId(), k -> new ArrayList<>()).add(detail);
        }

        //8、按「学生 + 月」应用规则封顶/保底（按比例摊平到明细，保证 ∑detail.amount == 结算单总额）
        for (Map.Entry<Long, List<SettlementDetail>> entry : detailsByStudent.entrySet()) {
            applyCapAndFloor(entry.getValue(), studentMonthlyRule.get(entry.getKey()));
        }

        //9、汇总总额（以摊平后的明细为准）
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SettlementDetail detail : details) {
            totalHours = totalHours.add(detail.getHours());
            totalAmount = totalAmount.add(detail.getAmount());
        }
        totalHours = totalHours.setScale(2, RoundingMode.HALF_UP);
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        //10、批次号：SETTLE_年月-序号（保证唯一）
        String batchNo = generateBatchNo(month);

        //11、写结算单
        Settlement settlement = Settlement.builder()
                .batchNo(batchNo)
                .semesterId(semesterId)
                .month(month)
                .deptId(deptId)
                .totalHours(totalHours)
                .totalAmount(totalAmount)
                .detailCount(details.size())
                .status(SETTLE_PENDING)
                .build();
        settlementMapper.insert(settlement);
        Long settlementId = settlement.getId();

        //12、写明细并回填工时 settlement_id
        LocalDateTime now = LocalDateTime.now();
        for (SettlementDetail detail : details) {
            detail.setSettlementId(settlementId);
            detail.setCreateTime(now);
        }
        settlementDetailMapper.insertBatch(details);

        List<Long> workHourIds = new ArrayList<>();
        for (WorkHour workHour : candidates) {
            workHourIds.add(workHour.getId());
        }
        settlementMapper.fillSettlementId(settlementId, workHourIds);

        //13、对账：从数据库汇总明细，必须与结算单总额/总工时/条数完全一致
        reconcile(settlement, settlementId);

        //14、业务节点：结算状态 WebSocket 群发（type=4）
        pushSettlementNotice("结算单已生成（待确认）", settlement);

        log.info("结算批次生成成功：批次号 {}，明细 {} 条，总工时 {}，总金额 {}",
                batchNo, details.size(), totalHours, totalAmount);
        return settlement;
    }

    /**
     * 分页查询结算单
     *
     * @param settlementPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SettlementPageQueryDTO settlementPageQueryDTO) {
        int page = settlementPageQueryDTO.getPage() == null ? DEFAULT_PAGE : settlementPageQueryDTO.getPage();
        int pageSize = settlementPageQueryDTO.getPageSize() == null ? DEFAULT_PAGE_SIZE
                : settlementPageQueryDTO.getPageSize();
        PageHelper.startPage(page, pageSize);

        Page<SettlementVO> p;
        if (dataScopeService.isStudent()) {
            //数据归属：学生仅能看到包含本人明细的结算单
            Long studentId = dataScopeService.getCurrentStudentId();
            if (studentId == null) {
                return new PageResult(0, new ArrayList<>());
            }
            p = settlementMapper.pageQueryForStudent(settlementPageQueryDTO, studentId);
        } else {
            //数据归属：部门管理员限本部门（账号未绑定部门时返回空页，不能退化为全库可见）
            if (dataScopeService.isDeptAdmin()) {
                Long deptId = dataScopeService.getCurrentDeptId();
                if (deptId == null) {
                    return new PageResult(0, new ArrayList<>());
                }
                settlementPageQueryDTO.setDeptId(deptId);
            } else if (settlementPageQueryDTO.getDeptId() != null) {
                dataScopeService.checkDeptScope(settlementPageQueryDTO.getDeptId());
            }
            p = settlementMapper.pageQuery(settlementPageQueryDTO);
        }
        return new PageResult(p.getTotal(), p.getResult());
    }

    /**
     * 根据id查询结算单
     *
     * @param id
     * @return
     */
    @Override
    public SettlementVO getById(Long id) {
        SettlementVO settlementVO = settlementMapper.getVoById(id);
        if (settlementVO == null) {
            throw new BaseException("结算单不存在");
        }
        checkViewPermission(settlementVO.getDeptId(), id);
        return settlementVO;
    }

    /**
     * 查询结算明细
     *
     * @param settlementId
     * @return
     */
    @Override
    public List<SettlementDetailVO> listDetail(Long settlementId) {
        Settlement settlement = settlementMapper.getById(settlementId);
        if (settlement == null) {
            throw new BaseException("结算单不存在");
        }
        checkViewPermission(settlement.getDeptId(), settlementId);
        return settlementDetailMapper.listBySettlementId(settlementId);
    }

    /**
     * 确认结算单：0 → 1（中心）
     *
     * @param id
     */
    @Override
    @Transactional
    public void confirm(Long id) {
        checkManagePermission();
        Settlement settlement = settlementMapper.getById(id);
        if (settlement == null) {
            throw new BaseException("结算单不存在");
        }
        if (!Integer.valueOf(SETTLE_PENDING).equals(settlement.getStatus())) {
            throw new BaseException("仅待确认状态的结算单可以确认");
        }
        Settlement update = new Settlement();
        update.setId(id);
        update.setStatus(SETTLE_CONFIRMED);
        update.setConfirmUser(BaseContext.getCurrentId());
        update.setConfirmTime(LocalDateTime.now());
        settlementMapper.update(update);

        pushSettlementNotice("结算单已确认（待发放）", settlement);
        log.info("结算单确认成功，id：{}，批次号：{}", id, settlement.getBatchNo());
    }

    /**
     * 发放结算单：1 → 2（财务，发放后锁定不可修改）
     *
     * @param id
     */
    @Override
    @Transactional
    public void pay(Long id) {
        if (!(dataScopeService.isFinance() || dataScopeService.isSuperAdmin())) {
            throw new BaseException("仅财务角色可发放结算单");
        }
        Settlement settlement = settlementMapper.getById(id);
        if (settlement == null) {
            throw new BaseException("结算单不存在");
        }
        if (!Integer.valueOf(SETTLE_CONFIRMED).equals(settlement.getStatus())) {
            throw new BaseException("仅已确认状态的结算单可以发放");
        }
        Settlement update = new Settlement();
        update.setId(id);
        update.setStatus(SETTLE_PAID);
        update.setPayUser(BaseContext.getCurrentId());
        update.setPayTime(LocalDateTime.now());
        settlementMapper.update(update);

        pushSettlementNotice("结算单已发放（发放后锁定）", settlement);
        log.info("结算单发放成功，id：{}，批次号：{}", id, settlement.getBatchNo());
    }

    /**
     * 作废结算单：0/1 → 3，并解绑其下工时的 settlement_id、删除明细以释放 uk_work_hour_id，便于重算
     *
     * @param id
     */
    @Override
    @Transactional
    public void voidSettlement(Long id) {
        checkManagePermission();
        Settlement settlement = settlementMapper.getById(id);
        if (settlement == null) {
            throw new BaseException("结算单不存在");
        }
        if (!(Integer.valueOf(SETTLE_PENDING).equals(settlement.getStatus())
                || Integer.valueOf(SETTLE_CONFIRMED).equals(settlement.getStatus()))) {
            throw new BaseException("仅待确认或已确认状态的结算单可以作废");
        }

        Settlement update = new Settlement();
        update.setId(id);
        update.setStatus(SETTLE_VOID);
        settlementMapper.update(update);

        //解绑工时（重新变为可结算），并删除明细以释放 work_hour_id 唯一键，保证同批工时可重新结算
        settlementMapper.clearSettlementIdBySettlementId(id);
        settlementDetailMapper.deleteBySettlementId(id);

        pushSettlementNotice("结算单已作废（工时已解绑，可重新生成）", settlement);
        log.info("结算单作废成功，id：{}，批次号：{}", id, settlement.getBatchNo());
    }

    /**
     * 结算调整：写入 settlement_adjust 并同步调整结算单总额（发放后锁定，不可调整）
     *
     * @param settlementAdjustDTO
     */
    @Override
    @Transactional
    public void adjust(SettlementAdjustDTO settlementAdjustDTO) {
        checkManagePermission();
        if (settlementAdjustDTO == null || settlementAdjustDTO.getSettlementId() == null) {
            throw new BaseException("结算单id不能为空");
        }
        if (settlementAdjustDTO.getStudentId() == null) {
            throw new BaseException("请选择调整的学生");
        }
        Integer adjustType = settlementAdjustDTO.getAdjustType();
        if (!(Integer.valueOf(ADJUST_ADD).equals(adjustType) || Integer.valueOf(ADJUST_SUBTRACT).equals(adjustType))) {
            throw new BaseException("调整类型不合法（1=补发，2=扣减）");
        }
        if (settlementAdjustDTO.getAmount() == null
                || settlementAdjustDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException("调整金额必须大于 0");
        }

        Settlement settlement = settlementMapper.getById(settlementAdjustDTO.getSettlementId());
        if (settlement == null) {
            throw new BaseException("结算单不存在");
        }
        if (Integer.valueOf(SETTLE_PAID).equals(settlement.getStatus())) {
            throw new BaseException("结算单已发放并锁定，不能调整");
        }
        if (Integer.valueOf(SETTLE_VOID).equals(settlement.getStatus())) {
            throw new BaseException("已作废的结算单不能调整");
        }

        BigDecimal amount = settlementAdjustDTO.getAmount().setScale(2, RoundingMode.HALF_UP);
        SettlementAdjust adjust = SettlementAdjust.builder()
                .settlementId(settlement.getId())
                .studentId(settlementAdjustDTO.getStudentId())
                .adjustType(adjustType)
                .amount(amount)
                .reason(settlementAdjustDTO.getReason())
                .createUser(BaseContext.getCurrentId())
                .createTime(LocalDateTime.now())
                .build();
        settlementAdjustMapper.insert(adjust);

        BigDecimal base = settlement.getTotalAmount() == null ? BigDecimal.ZERO : settlement.getTotalAmount();
        BigDecimal newTotal = adjustType == ADJUST_ADD ? base.add(amount) : base.subtract(amount);
        newTotal = newTotal.setScale(2, RoundingMode.HALF_UP);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BaseException("扣减后结算单总金额不能为负");
        }

        Settlement update = new Settlement();
        update.setId(settlement.getId());
        update.setTotalAmount(newTotal);
        settlementMapper.update(update);

        log.info("结算调整成功，结算单id：{}，类型：{}，金额：{}，总额 {} → {}",
                settlement.getId(), adjustType, amount, base, newTotal);
    }

    /**
     * 导出结算明细（Apache POI，xlsx）：明细 sheet + 调整记录 sheet
     *
     * @param id
     * @param response
     */
    @Override
    public void export(Long id, HttpServletResponse response) {
        SettlementVO settlement = settlementMapper.getVoById(id);
        if (settlement == null) {
            throw new BaseException("结算单不存在");
        }
        checkViewPermission(settlement.getDeptId(), id);
        List<SettlementDetailVO> details = settlementDetailMapper.listBySettlementId(id);
        List<SettlementAdjust> adjusts = settlementAdjustMapper.listBySettlementId(id);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            //========== Sheet1 结算明细 ==========
            XSSFSheet sheet = workbook.createSheet("结算明细");
            int[] widths = {8, 16, 14, 26, 14, 10, 10, 8, 12};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIndex = 0;
            XSSFRow titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue("结算明细表  批次号：" + nvl(settlement.getBatchNo()));
            XSSFRow metaRow = sheet.createRow(rowIndex++);
            metaRow.createCell(0).setCellValue("学期：" + nvl(settlement.getSemesterName())
                    + "    部门：" + (settlement.getDeptId() == null ? "全校" : nvl(settlement.getDeptName()))
                    + "    月份：" + nvl(settlement.getMonth())
                    + "    状态：" + settlementStatusText(settlement.getStatus()));

            String[] headers = {"序号", "学号", "姓名", "岗位", "工作日期", "工时", "时薪", "系数", "金额"};
            XSSFRow headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int index = 1;
            double sumHours = 0D;
            double sumAmount = 0D;
            for (SettlementDetailVO detail : details) {
                XSSFRow row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(index++);
                row.createCell(1).setCellValue(nvl(detail.getStudentNo()));
                row.createCell(2).setCellValue(nvl(detail.getStudentName()));
                row.createCell(3).setCellValue(nvl(detail.getPositionName()));
                row.createCell(4).setCellValue(detail.getWorkDate() == null ? "" : detail.getWorkDate().toString());
                row.createCell(5).setCellValue(num(detail.getHours()));
                row.createCell(6).setCellValue(num(detail.getHourlyWage()));
                row.createCell(7).setCellValue(num(detail.getCoefficient()));
                Cell amountCell = row.createCell(8);
                amountCell.setCellValue(num(detail.getAmount()));
                amountCell.setCellStyle(moneyStyle);
                sumHours += num(detail.getHours());
                sumAmount += num(detail.getAmount());
            }
            XSSFRow totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(0).setCellValue("合计");
            totalRow.createCell(5).setCellValue(sumHours);
            Cell totalAmountCell = totalRow.createCell(8);
            totalAmountCell.setCellValue(sumAmount);
            totalAmountCell.setCellStyle(moneyStyle);

            //========== Sheet2 调整记录 ==========
            if (!adjusts.isEmpty()) {
                XSSFSheet adjustSheet = workbook.createSheet("调整记录");
                String[] adjustHeaders = {"序号", "学生ID", "类型", "金额", "原因", "时间"};
                int[] adjustWidths = {8, 14, 10, 12, 40, 22};
                for (int i = 0; i < adjustWidths.length; i++) {
                    adjustSheet.setColumnWidth(i, adjustWidths[i] * 256);
                }
                XSSFRow adjustHeaderRow = adjustSheet.createRow(0);
                for (int i = 0; i < adjustHeaders.length; i++) {
                    Cell cell = adjustHeaderRow.createCell(i);
                    cell.setCellValue(adjustHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                int adjustIndex = 1;
                int adjustRowIndex = 1;
                for (SettlementAdjust adjust : adjusts) {
                    XSSFRow row = adjustSheet.createRow(adjustRowIndex++);
                    row.createCell(0).setCellValue(adjustIndex++);
                    row.createCell(1).setCellValue(adjust.getStudentId() == null ? 0D : adjust.getStudentId());
                    row.createCell(2).setCellValue(Integer.valueOf(ADJUST_ADD).equals(adjust.getAdjustType())
                            ? "补发" : "扣减");
                    Cell adjustAmountCell = row.createCell(3);
                    adjustAmountCell.setCellValue(num(adjust.getAmount()));
                    adjustAmountCell.setCellStyle(moneyStyle);
                    row.createCell(4).setCellValue(nvl(adjust.getReason()));
                    row.createCell(5).setCellValue(adjust.getCreateTime() == null ? ""
                            : adjust.getCreateTime().toString());
                }
            }

            //========== 输出到响应流 ==========
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename="
                    + URLEncoder.encode("结算明细_" + nvl(settlement.getBatchNo()) + ".xlsx", "UTF-8"));
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            log.info("结算明细导出成功，批次号：{}，明细 {} 条", settlement.getBatchNo(), details.size());
        } catch (IOException e) {
            log.error("结算明细导出失败：{}", e.getMessage());
            throw new BaseException("结算明细导出失败：" + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 生成批次号：SETTLE_年月-序号（如 SETTLE_202610-01），保证唯一
     */
    private String generateBatchNo(String month) {
        String prefix = "SETTLE_" + month.replace("-", "") + "-";
        int seq = settlementMapper.countByBatchPrefix(prefix) + 1;
        String batchNo = prefix + String.format("%02d", seq);
        int guard = 0;
        while (settlementMapper.getByBatchNo(batchNo) != null && guard < 1000) {
            seq++;
            batchNo = prefix + String.format("%02d", seq);
            guard++;
        }
        return batchNo;
    }

    /**
     * 规则匹配：semester 命中优先、其次 category、其次 difficulty；同等具体度取 priority 小者。
     * <p>rules 已按 priority asc, id asc 返回，故「&gt;」比较天然保留优先级更小的规则。</p>
     *
     * @param rules       启用中的规则（含通用规则），按 priority 升序
     * @param semesterId  工时所属学期
     * @param category    岗位类别
     * @param difficulty  学生困难等级
     * @return 命中的最具体规则，未命中返回 null（调用方使用岗位 hourly_wage、coefficient=1）
     */
    private SalaryRule matchRule(List<SalaryRule> rules, Long semesterId, String category, Integer difficulty) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        SalaryRule best = null;
        int bestScore = -1;
        for (SalaryRule rule : rules) {
            if (rule.getSemesterId() != null && !rule.getSemesterId().equals(semesterId)) {
                continue;
            }
            if (rule.getCategory() != null && !rule.getCategory().isEmpty() && !rule.getCategory().equals(category)) {
                continue;
            }
            if (rule.getDifficultyLevel() != null && !rule.getDifficultyLevel().equals(difficulty)) {
                continue;
            }
            int score = ruleScore(rule);
            if (score > bestScore) {
                best = rule;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 规则具体度评分：学期命中 4 分、类别命中 2 分、困难等级命中 1 分
     */
    private int ruleScore(SalaryRule rule) {
        int score = 0;
        if (rule.getSemesterId() != null) {
            score += 4;
        }
        if (rule.getCategory() != null && !rule.getCategory().isEmpty()) {
            score += 2;
        }
        if (rule.getDifficultyLevel() != null) {
            score += 1;
        }
        return score;
    }

    /**
     * 按「学生 + 月」应用封顶/保底：按比例摊平到该学生的明细，最后一条吃掉舍入余额，
     * 保证 ∑detail.amount 精确等于目标金额（从而不影响对账）。
     */
    private void applyCapAndFloor(List<SettlementDetail> details, SalaryRule rule) {
        if (rule == null || details == null || details.isEmpty()) {
            return;
        }
        BigDecimal cap = rule.getMaxMonthly();
        BigDecimal floor = rule.getMinMonthly();
        if (cap == null && floor == null) {
            return;
        }

        BigDecimal rawSum = BigDecimal.ZERO;
        for (SettlementDetail detail : details) {
            rawSum = rawSum.add(detail.getAmount());
        }
        if (rawSum.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal target = rawSum;
        if (cap != null && target.compareTo(cap) > 0) {
            target = cap;
        }
        if (floor != null && target.compareTo(floor) < 0) {
            target = floor;
        }
        target = target.setScale(2, RoundingMode.HALF_UP);
        if (target.compareTo(rawSum.setScale(2, RoundingMode.HALF_UP)) == 0) {
            return;
        }

        //按比例摊平
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < details.size(); i++) {
            SettlementDetail detail = details.get(i);
            BigDecimal scaled;
            if (i == details.size() - 1) {
                scaled = target.subtract(allocated).setScale(2, RoundingMode.HALF_UP);
            } else {
                scaled = detail.getAmount().multiply(target).divide(rawSum, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(scaled);
            }
            detail.setAmount(scaled);
        }

        //兜底：把舍入残差补到金额最大的一条明细，确保合计精确等于 target
        BigDecimal sum = BigDecimal.ZERO;
        SettlementDetail biggest = details.get(0);
        for (SettlementDetail detail : details) {
            sum = sum.add(detail.getAmount());
            if (detail.getAmount().compareTo(biggest.getAmount()) > 0) {
                biggest = detail;
            }
        }
        BigDecimal diff = target.subtract(sum);
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            biggest.setAmount(biggest.getAmount().add(diff).setScale(2, RoundingMode.HALF_UP));
        }

        log.info("月度封顶/保底生效：规则[{}]，原合计 {} → 目标 {}（封顶 {}，保底 {}）",
                rule.getName(), rawSum, target, cap, floor);
    }

    /**
     * 对账：从数据库汇总明细，必须与结算单的总额/总工时/明细条数完全一致，否则抛异常整体回滚
     */
    private void reconcile(Settlement settlement, Long settlementId) {
        Map<String, Object> sum = settlementDetailMapper.sumBySettlementId(settlementId);
        BigDecimal dbAmount = toBigDecimal(mapGet(sum, "totalAmount"));
        BigDecimal dbHours = toBigDecimal(mapGet(sum, "totalHours"));
        long dbCount = toLong(mapGet(sum, "detailCount"));

        boolean amountOk = dbAmount.compareTo(settlement.getTotalAmount()) == 0;
        boolean hoursOk = dbHours.compareTo(settlement.getTotalHours()) == 0;
        boolean countOk = dbCount == settlement.getDetailCount().longValue();
        if (!amountOk || !hoursOk || !countOk) {
            throw new BaseException("结算对账失败：明细合计（金额 " + dbAmount + "、工时 " + dbHours + "、条数 "
                    + dbCount + "）与结算单（金额 " + settlement.getTotalAmount() + "、工时 "
                    + settlement.getTotalHours() + "、条数 " + settlement.getDetailCount() + "）不一致，已回滚");
        }
        log.info("结算对账通过：结算单id {}，金额 {}，工时 {}，明细 {} 条",
                settlementId, dbAmount, dbHours, dbCount);
    }

    /**
     * 管理权限：仅超级管理员或勤工助学中心管理员
     */
    private void checkManagePermission() {
        if (!(dataScopeService.isSuperAdmin() || dataScopeService.isCenterAdmin())) {
            throw new BaseException("仅超级管理员或勤工助学中心管理员可操作结算单");
        }
    }

    /**
     * 查看权限：超管/中心/财务可跨部门；部门管理员限本部门（结算单为全校 dept_id=null 时对其不可见）；
     * 学生仅可见含本人明细的结算单
     */
    private void checkViewPermission(Long deptId, Long settlementId) {
        if (dataScopeService.isSuperAdmin() || dataScopeService.isCenterAdmin() || dataScopeService.isFinance()) {
            return;
        }
        if (dataScopeService.isDeptAdmin()) {
            //DataScopeService.checkDeptScope(null) 会直接放行，此处先拦截，避免部门管理员看到全校结算单
            if (deptId == null) {
                throw new BaseException("无权查看该结算单");
            }
            dataScopeService.checkDeptScope(deptId);
            return;
        }
        Long studentId = dataScopeService.getCurrentStudentId();
        if (studentId != null
                && settlementDetailMapper.countBySettlementIdAndStudentId(settlementId, studentId) > 0) {
            return;
        }
        throw new BaseException("无权查看该结算单");
    }

    /**
     * 结算状态 WebSocket 群发（type=4），失败不影响主业务
     */
    private void pushSettlementNotice(String action, Settlement settlement) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", 4);
            message.put("content", action + "：批次号 " + settlement.getBatchNo() + "，月份 " + settlement.getMonth()
                    + "，总工时 " + settlement.getTotalHours() + " 小时，总金额 " + settlement.getTotalAmount() + " 元");
            webSocketServer.sendToAllClient(JSON.toJSONString(message));
        } catch (Exception e) {
            log.error("结算状态 WebSocket 推送失败：{}", e.getMessage());
        }
    }

    /**
     * 结算单状态中文
     */
    private String settlementStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case SETTLE_PENDING:
                return "待确认";
            case SETTLE_CONFIRMED:
                return "已确认";
            case SETTLE_PAID:
                return "已发放";
            case SETTLE_VOID:
                return "已作废";
            default:
                return String.valueOf(status);
        }
    }

    /**
     * 忽略大小写从 Map 取值（MyBatis 返回列标签大小写可能不同）
     */
    private Object mapGet(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Object → BigDecimal（按值比较，不受 scale 影响）
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    /**
     * Object → long
     */
    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    /**
     * BigDecimal → double（Excel 数值单元格用），null 视为 0
     */
    private double num(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    /**
     * null 安全的字符串
     */
    private String nvl(String value) {
        return value == null ? "" : value;
    }

}
