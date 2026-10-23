package com.qws.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.dict.DictDeleteRequest;
import com.qws.common.dto.dict.DictItemRequest;
import com.qws.common.dto.dict.DictItemVO;
import com.qws.common.dto.dict.DictTypeRequest;
import com.qws.common.dto.dict.DictTypeVO;
import com.qws.common.entity.SysDictItem;
import com.qws.common.entity.SysDictType;
import com.qws.common.mapper.SysDictItemMapper;
import com.qws.common.mapper.SysDictTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 数据字典：字典类型维护 + 字典项维护。 */
@Service
public class DictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictItemMapper itemMapper;

    public DictService(SysDictTypeMapper typeMapper, SysDictItemMapper itemMapper) {
        this.typeMapper = typeMapper;
        this.itemMapper = itemMapper;
    }

    public Map<String, Object> listTypes(String keyword, Integer status) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysDictType::getDictCode, keyword)
                    .or().like(SysDictType::getDictName, keyword)
                    .or().like(SysDictType::getDescription, keyword));
        }
        if (status != null) {
            wrapper.eq(SysDictType::getStatus, status);
        }
        wrapper.orderByAsc(SysDictType::getSort).orderByAsc(SysDictType::getId);

        List<DictTypeVO> rows = typeMapper.selectList(wrapper).stream().map(type -> {
            DictTypeVO vo = DictTypeVO.from(type);
            Long count = itemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
                    .eq(SysDictItem::getDictCode, type.getDictCode()));
            vo.setItemCount(count == null ? 0 : count.intValue());
            return vo;
        }).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", rows);
        payload.put("total", rows.size());
        payload.put("stats", typeStats());
        return payload;
    }

    public List<DictItemVO> listItems(String dictCode) {
        if (!StringUtils.hasText(dictCode)) {
            throw new IllegalArgumentException("请指定字典编码");
        }
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictCode, dictCode)
                        .orderByAsc(SysDictItem::getSort)
                        .orderByAsc(SysDictItem::getId))
                .stream().map(DictItemVO::from).toList();
    }

    @Transactional
    public DictTypeVO createType(DictTypeRequest request) {
        validateType(request, true);
        Long exists = typeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, request.getDictCode().trim()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("字典编码已存在：" + request.getDictCode());
        }
        SysDictType type = new SysDictType();
        applyType(type, request);
        type.setIsSystem(0);
        type.setCreateTime(LocalDateTime.now());
        type.setUpdateTime(LocalDateTime.now());
        typeMapper.insert(type);
        return DictTypeVO.from(type);
    }

    @Transactional
    public DictTypeVO updateType(DictTypeRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要修改的字典");
        validateType(request, false);
        SysDictType type = typeMapper.selectById(request.getId());
        if (type == null) throw new IllegalArgumentException("字典不存在");
        applyType(type, request);
        type.setUpdateTime(LocalDateTime.now());
        typeMapper.updateById(type);
        return DictTypeVO.from(type);
    }

    @Transactional
    public void deleteType(DictDeleteRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要删除的字典");
        SysDictType type = typeMapper.selectById(request.getId());
        if (type == null) throw new IllegalArgumentException("字典不存在");
        if (type.getIsSystem() != null && type.getIsSystem() == 1) {
            throw new IllegalStateException("系统内置字典不允许删除");
        }
        // 级联删除字典项，避免脏数据
        itemMapper.delete(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictCode, type.getDictCode()));
        typeMapper.deleteById(request.getId());
    }

    @Transactional
    public DictItemVO createItem(DictItemRequest request) {
        validateItem(request, true);
        Long exists = itemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, request.getDictCode().trim())
                .eq(SysDictItem::getItemValue, request.getItemValue().trim()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("同一字典下字典值不能重复：" + request.getItemValue());
        }
        SysDictItem item = new SysDictItem();
        applyItem(item, request);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.insert(item);
        return DictItemVO.from(item);
    }

    @Transactional
    public DictItemVO updateItem(DictItemRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要修改的字典项");
        validateItem(request, false);
        SysDictItem item = itemMapper.selectById(request.getId());
        if (item == null) throw new IllegalArgumentException("字典项不存在");
        applyItem(item, request);
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.updateById(item);
        return DictItemVO.from(item);
    }

    @Transactional
    public void deleteItem(DictDeleteRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要删除的字典项");
        SysDictItem item = itemMapper.selectById(request.getId());
        if (item == null) throw new IllegalArgumentException("字典项不存在");
        itemMapper.deleteById(request.getId());
    }

    private void validateType(DictTypeRequest request, boolean requireCode) {
        if (requireCode && !StringUtils.hasText(request.getDictCode())) {
            throw new IllegalArgumentException("请填写字典编码");
        }
        if (!StringUtils.hasText(request.getDictName())) {
            throw new IllegalArgumentException("请填写字典名称");
        }
    }

    private void validateItem(DictItemRequest request, boolean requireCode) {
        if (requireCode && !StringUtils.hasText(request.getDictCode())) {
            throw new IllegalArgumentException("请指定字典编码");
        }
        if (!StringUtils.hasText(request.getItemValue())) {
            throw new IllegalArgumentException("请填写字典值");
        }
        if (!StringUtils.hasText(request.getItemLabel())) {
            throw new IllegalArgumentException("请填写字典标签");
        }
    }

    private void applyType(SysDictType type, DictTypeRequest request) {
        if (StringUtils.hasText(request.getDictCode()) && type.getId() == null) {
            type.setDictCode(request.getDictCode().trim());
        }
        type.setDictName(request.getDictName().trim());
        type.setDescription(request.getDescription());
        type.setSort(request.getSort() == null ? 0 : request.getSort());
        type.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private void applyItem(SysDictItem item, DictItemRequest request) {
        item.setDictCode(StringUtils.hasText(request.getDictCode()) ? request.getDictCode().trim() : item.getDictCode());
        item.setItemValue(request.getItemValue().trim());
        item.setItemLabel(request.getItemLabel().trim());
        item.setColor(StringUtils.hasText(request.getColor()) ? request.getColor() : "primary");
        item.setSort(request.getSort() == null ? 0 : request.getSort());
        item.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        item.setRemark(request.getRemark());
    }

    private Map<String, Object> typeStats() {
        List<SysDictType> all = typeMapper.selectList(null);
        Long itemTotal = itemMapper.selectCount(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("enabled", all.stream().filter(t -> t.getStatus() != null && t.getStatus() == 1).count());
        stats.put("itemTotal", itemTotal == null ? 0 : itemTotal);
        stats.put("systemCount", all.stream().filter(t -> t.getIsSystem() != null && t.getIsSystem() == 1).count());
        return stats;
    }
}
