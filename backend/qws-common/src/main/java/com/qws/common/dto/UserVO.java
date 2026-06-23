package com.qws.common.dto;

import com.qws.common.entity.SysUser;

import java.time.LocalDateTime;

public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String employeeId;
    private String email;
    private String phone;
    private String department;
    private String position;
    private String role;
    private String status;
    private LocalDateTime lastLogin;
    private LocalDateTime createTime;

    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.nickname = user.getNickname();
        vo.employeeId = user.getEmployeeId();
        vo.email = user.getEmail();
        vo.phone = user.getPhone();
        vo.department = user.getDepartment();
        vo.position = user.getPosition();
        vo.role = user.getRole();
        vo.status = user.getStatus() != null && user.getStatus() == 1 ? "启用" : "停用";
        vo.lastLogin = user.getLastLogin();
        vo.createTime = user.getCreateTime();
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
