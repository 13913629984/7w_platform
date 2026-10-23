package com.qws.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.config.ConfigDeleteRequest;
import com.qws.common.dto.config.ConfigRequest;
import com.qws.common.dto.config.ConfigVO;
import com.qws.common.entity.SysConfig;
import com.qws.common.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 系统参数配置：按分组读取、增删改、批量保存。 */
@Service
public class ConfigService {

    private final SysConfigMapper configMapper;

    public ConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public Map<String, Object> list(String keyword, String group) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysConfig::getConfigKey, keyword)
                    .or().like(SysConfig::getConfigName, keyword)
                    .or().like(SysConfig::getDescription, keyword));
        }
        if (StringUtils.hasText(group)) {
            wrapper.eq(SysConfig::getConfigGroup, group);
        }
        wrapper.orderByAsc(SysConfig::getSort).orderByAsc(SysConfig::getId);

        List<ConfigVO> rows = configMapper.selectList(wrapper).stream().map(ConfigVO::from).toList();

        // 按 config_group 聚合成分组，前端 tab / 折叠面板直接消费
        Map<String, List<ConfigVO>> groups = new LinkedHashMap<>();
        for (ConfigVO vo : rows) {
            String key = StringUtils.hasText(vo.getConfigGroup()) ? vo.getConfigGroup() : "未分组";
            groups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(vo);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", rows);
        payload.put("total", rows.size());
        payload.put("groups", groups);
        payload.put("stats", stats());
        return payload;
    }

    public ConfigVO detail(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) throw new IllegalArgumentException("参数不存在");
        return ConfigVO.from(config);
    }

    /** 按 key 取值的便捷方法，供其它业务读取配置。 */
    public String value(String configKey, String defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey).last("LIMIT 1"));
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    @Transactional
    public ConfigVO create(ConfigRequest request) {
        validate(request, true);
        Long exists = configMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, request.getConfigKey().trim()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("参数键名已存在：" + request.getConfigKey());
        }
        SysConfig config = new SysConfig();
        apply(config, request);
        config.setIsSystem(0);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.insert(config);
        return ConfigVO.from(config);
    }

    @Transactional
    public ConfigVO update(ConfigRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要修改的参数");
        validate(request, false);
        SysConfig config = configMapper.selectById(request.getId());
        if (config == null) throw new IllegalArgumentException("参数不存在");
        // 键名不可改，避免业务方读取不到
        apply(config, request);
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);
        return ConfigVO.from(config);
    }

    /** 批量保存：前端一次性提交整组参数值。 */
    @Transactional
    public int batchSave(List<ConfigRequest> requests) {
        if (requests == null || requests.isEmpty()) return 0;
        int updated = 0;
        for (ConfigRequest request : requests) {
            if (request.getId() == null) continue;
            SysConfig config = configMapper.selectById(request.getId());
            if (config == null) continue;
            config.setConfigValue(request.getConfigValue());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
            updated++;
        }
        return updated;
    }

    public void delete(ConfigDeleteRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要删除的参数");
        SysConfig config = configMapper.selectById(request.getId());
        if (config == null) throw new IllegalArgumentException("参数不存在");
        if (config.getIsSystem() != null && config.getIsSystem() == 1) {
            throw new IllegalStateException("系统内置参数不允许删除");
        }
        configMapper.deleteById(request.getId());
    }

    private void validate(ConfigRequest request, boolean requireKey) {
        if (requireKey && !StringUtils.hasText(request.getConfigKey())) {
            throw new IllegalArgumentException("请填写参数键名");
        }
        if (!StringUtils.hasText(request.getConfigName())) {
            throw new IllegalArgumentException("请填写参数名称");
        }
        String type = request.getValueType();
        if (StringUtils.hasText(type) && !List.of("text", "number", "boolean", "select", "textarea").contains(type)) {
            throw new IllegalArgumentException("参数类型不合法：" + type);
        }
        if ("number".equals(type) && StringUtils.hasText(request.getConfigValue())
                && !request.getConfigValue().trim().matches("-?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("参数值必须为数字");
        }
    }

    private void apply(SysConfig config, ConfigRequest request) {
        if (StringUtils.hasText(request.getConfigKey()) && config.getId() == null) {
            config.setConfigKey(request.getConfigKey().trim());
        }
        config.setConfigName(request.getConfigName().trim());
        config.setConfigValue(request.getConfigValue());
        config.setValueType(StringUtils.hasText(request.getValueType()) ? request.getValueType() : "text");
        config.setOptions(request.getOptions());
        config.setConfigGroup(StringUtils.hasText(request.getConfigGroup()) ? request.getConfigGroup() : "通用");
        config.setDescription(request.getDescription());
        config.setSort(request.getSort() == null ? 0 : request.getSort());
        config.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private Map<String, Object> stats() {
        List<SysConfig> all = configMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("enabled", all.stream().filter(c -> c.getStatus() != null && c.getStatus() == 1).count());
        stats.put("disabled", all.stream().filter(c -> c.getStatus() == null || c.getStatus() != 1).count());
        stats.put("groupCount", all.stream()
                .map(SysConfig::getConfigGroup)
                .filter(StringUtils::hasText)
                .distinct()
                .count());
        return stats;
    }
}
