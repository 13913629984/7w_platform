package com.qws.common.dto.order;

import com.qws.common.entity.CrmOrder;
import com.qws.common.entity.CrmOrderItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class OrderVO {
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
    private String outboundCode;
    private String createTime;
    private List<OrderItemDTO> items;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static OrderVO from(CrmOrder order, List<CrmOrderItem> itemEntities) {
        OrderVO vo = new OrderVO();
        vo.id = order.getId();
        vo.orderNo = order.getOrderNo();
        vo.customerId = order.getCustomerId();
        vo.customerName = order.getCustomerName();
        vo.dealId = order.getDealId();
        vo.dealName = order.getDealName();
        vo.contractNo = order.getContractNo();
        vo.amount = order.getAmount();
        vo.owner = order.getOwner();
        vo.signDate = formatDate(order.getSignDate());
        vo.expectDeliverAt = formatDate(order.getExpectDeliverAt());
        vo.status = order.getStatus();
        vo.remark = order.getRemark();
        vo.outboundCode = order.getOutboundCode();
        vo.createTime = formatDateTime(order.getCreateTime());
        vo.items = itemEntities == null ? List.of() : itemEntities.stream().map(item -> {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setId(item.getId());
            dto.setProductCode(item.getProductCode());
            dto.setProductName(item.getProductName());
            dto.setSpec(item.getSpec());
            dto.setQuantity(item.getQuantity());
            dto.setPrice(item.getPrice());
            dto.setSubtotal(item.getSubtotal());
            return dto;
        }).collect(Collectors.toList());
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

    public String getOutboundCode() { return outboundCode; }
    public void setOutboundCode(String outboundCode) { this.outboundCode = outboundCode; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }
}
