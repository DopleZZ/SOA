package ru.itmo.soa.vehicle.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CorsFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;
    private final boolean allowAll;

    public CorsFilter(@Value("${cors.allowed-origins:https://se.ifmo.ru}") String origins) {
        allowAll = origins.trim().equals("*");
        allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && (allowAll || allowedOrigins.contains(origin))) {
            response.setHeader("Access-Control-Allow-Origin", allowAll ? "*" : origin);
            response.addHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
            response.setHeader("Access-Control-Max-Age", "86400");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        chain.doFilter(request, response);
    }
}
