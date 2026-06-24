package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("fin_receivable")
public class FinReceivable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String customer;
    @TableField("sales_order")
    private String salesOrder;
    private String contract;
    private BigDecimal amount;
    @TableField("written_off")
    private BigDecimal writtenOff;
    private BigDecimal pending;
    private Integer invoiced;
    @TableField("due_date")
    private String dueDate;
    private String status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public String getSalesOrder() { return salesOrder; }
    public void setSalesOrder(String salesOrder) { this.salesOrder = salesOrder; }
    public String getContract() { return contract; }
    public void setContract(String contract) { this.contract = contract; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getWrittenOff() { return writtenOff; }
    public void setWrittenOff(BigDecimal writtenOff) { this.writtenOff = writtenOff; }
    public BigDecimal getPending() { return pending; }
    public void setPending(BigDecimal pending) { this.pending = pending; }
    public Integer getInvoiced() { return invoiced; }
    public void setInvoiced(Integer invoiced) { this.invoiced = invoiced; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
