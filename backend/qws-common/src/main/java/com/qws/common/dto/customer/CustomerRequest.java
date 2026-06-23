package com.qws.common.dto.customer;

import java.util.List;

public class CustomerRequest {
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
    private List<CustomerSalesDTO> salesList;
    private List<CustomerContactDTO> contactList;

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

    public List<CustomerSalesDTO> getSalesList() { return salesList; }
    public void setSalesList(List<CustomerSalesDTO> salesList) { this.salesList = salesList; }

    public List<CustomerContactDTO> getContactList() { return contactList; }
    public void setContactList(List<CustomerContactDTO> contactList) { this.contactList = contactList; }
}
