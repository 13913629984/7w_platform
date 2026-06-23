package com.qws.common.dto.contact;

import com.qws.common.entity.CrmContact;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ContactVO {
    private Long id;
    private String name;
    private String title;
    private Long customerId;
    private String customerName;
    private String phone;
    private String email;
    private Boolean isPrimary;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static ContactVO from(CrmContact contact) {
        ContactVO vo = new ContactVO();
        vo.id = contact.getId();
        vo.name = contact.getName();
        vo.title = contact.getTitle();
        vo.customerId = contact.getCustomerId();
        vo.customerName = contact.getCustomerName();
        vo.phone = contact.getPhone();
        vo.email = contact.getEmail();
        vo.isPrimary = contact.getIsPrimary() != null && contact.getIsPrimary() == 1;
        vo.remark = contact.getRemark();
        vo.createTime = formatDate(contact.getCreateTime());
        vo.updateTime = formatDate(contact.getUpdateTime());
        return vo;
    }

    private static String formatDate(LocalDateTime time) {
        return time == null ? null : time.format(DATE);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
