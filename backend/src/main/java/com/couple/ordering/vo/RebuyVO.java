package com.couple.ordering.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 再次下单结果出参。
 */
@Data
@Schema(description = "再次下单结果")
public class RebuyVO {

    @Schema(description = "成功加回购物车的菜品数量")
    private Integer addedCount;

    @Schema(description = "因下架等原因未能加回的菜品名称")
    private List<String> skippedNames;
}
