/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import java.io.IOException;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Filter checks existence of "w" query parameter in URL, if not present, it wraps the request into custom
 * {@link ParameterRequestWrapper} with "w" parameter added.
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

        // Try to get parameter from Referer header value and wrap the request
        String wi = getWindowParameterFromRefererHeader(request);
        if (!StringUtils.isEmpty(wi)) {
            HttpServletRequest wrappedRequest =
                    new ParameterRequestWrapper(request, PARAM_WI, wi);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
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

    private String getWindowParameterFromRefererHeader(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            return UriComponentsBuilder.fromUriString(referer)
                    .build()
                    .getQueryParams()
                    .getFirst(PARAM_WI);
        }
        return null;
    }

}
