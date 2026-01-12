package com.nirmani.btcexplainer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 60;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, ClientWindow> clients = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = Instant.now().getEpochSecond();

        ClientWindow window = clients.computeIfAbsent(ip, k -> new ClientWindow(now, 0));

        synchronized (window) {
            if (now - window.start >= WINDOW_SECONDS) {
                window.start = now;
                window.count = 0;
            }

            window.count++;

            if (window.count > MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Rate limit exceeded. Try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static class ClientWindow {
        long start;
        int count;

        ClientWindow(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }
}
