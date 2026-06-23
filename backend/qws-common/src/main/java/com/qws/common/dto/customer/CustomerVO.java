package com.qws.common.dto.customer;

import com.qws.common.entity.CrmCustomer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerVO {
    private Long id;
    private String name;
    private String englishName;
    private String address;
    private String level;
    private String owner;
    private String lastVisitAt;
    private String lastDealAt;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<CustomerSalesDTO> salesList;
    private List<CustomerContactDTO> contactList;

    public static CustomerVO from(CrmCustomer customer, List<CustomerSalesDTO> salesList, List<CustomerContactDTO> contactList) {
        CustomerVO vo = new CustomerVO();
        vo.id = customer.getId();
        vo.name = customer.getName();
        vo.englishName = customer.getEnglishName();
        vo.address = customer.getAddress();
        vo.level = customer.getLevel();
        vo.owner = customer.getOwner();
        vo.lastVisitAt = formatDate(customer.getLastVisitAt());
        vo.lastDealAt = formatDate(customer.getLastDealAt());
        vo.remark = customer.getRemark();
        vo.status = customer.getStatus();
        vo.createTime = customer.getCreateTime();
        vo.updateTime = customer.getUpdateTime();
        vo.salesList = salesList;
        vo.contactList = contactList;
        return vo;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getLastVisitAt() { return lastVisitAt; }
    public void setLastVisitAt(String lastVisitAt) { this.lastVisitAt = lastVisitAt; }

    public String getLastDealAt() { return lastDealAt; }
    public void setLastDealAt(String lastDealAt) { this.lastDealAt = lastDealAt; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public List<CustomerSalesDTO> getSalesList() { return salesList; }
    public void setSalesList(List<CustomerSalesDTO> salesList) { this.salesList = salesList; }

    public List<CustomerContactDTO> getContactList() { return contactList; }
    public void setContactList(List<CustomerContactDTO> contactList) { this.contactList = contactList; }
}
