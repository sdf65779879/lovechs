package com.couple.ordering.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单明细实体，对应表 order_item。
 * 下单时冗余菜品快照，菜品后续修改不影响历史订单。
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单 id */
    private Long orderId;

    /** 菜品 id */
    private Long dishId;

    /** 菜品名称快照 */
    private String dishName;

    /** 菜品图片快照 */
    private String dishImage;

    /** 下单时爱心值快照 */
    private Integer loveValue;

    /** 数量 */
    private Integer quantity;

    /** 口味快照 */
    private String taste;

    /** 单品忌口/留言 */
    private String remark;
}
