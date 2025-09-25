package com.example.bookshop2.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Pointcut("within(com.example.bookshop2.controller..*)")
    public void controllerMethods() {
    }

    @Before("controllerMethods()")
    public void logRequest(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("Request attributes not available for logging");
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String params = getParams(joinPoint.getArgs());
        log.info("[{}] {} - {} with params: {}", request.getMethod(), request.getRequestURI(),
                joinPoint.getSignature(), params);
    }

    @AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("Request attributes not available for logging exception");
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        log.error("[ERROR] [{}] {} - {}: {}", request.getMethod(), request.getRequestURI(),
                joinPoint.getSignature(), ex.getMessage());
    }

    private String getParams(Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg != null && !(arg instanceof HttpServletRequest)) {
                sb.append(arg.toString()).append(", ");
            }
        }
        return !sb.isEmpty() ? sb.substring(0, sb.length() - 2) : "";
    }
}