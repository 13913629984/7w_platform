package com.qws.common.dto.config;

import com.qws.common.entity.SysConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConfigVO {
    private Long id;
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private String options;
    private String configGroup;
    private String description;
    private Boolean isSystem;
    private Integer sort;
    private Integer status;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ConfigVO from(SysConfig entity) {
        ConfigVO vo = new ConfigVO();
        vo.id = entity.getId();
        vo.configKey = entity.getConfigKey();
        vo.configName = entity.getConfigName();
        vo.configValue = entity.getConfigValue();
        vo.valueType = entity.getValueType();
        vo.options = entity.getOptions();
        vo.configGroup = entity.getConfigGroup();
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
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
