package com.couple.ordering.service.impl;

import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.common.security.JwtUtils;
import com.couple.ordering.common.security.LoginUser;
import com.couple.ordering.common.security.SecurityUtils;
import com.couple.ordering.dto.LoginDTO;
import com.couple.ordering.dto.PasswordDTO;
import com.couple.ordering.dto.ProfileDTO;
import com.couple.ordering.entity.SysUser;
import com.couple.ordering.service.AuthService;
import com.couple.ordering.service.SysUserService;
import com.couple.ordering.vo.LoginVO;
import com.couple.ordering.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserService sysUserService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getUsername(), dto.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        } catch (DisabledException e) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = sysUserService.getById(loginUser.getUserId());
        String token = jwtUtils.generateToken(
                user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpireIn(jwtUtils.getExpireSeconds());
        vo.setUser(sysUserService.toUserVO(user));
        return vo;
    }

    @Override
    public void logout() {
        // 无状态 JWT：前端丢弃本地 Token 即完成登出，无需服务端处理
    }

    @Override
    public UserVO me() {
        SysUser user = sysUserService.getById(SecurityUtils.getUserId());
        return sysUserService.toUserVO(user);
    }

    @Override
    public void updateProfile(ProfileDTO dto) {
        SysUser update = new SysUser();
        update.setId(SecurityUtils.getUserId());
        update.setNickname(dto.getNickname());
        update.setAvatar(dto.getAvatar());
        sysUserService.updateById(update);
    }

    @Override
    public void changePassword(PasswordDTO dto) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserService.getById(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserService.updateById(update);
    }
}
