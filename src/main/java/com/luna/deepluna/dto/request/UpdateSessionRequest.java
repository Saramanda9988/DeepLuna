package com.luna.deepluna.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新Session请求DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSessionRequest {
    private String summary;
    private String researchBrief;
}