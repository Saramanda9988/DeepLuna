package com.luna.deepluna.agent;

import com.luna.deepluna.common.core.TaskResult;

/**
 * 子任务执行接口，定义了执行任务和获取代理类型的方法。
 */
public interface AgentWorker {

    TaskResult execute();

    String getAgentType();

    boolean canHandle();
}