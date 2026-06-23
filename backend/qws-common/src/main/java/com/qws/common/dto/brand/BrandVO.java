package com.qws.common.dto.brand;

import com.qws.common.entity.Brand;

import java.time.LocalDateTime;

public class BrandVO {
    private Long id;
    private String code;
    private String name;
    private String englishName;
    private String logo;
    private String country;
    private String supplier;
    private String contact;
    private String phone;
    private String description;
    private Integer sort;
    private Integer status;
    private String statusName;
    private Integer materialCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BrandVO from(Brand brand) {
        BrandVO vo = new BrandVO();
        vo.id = brand.getId();
        vo.code = brand.getCode();
        vo.name = brand.getName();
        vo.englishName = brand.getEnglishName();
        vo.logo = brand.getLogo();
        vo.country = brand.getCountry();
        vo.supplier = brand.getSupplier();
        vo.contact = brand.getContact();
        vo.phone = brand.getPhone();
        vo.description = brand.getDescription();
        vo.sort = brand.getSort();
        vo.status = brand.getStatus();
        vo.statusName = brand.getStatus() != null && brand.getStatus() == 1 ? "启用" : "禁用";
        vo.createTime = brand.getCreateTime();
        vo.updateTime = brand.getUpdateTime();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
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