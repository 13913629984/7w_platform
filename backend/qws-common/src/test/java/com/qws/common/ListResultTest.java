package com.qws.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一列表响应体测试。
 *
 * <p>第 2 批把 WMS 的裸数组接口收敛成 {@code {rows,total,page,pageSize}}，
 * 这里固定住那个契约。
 */
class ListResultTest {

    @Test
    @DisplayName("不分页列表：total 等于条数，page=1，pageSize=total")
    void ofList() {
        ListResult<String> result = ListResult.of(List.of("a", "b", "c"));
        assertEquals(3, result.rows().size());
        assertEquals(3, result.total());
        assertEquals(1, result.page());
        assertEquals(3, result.pageSize());
        assertNull(result.stats());
    }

    @Test
    @DisplayName("空列表不抛 NPE")
    void ofNull() {
        ListResult<String> result = ListResult.of(null);
        assertNull(result.rows());
        assertEquals(0, result.total());
    }

    @Test
    @DisplayName("分页列表保留原始分页信息")
    void pageResult() {
        ListResult<String> result = ListResult.page(List.of("a"), 100, 3, 20);
        assertEquals(1, result.rows().size());
        assertEquals(100, result.total());
        assertEquals(3, result.page());
        assertEquals(20, result.pageSize());
    }
}
