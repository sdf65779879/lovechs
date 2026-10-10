package com.couple.ordering.common.security;

import com.couple.ordering.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * 登录用户主体，扩展 Spring Security 的 {@link User}，
 * 额外携带 userId 与 role，便于业务层直接获取当前用户。
 */
@Getter
public class LoginUser extends User {

    private final Long userId;
    private final String role;

    public LoginUser(Long userId, String username, String password, boolean enabled,
                     String role, List<GrantedAuthority> authorities) {
        super(username, password == null ? "" : password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.role = role;
    }

    /**
     * 由数据库用户实体构建登录主体。
     */
    public static LoginUser from(SysUser user) {
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(),
                enabled, user.getRole(), authorities);
    }
}
