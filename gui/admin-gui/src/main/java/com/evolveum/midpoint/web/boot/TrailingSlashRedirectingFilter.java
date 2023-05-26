/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.web.application.PageMounter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingSlashRedirectingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String pathWithoutContextPath;
        String uri = request.getRequestURI();
        if (StringUtils.isNotBlank(request.getContextPath()) && uri.startsWith(request.getContextPath())) {
            if (uri.length() == request.getContextPath().length()) {
                pathWithoutContextPath = "";
            } else {
                pathWithoutContextPath = uri.substring(request.getContextPath().length());
            }
        } else {
            pathWithoutContextPath = uri;
        }
        if (StringUtils.isNotBlank(pathWithoutContextPath) && !pathWithoutContextPath.equals("/") && pathWithoutContextPath.endsWith("/")) {
            String pathWithoutLastSlash = pathWithoutContextPath.replaceFirst(".$","");
            if (!PageMounter.getUrlClassMap().containsKey(pathWithoutLastSlash)) { // use redirect only for GUI pages
                filterChain.doFilter(request, response);
                return;
            }
            ServletUriComponentsBuilder builder =
                    ServletUriComponentsBuilder.fromRequest(request);
            builder.replacePath(builder.build().getPath().replaceFirst(".$",""));
            response.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
            response.setHeader(HttpHeaders.LOCATION,
                    builder.toUriString());
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
