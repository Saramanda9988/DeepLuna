package com.luna.deepluna.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "澄清聊天响应")
public class ClarifyChatResponse extends ChatResp {

    @Schema(description = "是否需要澄清", example = "false")
    private Boolean needsClarification;

    @Schema(description = "澄清轮次", example = "1")
    private Integer clarifyRound;

    @Schema(description = "澄清问题")
    private String clarificationQuestion;
}