/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.beeline.fdmpackloader.utils.Constants.*;

public class RequestContext {
    private static final ThreadLocal<Map<String, Object>> headersThreadLocal = new ThreadLocal<>();

    public static void setHeaders(Map<String, Object> headers) {
        headersThreadLocal.set(headers);
    }

    public static Map<String, Object> getHeaders() {
        return headersThreadLocal.get();
    }

    public static List<String> getUserPermissions() {
        return (List<String>) getHeaders().get(USER_PERMISSION_HEADER);
    }
    public static List<String> getUserRoles() {
        return (List<String>) getHeaders().get(USER_ROLES_HEADER);
    }

    public static List<Long> getUserProducts() {
        List<String> stringList = (List<String>) getHeaders().get(USER_PRODUCTS_IDS_HEADER);
        List<Long> longList = stringList.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return longList;
    }
}
