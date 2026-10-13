package com.couple.ordering.controller.admin;

import com.couple.ordering.common.result.Result;
import com.couple.ordering.dto.EmailSettingsDTO;
import com.couple.ordering.notification.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "13. 系统邮件通知")
@RestController
@RequestMapping("/v1/admin/email")
@RequiredArgsConstructor
public class EmailAdminController {
    private final EmailSettingsService settings;
    private final OrderEmailNotificationService notifications;
    private final EmailDeliveryRepository deliveries;

    @GetMapping("/settings")
    @Operation(summary = "读取系统邮箱及大厨新订单通知设置（不返回密码）")
    public Result<EmailSettingsService.SettingsView> settings() {
        return Result.success(settings.view());
    }

    @PutMapping("/settings")
    @Operation(summary = "保存系统邮箱及大厨通知设置，授权码留空保留原值")
    public Result<Void> save(@Valid @RequestBody EmailSettingsDTO dto) {
        settings.save(dto);
        return Result.success();
    }

    @PostMapping("/test")
    @Operation(summary = "使用已保存配置发送测试邮件，投递结果请查询记录")
    public Result<Void> test() {
        notifications.test();
        return Result.success();
    }

    @GetMapping("/deliveries")
    @Operation(summary = "最近 20 条邮件投递记录（错误已脱敏）")
    public Result<List<EmailDeliveryRepository.Delivery>> deliveries() {
        return Result.success(deliveries.recent());
    }
}
