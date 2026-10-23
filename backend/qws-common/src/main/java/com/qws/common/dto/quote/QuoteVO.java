package com.qws.common.dto.quote;

import com.qws.common.entity.CrmQuote;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QuoteVO {
    private Long id;
    private String customerName;
    private String brand;
    private String model;
    private BigDecimal price;
    private String visitSubject;
    private String createTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static QuoteVO from(CrmQuote entity) {
        QuoteVO vo = new QuoteVO();
        vo.id = entity.getId();
        vo.customerName = entity.getCustomerName();
        vo.brand = entity.getBrand();
        vo.model = entity.getModel();
        vo.price = entity.getPrice();
        vo.visitSubject = entity.getVisitSubject();
        vo.createTime = format(entity.getCreateTime());
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getVisitSubject() { return visitSubject; }
    public void setVisitSubject(String visitSubject) { this.visitSubject = visitSubject; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
