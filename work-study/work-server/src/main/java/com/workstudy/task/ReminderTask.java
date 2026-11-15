package com.workstudy.task;

import com.alibaba.fastjson.JSON;
import com.workstudy.mapper.ReportMapper;
import com.workstudy.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知提醒定时任务（M7）
 *
 * 1) 每天 09:00 统计待部门审核/待中心复核工时数、待确认结算单数，>0 时群发 type=5 提醒；
 * 2) 每月 1 日 09:05 提醒生成上月结算单，群发 type=5。
 */
@Component
@Slf4j
public class ReminderTask {

    /**
     * 消息类型：5 定时提醒
     */
    private static final int TYPE_REMINDER = 5;

    /**
     * 工时状态：0 待部门审核
     */
    private static final int WORK_HOUR_DEPT_PENDING = 0;

    /**
     * 工时状态：1 待中心复核
     */
    private static final int WORK_HOUR_CENTER_PENDING = 1;

    /**
     * 结算单状态：0 待确认
     */
    private static final int SETTLEMENT_PENDING = 0;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 每日待办提醒：每天 09:00 执行
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void remindPending() {
        log.info("定时任务：统计待办并发送提醒");
        long deptPending = count(reportMapper.countWorkHourByStatus(WORK_HOUR_DEPT_PENDING));
        long centerPending = count(reportMapper.countWorkHourByStatus(WORK_HOUR_CENTER_PENDING));
        long settlementPending = count(reportMapper.countSettlementByStatus(SETTLEMENT_PENDING));

        if (deptPending + centerPending + settlementPending <= 0) {
            log.info("定时任务：当前无待办事项，不发送提醒");
            return;
        }

        String content = "【待办提醒】待部门审核工时 " + deptPending + " 条，待中心复核工时 "
                + centerPending + " 条，待确认结算单 " + settlementPending + " 张，请及时处理。";
        send(content);
    }

    /**
     * 月度结算提醒：每月 1 日 09:05 执行
     */
    @Scheduled(cron = "0 5 9 1 * ?")
    public void remindGenerateSettlement() {
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("定时任务：提醒生成上月（{}）结算单", lastMonth);
        String content = "【月度结算提醒】新的一个月已开始，请及时生成上月（" + lastMonth + "）的结算单。";
        send(content);
    }

    /**
     * 群发 type=5 提醒消息
     */
    private void send(String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", TYPE_REMINDER);
        message.put("content", content);
        webSocketServer.sendToAllClient(JSON.toJSONString(message));
        log.info("定时提醒已发送：{}", content);
    }

    /**
     * 空计数值归一为 0
     */
    private long count(Long value) {
        return value == null ? 0L : value;
    }

}
