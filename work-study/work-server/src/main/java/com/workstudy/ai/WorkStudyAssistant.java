package com.workstudy.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * 勤工助学智能助手（LangChain4j 声明式 AI Service）
 *
 * <p>与苍穹外卖不同：这里没有手写 AiManualConfig 去 new ChatModel，
 * 模型由 langchain4j-open-ai-spring-boot-starter 依据 application.yml 自动装配。</p>
 *
 * <p>与志愿填报的 ConsultantService 对照：这里同样是 EXPLICIT 显式注入，
 * 逐个指定 Bean 名称，区别在于本项目没有做 RAG，所以没有 contentRetriever。</p>
 *
 * <ul>
 *   <li>wiringMode = EXPLICIT：按名称注入，而不是按类型自动找（多模型时更不容易错）</li>
 *   <li>chatModel：同步模型，方法返回 String 时用</li>
 *   <li>streamingChatModel：流式模型，方法返回 Flux 时用</li>
 *   <li>chatMemoryProvider：会话记忆工厂（本项目窗口 20 条 + Redis 持久化）</li>
 *   <li>tools：工具 Bean，模型据此决定调用哪个 @Tool 方法</li>
 * </ul>
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "workStudyTools"
)
public interface WorkStudyAssistant {

    /**
     * 流式对话：返回 Flux，Spring MVC 通过 ReactiveTypeHandler 以 SSE 逐字输出
     *
     * @param memoryId 会话ID（本项目用登录用户ID，实现多轮记忆隔离）
     * @param message  用户问题
     */
    @SystemMessage(fromResource = "ai/system-message.txt")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}