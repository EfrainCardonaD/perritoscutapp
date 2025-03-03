package com.cut.cardona.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int MAX_REQUESTS = 50000000; // Máximo número de solicitudes permitidas
    private final long TIME_WINDOW = 60 * 1000; // Ventana de tiempo en milisegundos (1 minuto)

    private final ConcurrentHashMap<String, RequestInfo> requestMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String clientIp = request.getRemoteAddr();
        long currentTime = Instant.now().toEpochMilli();

        requestMap.compute(clientIp, (ip, info) -> {
            if (info == null || currentTime - info.startTime > TIME_WINDOW) {
                return new RequestInfo(currentTime, new AtomicInteger(1));
            } else {
                info.requestCount.incrementAndGet();
                return info;
            }
        });

        RequestInfo info = requestMap.get(clientIp);
        if (info.requestCount.get() > MAX_REQUESTS) {
            response.setStatus(429);
            if (info.requestCount.get() > MAX_REQUESTS) {
                throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
            }

            response.getWriter().write("Rate limit exceeded. Try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class RequestInfo {
        long startTime;
        AtomicInteger requestCount;

        RequestInfo(long startTime, AtomicInteger requestCount) {
            this.startTime = startTime;
            this.requestCount = requestCount;
        }
    }
}