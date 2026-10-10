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
 * 菜品实体，对应表 dish。
 */
@Data
@TableName("dish")
public class Dish {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分类 id */
    private Long categoryId;

    /** 菜品名称 */
    private String name;

    /** 菜品描述 */
    private String description;

    /** 图片地址 */
    private String image;

    /** 标签，英文逗号分隔 */
    private String tags;

    /** 爱心值 */
    private Integer loveValue;

    /** 排序值 */
    private Integer sort;

    /** 状态：1 上架 / 0 下架 */
    private Integer status;

    /** 是否推荐：1 是 / 0 否 */
    private Integer isRecommend;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
