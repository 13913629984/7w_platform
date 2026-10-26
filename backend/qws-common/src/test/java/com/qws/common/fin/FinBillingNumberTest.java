package com.qws.common.fin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 财务自动建单的方法契约测试（不依赖数据库）。
 *
 * <p>为何要锁定签名：这两个方法被出库/入库事务直接调用，返回值 {@code null}
 * 表示"跳过建单"（金额为 0、客户为空等正常业务情况），<b>不能抛异常</b>，
 * 否则整个出库事务会回滚。签名或异常声明变了就是破坏性变更。
 */
class FinBillingNumberTest {

    @Test
    @DisplayName("销售应收：public String createReceivableForSales(...)，不抛受检异常")
    void receivableContract() throws Exception {
        var method = FinBillingService.class.getMethod("createReceivableForSales",
                String.class, String.class, String.class, java.math.BigDecimal.class, int.class);
        assertNotNull(method);
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
        assertEquals(String.class, method.getReturnType());
        assertArrayEquals(new Class<?>[0], method.getExceptionTypes(), "不应声明受检异常");
    }

    @Test
    @DisplayName("采购应付：public String createPayableForPurchase(...)，不抛受检异常")
    void payableContract() throws Exception {
        var method = FinBillingService.class.getMethod("createPayableForPurchase",
                String.class, String.class, String.class,
                java.math.BigDecimal.class, java.math.BigDecimal.class);
        assertEquals(String.class, method.getReturnType());
        assertArrayEquals(new Class<?>[0], method.getExceptionTypes());
    }

    @Test
    @DisplayName("零元流水方法存在（金额为 0 时只记流水不建应收）")
    void recordSalesWithoutAmountExists() throws Exception {
        var method = FinBillingService.class.getMethod("recordSalesWithoutAmount",
                String.class, String.class, java.time.LocalDateTime.class);
        assertNotNull(method);
    }
}
