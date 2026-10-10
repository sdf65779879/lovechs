package com.couple.ordering.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.couple.ordering.common.exception.BusinessException;
import com.couple.ordering.common.result.PageResult;
import com.couple.ordering.common.result.ResultCode;
import com.couple.ordering.common.security.SecurityUtils;
import com.couple.ordering.dto.UserCreateDTO;
import com.couple.ordering.dto.UserUpdateDTO;
import com.couple.ordering.entity.SysUser;
import com.couple.ordering.enums.UserRole;
import com.couple.ordering.mapper.SysUserMapper;
import com.couple.ordering.service.SysUserService;
import com.couple.ordering.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    @Override
    public SysUser findByUsername(String username) {
        return getBaseMapper().selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public PageResult<UserVO> adminPage(Integer pageNum, Integer pageSize, String keyword) {
        Page<SysUser> page = new Page<>(safePageNum(pageNum), safePageSize(pageSize));
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = getBaseMapper().selectPage(page, wrapper);
        List<UserVO> list = result.getRecords().stream().map(this::toUserVO).toList();
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public UserVO createUser(UserCreateDTO dto) {
        if (findByUsername(dto.getUsername()) != null) {
            throw new BusinessException("账号「" + dto.getUsername() + "」已存在");
        }
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user, "password");
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 校验并规范化角色，缺省为 USER
        user.setRole(StringUtils.hasText(dto.getRole())
                ? UserRole.from(dto.getRole()).name() : UserRole.USER.name());
        user.setStatus(1);
        getBaseMapper().insert(user);
        return toUserVO(user);
    }

    @Override
    public void updateUser(Long id, UserUpdateDTO dto) {
        SysUser user = mustGet(id);
        // 防止管理员把自己降级而失去管理能力
        if (id.equals(SecurityUtils.getUserId())
                && UserRole.USER.name().equals(dto.getRole())) {
            throw new BusinessException("不能降级当前登录的管理员账号");
        }
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        if (StringUtils.hasText(dto.getRole())) {
            user.setRole(UserRole.from(dto.getRole()).name());
        }
        getBaseMapper().updateById(user);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        SysUser user = mustGet(id);
        int normalized = normalizeStatus(status);
        if (id.equals(SecurityUtils.getUserId()) && normalized == 0) {
            throw new BusinessException("不能禁用当前登录账号");
        }
        user.setStatus(normalized);
        getBaseMapper().updateById(user);
    }

    @Override
    public void resetPassword(Long id, String password) {
        SysUser user = mustGet(id);
        user.setPassword(passwordEncoder.encode(password));
        getBaseMapper().updateById(user);
    }

    private SysUser mustGet(Long id) {
        SysUser user = getBaseMapper().selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private int normalizeStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态值只能为 0 或 1");
        }
        return status;
    }

    private long safePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
