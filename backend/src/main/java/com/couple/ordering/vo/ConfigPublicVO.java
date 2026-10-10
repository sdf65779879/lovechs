package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户端公开配置出参（营业状态 + 公告）。
 */
@Data
@Schema(description = "公开配置")
public class ConfigPublicVO {

    @Schema(description = "营业状态：1 营业 / 0 歇业")
    private Integer businessOpen;

    @Schema(description = "首页公告文案")
    private String notice;
}
