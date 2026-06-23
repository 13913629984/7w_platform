package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("wms_inbound_sn")
public class InboundSn {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("item_id")
    private Long itemId;
    @TableField("material_id")
    private Long materialId;
    @TableField("location_code")
    private String locationCode;
    private String sn;
    private String status;
    @TableField("inbound_time")
    private LocalDateTime inboundTime;
    @TableField("outbound_code")
    private String outboundCode;
    @TableField("outbound_time")
    private LocalDateTime outboundTime;
    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getSn() { return sn; }
    public void setSn(String sn) { this.sn = sn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getInboundTime() { return inboundTime; }
    public void setInboundTime(LocalDateTime inboundTime) { this.inboundTime = inboundTime; }
    public String getOutboundCode() { return outboundCode; }
    public void setOutboundCode(String outboundCode) { this.outboundCode = outboundCode; }
    public LocalDateTime getOutboundTime() { return outboundTime; }
    public void setOutboundTime(LocalDateTime outboundTime) { this.outboundTime = outboundTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
