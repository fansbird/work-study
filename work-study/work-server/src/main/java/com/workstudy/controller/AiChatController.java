package com.workstudy.controller;

import com.workstudy.ai.WorkStudyAssistant;
import com.workstudy.context.BaseContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 助手对话接口（流式）
 */
@RestController
@RequestMapping("/ai")
@Slf4j
@Tag(name = "AI 智能助手")
public class AiChatController {

    @Autowired
    private WorkStudyAssistant workStudyAssistant;

    /**
     * 流式对话
     * <p>memoryId 用当前登录用户 id：不同学生各自一份记忆，互不串话。
     * 登录态由 JwtTokenInterceptor 保证（/ai/** 未放行，必须带 token）。</p>
     */
    @GetMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "AI 助手对话（流式返回）")
    public Flux<String> chat(@RequestParam("message") String message) {
        Long userId = BaseContext.getCurrentId();
        String memoryId = userId == null ? "anonymous" : String.valueOf(userId);
        log.info("[AI] userId={}, memoryId={}, message={}", userId, memoryId, message);
        return workStudyAssistant.chat(memoryId, message);
    }
}