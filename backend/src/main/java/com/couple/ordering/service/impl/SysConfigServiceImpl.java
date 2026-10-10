package com.couple.ordering.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.couple.ordering.common.constant.Constants;
import com.couple.ordering.entity.SysConfig;
import com.couple.ordering.mapper.SysConfigMapper;
import com.couple.ordering.service.SysConfigService;
import com.couple.ordering.vo.ConfigPublicVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现。
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigService {

    @Override
    public ConfigPublicVO publicConfig() {
        ConfigPublicVO vo = new ConfigPublicVO();
        vo.setBusinessOpen(isBusinessOpen() ? 1 : 0);
        vo.setNotice(StrUtil.nullToEmpty(
                getConfigValue(Constants.CONFIG_NOTICE)));
        return vo;
    }

    @Override
    public List<SysConfig> listAll() {
        return getBaseMapper().selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = StrUtil.nullToEmpty(entry.getKey());
            String value = StrUtil.nullToEmpty(entry.getValue());
            if (key.isBlank()) {
                continue;
            }
            SysConfig existing = getBaseMapper().selectOne(
                    new LambdaQueryWrapper<SysConfig>()
                            .eq(SysConfig::getConfigKey, key));
            if (existing == null) {
                SysConfig config = new SysConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                getBaseMapper().insert(config);
            } else {
                existing.setConfigValue(value);
                getBaseMapper().updateById(existing);
            }
        }
    }

    @Override
    public String getConfigValue(String key) {
        SysConfig config = getBaseMapper().selectOne(
                new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getConfigKey, key));
        return config == null ? null : config.getConfigValue();
    }

    @Override
    public boolean isBusinessOpen() {
        return "1".equals(getConfigValue(Constants.CONFIG_BUSINESS_OPEN));
    }
}
