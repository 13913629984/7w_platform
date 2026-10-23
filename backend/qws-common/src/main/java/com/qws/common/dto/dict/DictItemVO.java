package com.qws.common.dto.dict;

import com.qws.common.entity.SysDictItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DictItemVO {
    private Long id;
    private String dictCode;
    private String itemValue;
    private String itemLabel;
    private String color;
    private Integer sort;
    private Integer status;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static DictItemVO from(SysDictItem entity) {
        DictItemVO vo = new DictItemVO();
        vo.id = entity.getId();
        vo.dictCode = entity.getDictCode();
        vo.itemValue = entity.getItemValue();
        vo.itemLabel = entity.getItemLabel();
        vo.color = entity.getColor();
        vo.sort = entity.getSort();
        vo.status = entity.getStatus();
        vo.remark = entity.getRemark();
        vo.createTime = format(entity.getCreateTime());
        vo.updateTime = format(entity.getUpdateTime());
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDictCode() { return dictCode; }
    public void setDictCode(String dictCode) { this.dictCode = dictCode; }
    public String getItemValue() { return itemValue; }
    public void setItemValue(String itemValue) { this.itemValue = itemValue; }
    public String getItemLabel() { return itemLabel; }
    public void setItemLabel(String itemLabel) { this.itemLabel = itemLabel; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
