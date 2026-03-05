package com.luna.deepluna.cache;

import com.luna.deepluna.common.utils.AssertUtil;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatClientCache {
    // sessionId -> chat client
    private final Map<String, OpenAiChatModel> chatClientMap = new ConcurrentHashMap<>();

    public OpenAiChatModel getBySessionId(String sessionId) {
        AssertUtil.isNotNull(sessionId, "sessionId must not be null");
        return chatClientMap.get(sessionId);
    }

    public void putBySessionId(String sessionId, OpenAiChatModel chatClient) {
        AssertUtil.isNotNull(sessionId, "sessionId must not be null");
        AssertUtil.isNotNull(chatClient, "chatClient must not be null");
        chatClientMap.put(sessionId, chatClient);
    }

    @Deprecated
    public OpenAiChatModel getChatClient(String sessionId) {
        return getBySessionId(sessionId);
    }

    @Deprecated
    public void putChatClient(String sessionId, OpenAiChatModel chatClient) {
        putBySessionId(sessionId, chatClient);
    }
}
