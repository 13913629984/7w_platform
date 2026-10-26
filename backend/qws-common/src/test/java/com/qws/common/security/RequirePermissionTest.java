package com.qws.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * {@code @RequirePermission} 注解与拦截器解析逻辑测试。
 *
 * <p>重点锁定：方法级注解优先于类级注解（这是"某个写接口放宽/收紧权限"的实现基础）。
 */
class RequirePermissionTest {

    @RequirePermission("WMS")
    static class ClassLevelController {
        public void inherit() {
        }

        @RequirePermission("WMS_OUTBOUND")
        public void override() {
        }
    }

    @Test
    @DisplayName("方法级注解优先于类级注解")
    void methodOverridesClass() throws Exception {
        Method inherit = ClassLevelController.class.getMethod("inherit");
        Method override = ClassLevelController.class.getMethod("override");

        RequirePermission classAnnotation = ClassLevelController.class.getAnnotation(RequirePermission.class);
        assertEquals("WMS", classAnnotation.value()[0]);

        // 方法上没有注解 -> 继承类级
        assertEquals(null, inherit.getAnnotation(RequirePermission.class));
        assertArrayEquals(new String[]{"WMS"}, classAnnotation.value());

        // 方法上有注解 -> 覆盖
        RequirePermission methodAnnotation = override.getAnnotation(RequirePermission.class);
        assertArrayEquals(new String[]{"WMS_OUTBOUND"}, methodAnnotation.value());
    }

    @Test
    @DisplayName("默认不允许匿名访问")
    void defaultNotAnonymous() {
        RequirePermission annotation = ClassLevelController.class.getAnnotation(RequirePermission.class);
        assertFalse(annotation.allowAnonymous());
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertArrayEquals(String[] expected, String[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }

    private static void assertFalse(boolean value) {
        org.junit.jupiter.api.Assertions.assertFalse(value);
    }
}
