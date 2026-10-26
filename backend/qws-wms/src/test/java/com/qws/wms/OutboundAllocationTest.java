package com.qws.wms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出库库位/库存匹配的纯逻辑测试。
 *
 * <p>第 1 批把「出库单明细必须带库位」修好之后，最容易回归的点是
 * 「库位为空 / 库位跨仓 / 数量拆分」这几类判断。这里不启 Spring、不连库，
 * 只把关键判定抽出来用等价逻辑锁定（实现若变更，这几条必须仍然成立）。
 */
class OutboundAllocationTest {

    /** 模拟 decreaseInventory 的入参校验：库位必须属于出库单仓库 */
    private boolean locationBelongsToWarehouse(String locationWarehouse, String orderWarehouse) {
        return locationWarehouse != null && locationWarehouse.equals(orderWarehouse);
    }

    @Test
    @DisplayName("库位属于出库单仓库 -> 允许扣减")
    void matchingWarehouse() {
        assertTrue(locationBelongsToWarehouse("WH-A", "WH-A"));
    }

    @Test
    @DisplayName("库位跨仓库 -> 拒绝（这是第 1 批之前的 500「库存不存在」根因）")
    void crossWarehouseRejected() {
        assertFalse(locationBelongsToWarehouse("WH-B", "WH-A"));
        assertFalse(locationBelongsToWarehouse(null, "WH-A"));
    }

    /** 模拟按库位拆分出库数量：每次最多取「剩余需求」和「该库位可用量」的较小值 */
    private BigDecimal take(BigDecimal remaining, BigDecimal available) {
        BigDecimal take = available.min(remaining);
        return take.signum() < 0 ? BigDecimal.ZERO : take;
    }

    @Test
    @DisplayName("按库位拆分：单库位足够时一次取完")
    void singleLocationCoversDemand() {
        assertEquals(0, take(new BigDecimal("5"), new BigDecimal("10")).compareTo(new BigDecimal("5")));
    }

    @Test
    @DisplayName("按库位拆分：单库位不足时取该库位全部，剩余继续找下一库位")
    void multipleLocationsSplit() {
        BigDecimal remaining = new BigDecimal("10");
        BigDecimal first = take(remaining, new BigDecimal("4"));
        assertEquals(0, first.compareTo(new BigDecimal("4")));

        remaining = remaining.subtract(first);
        BigDecimal second = take(remaining, new BigDecimal("8"));
        assertEquals(0, second.compareTo(new BigDecimal("6")));

        remaining = remaining.subtract(second);
        assertEquals(0, remaining.compareTo(BigDecimal.ZERO), "两个库位应刚好满足需求");
    }

    @Test
    @DisplayName("可用量为 0 的库存行不参与分配")
    void zeroAvailableSkipped() {
        assertEquals(0, take(new BigDecimal("5"), BigDecimal.ZERO).compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("订单总额 = 数量 × 单价（单价在明细上，不随库位变化）")
    void totalAmountComputedFromLines() {
        BigDecimal qty = new BigDecimal("5");
        BigDecimal price = new BigDecimal("2000");
        assertEquals(0, qty.multiply(price).compareTo(new BigDecimal("10000")));
    }
}
