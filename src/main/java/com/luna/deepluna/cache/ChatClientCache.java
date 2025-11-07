package com.luna.deepluna.cache;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatClientCache {
    // sessionId -> chat client
    private final Map<String, OpenAiChatModel> chatClientMap = new ConcurrentHashMap<>();

    public OpenAiChatModel getChatClient(String sessionId) {
        return chatClientMap.get(sessionId);
    }

    public void putChatClient(String sessionId, OpenAiChatModel chatClient) {
        chatClientMap.put(sessionId, chatClient);
    }
}
