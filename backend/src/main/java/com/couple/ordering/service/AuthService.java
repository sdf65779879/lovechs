package com.couple.ordering.service;

import com.couple.ordering.dto.LoginDTO;
import com.couple.ordering.dto.PasswordDTO;
import com.couple.ordering.dto.ProfileDTO;
import com.couple.ordering.vo.LoginVO;
import com.couple.ordering.vo.UserVO;

/**
 * 认证服务：登录登出、当前用户信息、资料与密码修改。
 */
public interface AuthService {

    /**
     * 账号密码登录，返回 Token 与用户信息。
     */
    LoginVO login(LoginDTO dto);

    /**
     * 登出（无状态 JWT，前端清除 Token 即可）。
     */
    void logout();

    /**
     * 获取当前登录用户信息。
     */
    UserVO me();

    /**
     * 修改当前用户昵称/头像。
     */
    void updateProfile(ProfileDTO dto);

    /**
     * 修改当前用户密码。
     */
    void changePassword(PasswordDTO dto);
}
