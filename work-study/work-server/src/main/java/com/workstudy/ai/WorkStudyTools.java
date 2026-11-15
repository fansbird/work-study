package com.workstudy.ai;

import com.workstudy.dto.PositionPageQueryDTO;
import com.workstudy.entity.SalaryRule;
import com.workstudy.properties.CheckProperties;
import com.workstudy.service.PositionService;
import com.workstudy.service.SalaryRuleService;
import com.workstudy.vo.PositionVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 可调用的工具（Function Calling）
 * <p>模型根据用户问题自行决定调用哪个方法、传什么参数；
 * 方法内部复用系统原有 Service，因此返回的是数据库里的真实数据，而不是模型编造。</p>
 */
@Slf4j
@Component("workStudyTools")
public class WorkStudyTools {

    @Autowired
    private PositionService positionService;

    @Autowired
    private SalaryRuleService salaryRuleService;

    @Autowired
    private CheckProperties checkProperties;

    /**
     * 查询在招岗位
     */
    @Tool("查询当前在招的勤工助学岗位，可按岗位名称关键词过滤；返回岗位名称、时薪、剩余名额、工作地点")
    public String searchOpenPositions(@P(value = "岗位名称关键词，可为空", required = false) String keyword) {
        log.info("[AI工具] 查询在招岗位, keyword={}", keyword);
        PositionPageQueryDTO dto = new PositionPageQueryDTO();
        if (StringUtils.hasText(keyword)) {
            dto.setName(keyword.trim());
        }
        List<PositionVO> list = positionService.list(dto);
        if (list == null || list.isEmpty()) {
            return "当前没有查询到在招岗位。";
        }
        return list.stream().limit(10).map(p -> String.format(
                        "岗位ID=%d，名称=%s，部门=%s，时薪=%s元/小时，名额=%s人，已录用=%s人，剩余=%s人，地点=%s",
                        p.getId(), p.getName(),
                        p.getDeptName() == null ? "-" : p.getDeptName(),
                        p.getHourlyWage(), p.getQuota(), p.getHired(),
                        p.getRemainQuota() == null ? (p.getQuota() == null || p.getHired() == null ? "-" : String.valueOf(p.getQuota() - p.getHired())) : p.getRemainQuota(),
                        p.getWorkLocation() == null ? "-" : p.getWorkLocation()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 查询岗位详情
     */
    @Tool("按岗位ID查询岗位详情，包括工作内容、工作地点、联系人、时薪与剩余名额")
    public String getPositionDetail(@P("岗位ID") Long positionId) {
        log.info("[AI工具] 查询岗位详情, id={}", positionId);
        if (positionId == null) {
            return "请提供岗位ID。";
        }
        try {
            PositionVO p = positionService.getById(positionId);
            return String.format("岗位：%s\n工作内容：%s\n工作地点：%s\n时薪：%s元/小时\n名额：%s人，已录用：%s人\n联系人：%s（%s）",
                    p.getName(),
                    p.getWorkContent() == null ? "-" : p.getWorkContent(),
                    p.getWorkLocation() == null ? "-" : p.getWorkLocation(),
                    p.getHourlyWage(), p.getQuota(), p.getHired(),
                    p.getContactPerson() == null ? "-" : p.getContactPerson(),
                    p.getContactPhone() == null ? "-" : p.getContactPhone());
        } catch (Exception e) {
            return "没有找到该岗位（" + e.getMessage() + "）。";
        }
    }

    /**
     * 工时规则
     */
    @Tool("查询勤工助学的工时上限规则：每日、每周、每月最多可申报多少小时")
    public String getWorkHourRules() {
        log.info("[AI工具] 查询工时规则");
        return String.format("工时申报上限：每日 %s 小时，每周 %s 小时，每月 %s 小时；同一学生同一天的时段不能重叠，超过上限会被规则校验拦截。",
                checkProperties.getDailyMaxHours(),
                checkProperties.getWeeklyMaxHours(),
                checkProperties.getMonthlyMaxHours());
    }

    /**
     * 薪酬规则
     */
    @Tool("查询薪酬规则：不同岗位类别/困难等级的时薪与系数")
    public String getSalaryRules() {
        log.info("[AI工具] 查询薪酬规则");
        List<SalaryRule> rules = salaryRuleService.list(null);
        if (rules == null || rules.isEmpty()) {
            return "当前没有配置薪酬规则。";
        }
        return rules.stream().limit(10).map(r -> String.format(
                        "规则=%s，类别=%s，困难等级=%s，基础时薪=%s元，系数=%s，优先级=%s",
                        r.getName(),
                        r.getCategory() == null ? "通用" : r.getCategory(),
                        r.getDifficultyLevel() == null ? "不限" : r.getDifficultyLevel(),
                        r.getBaseWage(), r.getCoefficient(), r.getPriority()))
                .collect(Collectors.joining("\n"));
    }
}