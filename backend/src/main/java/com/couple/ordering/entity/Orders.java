package com.couple.ordering.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体，对应表 orders。
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号：yyyyMMddHHmmss + 4 位随机数 */
    private String orderNo;

    /** 下单用户 id */
    private Long userId;

    /** 订单状态，见 OrderStatus */
    private String status;

    /** 合计爱心值 */
    private Integer totalLove;

    /** 整单备注 */
    private String remark;

    /** 期望用餐时间，null 表示尽快 */
    private LocalDateTime expectTime;

    /** 拒绝理由 */
    private String rejectReason;

    /** 下单时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
