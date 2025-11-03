package com.luna.deepluna.domain.response.rag;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {
    private String id;

    private Double score;

    private String content;

    public static RagResponse fromDocument(Document document) {
        return RagResponse.builder()
                .id(document.getId())
                .score(document.getScore())
                .content(document.getFormattedContent())
                .build();
    }
}
