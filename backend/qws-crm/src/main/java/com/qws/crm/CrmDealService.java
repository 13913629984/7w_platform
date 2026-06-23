package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.deal.DealBatchDeleteRequest;
import com.qws.common.dto.deal.DealDeleteRequest;
import com.qws.common.dto.deal.DealListResult;
import com.qws.common.dto.deal.DealRequest;
import com.qws.common.dto.deal.DealVO;
import com.qws.common.entity.CrmCustomer;
import com.qws.common.entity.CrmDeal;
import com.qws.common.entity.CrmOrder;
import com.qws.common.mapper.CrmCustomerMapper;
import com.qws.common.mapper.CrmDealMapper;
import com.qws.common.mapper.CrmOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CrmDealService {

    private static final List<String> STAGES = List.of("需求确认", "方案报价", "商务谈判", "即将签约");

    private final CrmDealMapper dealMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmOrderMapper orderMapper;

    public CrmDealService(CrmDealMapper dealMapper, CrmCustomerMapper customerMapper, CrmOrderMapper orderMapper) {
        this.dealMapper = dealMapper;
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
    }

    public DealListResult list(String keyword, String stage, String owner, int page, int pageSize) {
        LambdaQueryWrapper<CrmDeal> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmDeal::getName, keyword)
                    .or().like(CrmDeal::getCustomerName, keyword));
        }
        if (StringUtils.hasText(stage)) {
            wrapper.eq(CrmDeal::getStage, stage);
        }
        if (StringUtils.hasText(owner)) {
            wrapper.eq(CrmDeal::getOwner, owner);
        }
        wrapper.orderByDesc(CrmDeal::getCreateTime).orderByDesc(CrmDeal::getId);
        Page<CrmDeal> result = dealMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<DealVO> vos = result.getRecords().stream().map(DealVO::from).toList();
        DealListResult payload = new DealListResult();
        payload.setRows(vos);
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        payload.setStages(STAGES);
        return payload;
    }

    public DealVO detail(Long id) {
        CrmDeal deal = dealMapper.selectById(id);
        if (deal == null) {
            throw new IllegalArgumentException("商机不存在");
        }
        return DealVO.from(deal);
    }

    public DealVO create(DealRequest request) {
        validate(request);
        CrmDeal deal = new CrmDeal();
        applyRequest(deal, request);
        if (!StringUtils.hasText(deal.getStatus())) {
            deal.setStatus("进行中");
        }
        deal.setCreateTime(LocalDateTime.now());
        deal.setUpdateTime(LocalDateTime.now());
        dealMapper.insert(deal);
        return DealVO.from(deal);
    }

    public DealVO update(DealRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的商机");
        }
        validate(request);
        CrmDeal deal = dealMapper.selectById(request.getId());
        if (deal == null) {
            throw new IllegalArgumentException("商机不存在");
        }
        applyRequest(deal, request);
        deal.setUpdateTime(LocalDateTime.now());
        dealMapper.updateById(deal);
        return DealVO.from(deal);
    }

    public void delete(DealDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的商机");
        }
        CrmDeal deal = dealMapper.selectById(request.getId());
        if (deal == null) {
            throw new IllegalArgumentException("商机不存在");
        }
        dealMapper.deleteById(request.getId());
    }

    public int batchDelete(DealBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }
        return dealMapper.deleteByIds(request.getIds());
    }

    @Transactional
    public DealVO convert(Long id) {
        CrmDeal deal = dealMapper.selectById(id);
        if (deal == null) {
            throw new IllegalArgumentException("商机不存在");
        }
        if ("已转订单".equals(deal.getStatus())) {
            throw new IllegalStateException("该商机已转订单");
        }
        // 生成订单
        CrmOrder order = new CrmOrder();
        order.setOrderNo(nextOrderNo());
        order.setCustomerId(deal.getCustomerId());
        order.setCustomerName(deal.getCustomerName());
        order.setDealId(deal.getId());
        order.setDealName(deal.getName());
        order.setContractNo("HT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%03d", id));
        order.setAmount(deal.getAmount());
        order.setOwner(deal.getOwner());
        order.setSignDate(LocalDate.now());
        order.setExpectDeliverAt(deal.getExpectDealAt());
        order.setStatus("待确认");
        order.setRemark("由商机「" + deal.getName() + "」转换生成");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        // 更新商机状态
        deal.setStatus("已转订单");
        deal.setStage("即将签约");
        deal.setWinRate(100);
        deal.setUpdateTime(LocalDateTime.now());
        dealMapper.updateById(deal);
        return DealVO.from(deal);
    }

    /** 生成下一个订单编号：SO + yyyyMMdd + 3位流水 */
    private String nextOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "SO" + dateStr;
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<CrmOrder>()
                .likeRight(CrmOrder::getOrderNo, prefix));
        long seq = (count == null ? 0 : count) + 1;
        return prefix + String.format("%03d", seq);
    }

    private void validate(DealRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写商机名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("商机名称长度不能超过100个字符");
        }
        if (!StringUtils.hasText(request.getCustomerName()) && request.getCustomerId() == null) {
            throw new IllegalArgumentException("请填写所属客户");
        }
        if (StringUtils.hasText(request.getStage()) && !STAGES.contains(request.getStage())) {
            throw new IllegalArgumentException("销售阶段不合法");
        }
        if (request.getWinRate() != null && (request.getWinRate() < 0 || request.getWinRate() > 100)) {
            throw new IllegalArgumentException("胜率必须在0-100之间");
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("预计金额不能为负数");
        }
    }

    private void applyRequest(CrmDeal deal, DealRequest request) {
        deal.setName(request.getName().trim());
        deal.setLeadName(request.getLeadName());
        deal.setAmount(request.getAmount() == null ? BigDecimal.ZERO : request.getAmount());
        deal.setStage(request.getStage());
        deal.setWinRate(request.getWinRate());
        deal.setOwner(request.getOwner());
        deal.setExpectDealAt(parseDate(request.getExpectDealAt()));
        deal.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getStatus())) {
            deal.setStatus(request.getStatus());
        }
        // 解析关联客户：优先按 customerId，其次按名称匹配
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
        deal.setCustomerId(customerId);
        deal.setCustomerName(StringUtils.hasText(customerName) ? customerName.trim() : null);
    }

    private Map<String, Object> computeStats() {
        List<CrmDeal> all = dealMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        int total = all.size();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        long monthAdded = all.stream()
                .filter(d -> d.getCreateTime() != null && !d.getCreateTime().toLocalDate().isBefore(firstDayOfMonth))
                .count();
        BigDecimal totalAmount = all.stream()
                .map(d -> d.getAmount() == null ? BigDecimal.ZERO : d.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double avgWinRate = all.stream()
                .filter(d -> d.getWinRate() != null)
                .mapToInt(CrmDeal::getWinRate)
                .average()
                .orElse(0);
        stats.put("total", total);
        stats.put("monthAdded", monthAdded);
        stats.put("totalAmount", totalAmount.stripTrailingZeros().toPlainString());
        stats.put("avgWinRate", BigDecimal.valueOf(avgWinRate).setScale(0, RoundingMode.HALF_UP).intValue());
        return stats;
    }

    public List<Map<String, Object>> customerOptions() {
        return customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
                        .orderByDesc(CrmCustomer::getUpdateTime)).stream()
                .map(c -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", c.getId());
                    option.put("name", c.getName());
                    return option;
                }).collect(Collectors.toList());
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            // 兼容 ISO 与带时间的字符串
            String trimmed = value.trim();
            if (trimmed.length() > 10) {
                trimmed = trimmed.substring(0, 10);
            }
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ex) {
            return null;
        }
    }
}
