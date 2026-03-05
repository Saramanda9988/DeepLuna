package com.luna.deepluna.agent.context;

import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubAgentContext {
    // 属于的会话ID
    private String sessionId;

    // 整个流程的监督者ID
    private String subAgentId;

    // 子智能体的研究主题
    private String researchTopic;

    // 当前流程的状态
    private SubAgentTaskStatus status;

    // 聊天记忆
    private ChatMemory chatMemory;

    // 最大工具调用次数
    private Integer maxWebSearch;

    /**
     * Supervisor 发送的取消信号。
     * 使用 volatile 保证跨线程可见性：Supervisor 线程写，SubAgent 线程读。
     */
    @Builder.Default
    private volatile boolean cancelled = false;

    /**
     * SubAgent 被提前取消时，保存已收集到的中间研究结果，
     * 避免已完成的搜索工作完全丢失。
     */
    private String earlyStopResult;
}
