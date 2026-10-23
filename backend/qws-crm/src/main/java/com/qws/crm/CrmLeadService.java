package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.lead.LeadConvertRequest;
import com.qws.common.dto.lead.LeadDeleteRequest;
import com.qws.common.dto.lead.LeadListResult;
import com.qws.common.dto.lead.LeadRequest;
import com.qws.common.dto.lead.LeadVO;
import com.qws.common.entity.CrmCustomer;
import com.qws.common.entity.CrmLead;
import com.qws.common.mapper.CrmCustomerMapper;
import com.qws.common.mapper.CrmLeadMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CRM 线索：维护、跟进、转化为客户。 */
@Service
public class CrmLeadService {

    private final CrmLeadMapper leadMapper;
    private final CrmCustomerMapper customerMapper;

    public CrmLeadService(CrmLeadMapper leadMapper, CrmCustomerMapper customerMapper) {
        this.leadMapper = leadMapper;
        this.customerMapper = customerMapper;
    }

    public LeadListResult list(String keyword, String status, String source, String owner, int page, int pageSize) {
        LambdaQueryWrapper<CrmLead> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmLead::getName, keyword)
                    .or().like(CrmLead::getRemark, keyword)
                    .or().like(CrmLead::getOwner, keyword));
        }
        if (StringUtils.hasText(status)) wrapper.eq(CrmLead::getStatus, status);
        if (StringUtils.hasText(source)) wrapper.eq(CrmLead::getSource, source);
        if (StringUtils.hasText(owner)) wrapper.eq(CrmLead::getOwner, owner);
        wrapper.orderByDesc(CrmLead::getCreateTime).orderByDesc(CrmLead::getId);

        Page<CrmLead> result = leadMapper.selectPage(new Page<>(page, pageSize), wrapper);
        LeadListResult payload = new LeadListResult();
        payload.setRows(result.getRecords().stream().map(LeadVO::from).toList());
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        return payload;
    }

    public LeadVO detail(Long id) {
        return LeadVO.from(requireLead(id));
    }

    @Transactional
    public LeadVO create(LeadRequest request) {
        validate(request);
        CrmLead lead = new CrmLead();
        apply(lead, request);
        lead.setCreateTime(LocalDateTime.now());
        lead.setUpdateTime(LocalDateTime.now());
        leadMapper.insert(lead);
        return LeadVO.from(lead);
    }

    @Transactional
    public LeadVO update(LeadRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要修改的线索");
        validate(request);
        CrmLead lead = requireLead(request.getId());
        apply(lead, request);
        lead.setUpdateTime(LocalDateTime.now());
        leadMapper.updateById(lead);
        return LeadVO.from(lead);
    }

    public void delete(LeadDeleteRequest request) {
        if (request.getId() == null && (request.getIds() == null || request.getIds().isEmpty())) {
            throw new IllegalArgumentException("请选择要删除的线索");
        }
        if (request.getId() != null) {
            requireLead(request.getId());
            leadMapper.deleteById(request.getId());
            return;
        }
        leadMapper.deleteByIds(request.getIds());
    }

    /** 线索转客户：校验同名客户，创建客户并把线索置为"已转化"。 */
    @Transactional
    public Long convert(LeadConvertRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("请选择要转化的线索");
        CrmLead lead = requireLead(request.getId());
        String customerName = StringUtils.hasText(request.getCustomerName())
                ? request.getCustomerName().trim() : lead.getName();

        CrmCustomer exists = customerMapper.selectOne(new LambdaQueryWrapper<CrmCustomer>()
                .eq(CrmCustomer::getName, customerName).last("LIMIT 1"));
        if (exists != null) {
            throw new IllegalStateException("客户已存在：" + customerName);
        }

        CrmCustomer customer = new CrmCustomer();
        customer.setName(customerName);
        customer.setOwner(StringUtils.hasText(request.getOwner()) ? request.getOwner() : lead.getOwner());
        customer.setAddress("");
        customer.setLevel("C");
        customer.setStatus(1);
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.insert(customer);

        lead.setStatus("已转化");
        lead.setUpdateTime(LocalDateTime.now());
        leadMapper.updateById(lead);
        return customer.getId();
    }

    private CrmLead requireLead(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) throw new IllegalArgumentException("线索不存在");
        return lead;
    }

    private void validate(LeadRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写线索名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("线索名称长度不能超过100个字符");
        }
    }

    private void apply(CrmLead lead, LeadRequest request) {
        lead.setName(request.getName().trim());
        lead.setSource(request.getSource());
        lead.setOwner(request.getOwner());
        lead.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "待跟进");
        lead.setRemark(request.getRemark());
        lead.setFollowAt(parseDate(request.getFollowAt()));
    }

    private LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) return null;
        try {
            return LocalDate.parse(text.trim().substring(0, Math.min(10, text.trim().length())));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("跟进日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    private Map<String, Object> computeStats() {
        List<CrmLead> all = leadMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        stats.put("total", all.size());
        stats.put("waiting", all.stream().filter(l -> "待跟进".equals(l.getStatus())).count());
        stats.put("following", all.stream().filter(l -> "跟进中".equals(l.getStatus())).count());
        stats.put("converted", all.stream().filter(l -> "已转化".equals(l.getStatus())).count());
        stats.put("monthAdded", all.stream()
                .filter(l -> l.getCreateTime() != null && !l.getCreateTime().toLocalDate().isBefore(firstDayOfMonth))
                .count());
        return stats;
    }
}
