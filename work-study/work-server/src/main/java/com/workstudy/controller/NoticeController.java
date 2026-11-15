package com.workstudy.controller;

import com.alibaba.fastjson.JSON;
import com.workstudy.annotation.Log;
import com.workstudy.result.Result;
import com.workstudy.websocket.WebSocketServer;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息通知（WebSocket 群发）
 */
@RestController
@RequestMapping("/notice")
@Slf4j
@Tag(name = "消息通知接口")
public class NoticeController {

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 群发一条测试消息（type=5 定时提醒）
     *
     * @return
     */
    @PostMapping("/ping")
    @Operation(summary = "群发测试消息")
    @Log(title = "消息通知")
    public Result<Map<String, Object>> ping() {
        log.info("群发测试消息");
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", 5);
        message.put("content", "【测试】系统消息通道正常 "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        webSocketServer.sendToAllClient(JSON.toJSONString(message));
        return Result.success(message);
    }

    /**
     * 消息类型说明
     *
     * @return
     */
    @GetMapping("/types")
    @Operation(summary = "消息类型说明")
    public Result<List<Map<String, Object>>> types() {
        log.info("查询消息类型说明");
        List<Map<String, Object>> types = new ArrayList<>();
        types.add(type(1, "岗位审批结果"));
        types.add(type(2, "报名审核/录用"));
        types.add(type(3, "工时审核结果"));
        types.add(type(4, "结算状态（待确认/已发放）"));
        types.add(type(5, "定时提醒"));
        return Result.success(types);
    }

    /**
     * 组装一条类型说明
     */
    private Map<String, Object> type(Integer type, String desc) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("desc", desc);
        return item;
    }

}
