package com.example.bookshop2.aspect;

import com.example.bookshop2.service.VisitCounterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VisitCounterInterceptor implements HandlerInterceptor {
    private final VisitCounterService counterService;

    public VisitCounterInterceptor(VisitCounterService counterService) {
        this.counterService = counterService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String path = request.getRequestURI();
        counterService.increment(path);
        return true;
    }
}