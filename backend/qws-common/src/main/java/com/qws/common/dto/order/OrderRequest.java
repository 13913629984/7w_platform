package com.qws.common.dto.order;

import java.math.BigDecimal;
import java.util.List;

public class OrderRequest {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Long dealId;
    private String dealName;
    private String contractNo;
    private BigDecimal amount;
    private String owner;
    private String signDate;
    private String expectDeliverAt;
    private String status;
    private String remark;
    private List<OrderItemDTO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Long getDealId() { return dealId; }
    public void setDealId(Long dealId) { this.dealId = dealId; }

    public String getDealName() { return dealName; }
    public void setDealName(String dealName) { this.dealName = dealName; }

    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getSignDate() { return signDate; }
    public void setSignDate(String signDate) { this.signDate = signDate; }

    public String getExpectDeliverAt() { return expectDeliverAt; }
    public void setExpectDeliverAt(String expectDeliverAt) { this.expectDeliverAt = expectDeliverAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }
}
