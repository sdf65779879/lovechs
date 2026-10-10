package com.couple.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.dto.UserCreateDTO;
import com.couple.ordering.dto.UserUpdateDTO;
import com.couple.ordering.entity.SysUser;
import com.couple.ordering.vo.UserVO;

/**
 * 用户服务（含管理员对用户的管理）。
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 实体转 UserVO。
     */
    UserVO toUserVO(SysUser user);

    /**
     * 按用户名查询用户（含逻辑删除过滤）。
     */
    SysUser findByUsername(String username);

    /**
     * 管理端分页查询用户。
     */
    PageResult<UserVO> adminPage(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 新增用户。
     */
    UserVO createUser(UserCreateDTO dto);

    /**
     * 编辑用户。
     */
    void updateUser(Long id, UserUpdateDTO dto);

    /**
     * 启用/禁用用户。
     */
    void toggleStatus(Long id, Integer status);

    /**
     * 重置用户密码。
     */
    void resetPassword(Long id, String password);
}
