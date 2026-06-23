package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("crm_customer_sales")
public class CrmCustomerSales {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String name;

    private String position;

    private Integer isOwner;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getIsOwner() { return isOwner; }
    public void setIsOwner(Integer isOwner) { this.isOwner = isOwner; }
}
