/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.beeline.fdmpackloader.exception.ForbiddenException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static ru.beeline.fdmpackloader.utils.Constants.ADMIN_ROLE;
import static ru.beeline.fdmpackloader.utils.Constants.USER_ROLES_HEADER;

@Aspect
@Component
public class AccessControlAspect {

    @Around("@annotation(ru.beeline.fdmpackloader.aspect.AdminAccessControl)")
    public Object checkAdminAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String userRoles = request.getHeader(USER_ROLES_HEADER);
        boolean isAdmin = toList(userRoles).stream().anyMatch(userRole -> userRole.equals(ADMIN_ROLE));
        if (!isAdmin) throw new ForbiddenException("403 Permission denied");
        return joinPoint.proceed();
    }

    private List<String> toList(String value) {
        return Arrays.asList(
                value.replaceAll("^\\[|\\]$|\"", "").split(",")
        );

    }

}
