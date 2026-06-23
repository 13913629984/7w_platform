package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("crm_customer")
public class CrmCustomer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String englishName;

    private String address;

    private String level;

    private String owner;

    private LocalDate lastVisitAt;

    private LocalDate lastDealAt;

    private String remark;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

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

    public LocalDate getLastVisitAt() { return lastVisitAt; }
    public void setLastVisitAt(LocalDate lastVisitAt) { this.lastVisitAt = lastVisitAt; }

    public LocalDate getLastDealAt() { return lastDealAt; }
    public void setLastDealAt(LocalDate lastDealAt) { this.lastDealAt = lastDealAt; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
