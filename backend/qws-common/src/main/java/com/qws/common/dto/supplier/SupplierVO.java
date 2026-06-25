package com.qws.common.dto.supplier;

import com.qws.common.entity.Supplier;

import java.time.LocalDateTime;

public class SupplierVO {
    private Long id;
    private String code;
    private String name;
    private String shortName;
    private String category;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String taxNo;
    private String bankAccount;
    private String settleType;
    private String description;
    private Integer sort;
    private Integer status;
    private String statusName;
    private Integer materialCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static SupplierVO from(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        vo.id = supplier.getId();
        vo.code = supplier.getCode();
        vo.name = supplier.getName();
        vo.shortName = supplier.getShortName();
        vo.category = supplier.getCategory();
        vo.contact = supplier.getContact();
        vo.phone = supplier.getPhone();
        vo.email = supplier.getEmail();
        vo.address = supplier.getAddress();
        vo.taxNo = supplier.getTaxNo();
        vo.bankAccount = supplier.getBankAccount();
        vo.settleType = supplier.getSettleType();
        vo.description = supplier.getDescription();
        vo.sort = supplier.getSort();
        vo.status = supplier.getStatus();
        vo.statusName = supplier.getStatus() != null && supplier.getStatus() == 1 ? "启用" : "禁用";
        vo.createTime = supplier.getCreateTime();
        vo.updateTime = supplier.getUpdateTime();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String taxNo) { this.taxNo = taxNo; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getSettleType() { return settleType; }
    public void setSettleType(String settleType) { this.settleType = settleType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Integer getMaterialCount() { return materialCount; }
    public void setMaterialCount(Integer materialCount) { this.materialCount = materialCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
