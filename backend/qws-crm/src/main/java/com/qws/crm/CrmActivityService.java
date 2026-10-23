package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.activity.ActivityDeleteRequest;
import com.qws.common.dto.activity.ActivityListResult;
import com.qws.common.dto.activity.ActivityRequest;
import com.qws.common.dto.activity.ActivityVO;
import com.qws.common.entity.CrmActivity;
import com.qws.common.entity.CrmCustomer;
import com.qws.common.mapper.CrmActivityMapper;
import com.qws.common.mapper.CrmCustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CRM 跟进活动：拜访、电话、会议等记录的维护。 */
@Service
public class CrmActivityService {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CrmActivityMapper activityMapper;
    private final CrmCustomerMapper customerMapper;

    public CrmActivityService(CrmActivityMapper activityMapper, CrmCustomerMapper customerMapper) {
        this.activityMapper = activityMapper;
        this.customerMapper = customerMapper;
    }

    public ActivityListResult list(String keyword, String status, String type, Long customerId, int page, int pageSize) {
        LambdaQueryWrapper<CrmActivity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmActivity::getTitle, keyword)
                    .or().like(CrmActivity::getCustomerName, keyword)
                    .or().like(CrmActivity::getOwner, keyword));
        }
        if (StringUtils.hasText(status)) wrapper.eq(CrmActivity::getStatus, status);
        if (StringUtils.hasText(type)) wrapper.eq(CrmActivity::getType, type);
        if (customerId != null) wrapper.eq(CrmActivity::getCustomerId, customerId);
        wrapper.orderByDesc(CrmActivity::getActivityAt).orderByDesc(CrmActivity::getId);

        Page<CrmActivity> result = activityMapper.selectPage(new Page<>(page, pageSize), wrapper);
        ActivityListResult payload = new ActivityListResult();
        payload.setRows(result.getRecords().stream().map(ActivityVO::from).toList());
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        return payload;
    }

    public ActivityVO detail(Long id) {
        return ActivityVO.from(requireActivity(id));
    }

    @Transactional
    public ActivityVO create(ActivityRequest request) {
        validate(request);
        CrmActivity activity = new CrmActivity();
        apply(activity, request);
        activity.setCreateTime(LocalDateTime.now());
        activityMapper.insert(activity);
        return ActivityVO.from(activity);
    }

    @Transactional
    public ActivityVO update(ActivityRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要修改的跟进记录");
        validate(request);
        CrmActivity activity = requireActivity(request.getId());
        apply(activity, request);
        activityMapper.updateById(activity);
        return ActivityVO.from(activity);
    }

    public void delete(ActivityDeleteRequest request) {
        if (request.getId() == null && (request.getIds() == null || request.getIds().isEmpty())) {
            throw new IllegalArgumentException("请选择要删除的跟进记录");
        }
        if (request.getId() != null) {
            requireActivity(request.getId());
            activityMapper.deleteById(request.getId());
            return;
        }
        activityMapper.deleteByIds(request.getIds());
    }

    public List<Map<String, Object>> customerOptions() {
        return customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
                        .orderByDesc(CrmCustomer::getUpdateTime)).stream()
                .map(c -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", c.getId());
                    option.put("name", c.getName());
                    return option;
                }).toList();
    }

    private CrmActivity requireActivity(Long id) {
        CrmActivity activity = activityMapper.selectById(id);
        if (activity == null) throw new IllegalArgumentException("跟进记录不存在");
        return activity;
    }

    private void validate(ActivityRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("请填写跟进主题");
        }
        if (request.getTitle().length() > 120) {
            throw new IllegalArgumentException("跟进主题长度不能超过120个字符");
        }
    }

    private void apply(CrmActivity activity, ActivityRequest request) {
        activity.setTitle(request.getTitle().trim());
        activity.setType(StringUtils.hasText(request.getType()) ? request.getType() : "visit");
        activity.setOwner(request.getOwner());
        activity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "待处理");
        activity.setRemark(request.getRemark());
        activity.setActivityAt(parseDateTime(request.getActivityAt()));

        Long customerId = request.getCustomerId();
        String customerName = request.getCustomerName();
        if (customerId != null) {
            CrmCustomer customer = customerMapper.selectById(customerId);
            if (customer != null) {
                customerName = customer.getName();
            }
        } else if (StringUtils.hasText(customerName)) {
            CrmCustomer customer = customerMapper.selectOne(new LambdaQueryWrapper<CrmCustomer>()
                    .eq(CrmCustomer::getName, customerName.trim()).last("LIMIT 1"));
            if (customer != null) {
                customerId = customer.getId();
            }
        }
        activity.setCustomerId(customerId);
        activity.setCustomerName(StringUtils.hasText(customerName) ? customerName.trim() : null);
    }

    private LocalDateTime parseDateTime(String text) {
        if (!StringUtils.hasText(text)) return LocalDateTime.now();
        String value = text.trim().replace('T', ' ');
        try {
            if (value.length() <= 10) {
                return LocalDate.parse(value.substring(0, 10)).atStartOfDay();
            }
            if (value.length() == 16) {
                return LocalDateTime.parse(value, DATETIME);
            }
            return LocalDateTime.parse(value.substring(0, 19).replace(' ', 'T'));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("跟进时间格式不正确，应为 yyyy-MM-dd HH:mm");
        }
    }

    private Map<String, Object> computeStats() {
        List<CrmActivity> all = activityMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();
        stats.put("total", all.size());
        stats.put("pending", all.stream().filter(a -> "待处理".equals(a.getStatus())).count());
        stats.put("done", all.stream().filter(a -> "已完成".equals(a.getStatus())).count());
        stats.put("todayCount", all.stream()
                .filter(a -> a.getActivityAt() != null && a.getActivityAt().toLocalDate().isEqual(today))
                .count());
        stats.put("customerCount", all.stream()
                .map(CrmActivity::getCustomerName)
                .filter(StringUtils::hasText)
                .distinct()
                .count());
        return stats;
    }
}
