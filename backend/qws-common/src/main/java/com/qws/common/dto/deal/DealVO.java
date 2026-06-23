package com.qws.common.dto.deal;

import com.qws.common.entity.CrmDeal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DealVO {
    private Long id;
    private String name;
    private Long customerId;
    private String customerName;
    private String leadName;
    private BigDecimal amount;
    private String stage;
    private Integer winRate;
    private String owner;
    private String expectDealAt;
    private String status;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static DealVO from(CrmDeal deal) {
        DealVO vo = new DealVO();
        vo.id = deal.getId();
        vo.name = deal.getName();
        vo.customerId = deal.getCustomerId();
        vo.customerName = deal.getCustomerName();
        vo.leadName = deal.getLeadName();
        vo.amount = deal.getAmount();
        vo.stage = deal.getStage();
        vo.winRate = deal.getWinRate();
        vo.owner = deal.getOwner();
        vo.expectDealAt = formatDate(deal.getExpectDealAt());
        vo.status = deal.getStatus();
        vo.remark = deal.getRemark();
        vo.createTime = formatDateTime(deal.getCreateTime());
        vo.updateTime = formatDateTime(deal.getUpdateTime());
        return vo;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE);
    }

    private static String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATE);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public Integer getWinRate() { return winRate; }
    public void setWinRate(Integer winRate) { this.winRate = winRate; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getExpectDealAt() { return expectDealAt; }
    public void setExpectDealAt(String expectDealAt) { this.expectDealAt = expectDealAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
