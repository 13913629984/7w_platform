package com.qws.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.SysUser;
import com.qws.common.mapper.SysUserMapper;
import com.qws.common.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final String initialAdminPassword;

    public AdminUserInitializer(SysUserMapper sysUserMapper,
                                @Value("${qws.security.initial-admin-password:${QWS_ADMIN_PASSWORD:123456}}") String initialAdminPassword) {
        this.sysUserMapper = sysUserMapper;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    public void run(String... args) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
        );
        if (count != null && count > 0) {
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(PasswordUtil.hash(initialAdminPassword));
        admin.setNickname("系统管理员");
        admin.setEmail("admin@qws.com");
        admin.setEmployeeId("EMP-001");
        admin.setDepartment("系统管理部");
        admin.setPosition("系统管理员");
        admin.setRole("超级管理员");
        admin.setStatus(1);
        sysUserMapper.insert(admin);
    }
}
