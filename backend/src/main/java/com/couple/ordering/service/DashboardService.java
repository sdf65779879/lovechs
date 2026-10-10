package com.couple.ordering.service;

import com.couple.ordering.vo.DashboardVO;

/**
 * 管理端数据看板服务。
 */
public interface DashboardService {

    /**
     * 聚合看板数据。
     */
    DashboardVO build();
}
