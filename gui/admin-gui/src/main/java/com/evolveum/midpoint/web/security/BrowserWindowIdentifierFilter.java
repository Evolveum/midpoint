/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter checks existence of "w" query parameter in URL, if not present, it redirects to the same URL with "w" parameter added.
 * This is used to distinguish different browser windows/tabs and prevent session sharing between them.
 *
 * @author Viliam Repan
 */
public class BrowserWindowIdentifierFilter extends OncePerRequestFilter {

    public static final String PARAM_WI = "w";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Only handle top-level GET navigations
        if (!"GET".equalsIgnoreCase(request.getMethod()) || isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = getPathWithoutContext(request);
        if (path.equals("/favicon.ico") || Objects.equals(path, "/status.html")
                || path.startsWith("/css")
                || path.startsWith("/fonts")
                || path.startsWith("/img")
                || path.startsWith("/js")
                || path.startsWith("/static")
                || path.startsWith("/wicket/resource")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If parameter present and non-empty, continue normally
        String existing = request.getParameter(PARAM_WI);
        if (existing != null && !existing.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Build redirect URL with appended wi parameter
        String requestURL = request.getRequestURL().toString();
        String query = request.getQueryString(); // may be null
        String wi = URLEncoder.encode(UUID.randomUUID().toString().substring(0, 8), StandardCharsets.UTF_8);
        request.setAttribute(PARAM_WI, wi);

        StringBuilder newUrl = new StringBuilder(requestURL);
        if (StringUtils.isNotEmpty(query)) {
            newUrl.append("?").append(query).append("&");
        }

        newUrl.append(PARAM_WI).append("=").append(wi);

        response.sendRedirect(newUrl.toString());
    }

    public static String getPathWithoutContext(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String uri = request.getRequestURI();           // e.g. "/app/foo/bar"
        String context = request.getContextPath();     // e.g. "/app" or ""
        if (uri == null) {
            return null;
        }
        return uri.startsWith(context) ? uri.substring(context.length()) : uri;
    }
}
