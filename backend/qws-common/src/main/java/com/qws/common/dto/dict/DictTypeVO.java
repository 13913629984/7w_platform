package com.qws.common.dto.dict;

import com.qws.common.entity.SysDictType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DictTypeVO {
    private Long id;
    private String dictCode;
    private String dictName;
    private String description;
    private Boolean isSystem;
    private Integer sort;
    private Integer status;
    private Integer itemCount;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static DictTypeVO from(SysDictType entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.id = entity.getId();
        vo.dictCode = entity.getDictCode();
        vo.dictName = entity.getDictName();
        vo.description = entity.getDescription();
        vo.isSystem = entity.getIsSystem() != null && entity.getIsSystem() == 1;
        vo.sort = entity.getSort();
        vo.status = entity.getStatus();
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
    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
