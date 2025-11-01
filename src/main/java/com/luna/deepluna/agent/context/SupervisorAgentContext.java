package com.luna.deepluna.agent.context;

import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupervisorAgentContext {
    // 属于的会话ID
    private String sessionId;

    // 整个流程的监督者ID
    private String supervisorId;

    // 各个子任务的结果,作为自己的记录笔记
    private List<String> notes;

    // 这个流程的研究简报
    private String researchBrief;

    // 当前总流程的状态
    private SupervisorAgentState status;

    // 聊天记忆
    private ChatMemory chatMemory;

    // 最大并行数量
    private long maxSubAgentsNumber;
}
