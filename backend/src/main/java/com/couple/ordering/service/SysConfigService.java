package com.couple.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.couple.ordering.entity.SysConfig;
import com.couple.ordering.vo.ConfigPublicVO;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务。
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 用户端公开配置（营业状态 + 公告）。
     */
    ConfigPublicVO publicConfig();

    /**
     * 全部配置列表。
     */
    List<SysConfig> listAll();

    /**
     * 批量更新配置（不存在的键自动创建）。
     */
    void updateConfigs(Map<String, String> configs);

    /**
     * 按键获取配置值，不存在返回 null。
     */
    String getConfigValue(String key);

    /**
     * 当前是否营业。
     */
    boolean isBusinessOpen();
}
