package com.luna.deepluna.agent.context;

import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;

import java.time.LocalDateTime;

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
    private volatile SubAgentTaskStatus status;

    // 聊天记忆
    private ChatMemory chatMemory;

    // 最大工具调用次数
    private Integer maxWebSearch;

    // 子任务最终压缩结果（完成时写入）
    private volatile String result;

    // 子任务失败原因（失败时写入）
    private volatile String errorMessage;

    // 开始时间
    private volatile LocalDateTime startedTime;

    // 结束时间
    private volatile LocalDateTime finishedTime;

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
