package com.luna.deepluna.agent.agentTool;

import com.luna.deepluna.domain.response.websearch.WebSearchResponse;

public interface WebSearchProvider {

    String providerId();

    WebSearchResponse search(String query);
}

