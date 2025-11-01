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

    // 最大反思次数
    private Integer maxWebSearch;
}
