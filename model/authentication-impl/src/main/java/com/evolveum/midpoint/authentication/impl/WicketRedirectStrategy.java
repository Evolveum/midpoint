/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.wicket.request.http.WebRequest;
import org.springframework.security.web.DefaultRedirectStrategy;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WicketRedirectStrategy extends DefaultRedirectStrategy {

    @Override
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        String redirectUrl = calculateRedirectUrl(request, url);

        response.setStatus(HttpServletResponse.SC_OK);

        response.setContentType("text/xml");

        response.setHeader("Ajax-Location", redirectUrl);
        // disabled caching
        response.setHeader("Date", Long.toString(java.time.Instant.now().toEpochMilli()));
        response.setHeader("Expires", Long.toString(Instant.EPOCH.toEpochMilli()));
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, no-store");

        Writer writer = response.getWriter();
        writer.write("<ajax-response><redirect><![CDATA[" + redirectUrl + "]]></redirect></ajax-response>");
    }

    /**
     * Calculates a Wicket Ajax redirect URL without losing the servlet context path.
     *
     * Prefixes application-local paths like {@code /login}, but leaves already
     * context-prefixed, relative, protocol-relative, and {@code null} URLs unchanged.
     */
    private String calculateRedirectUrl(HttpServletRequest request, String url) {
        if (url == null || !url.startsWith("/") || url.startsWith("//")) {
            return url;
        }

        String contextPath = request.getContextPath();
        if (contextPath != null
                && !contextPath.isEmpty()
                && (url.equals(contextPath) || url.startsWith(contextPath + "/"))) {
            return url;
        }

        return calculateRedirectUrl(contextPath, url);
    }

    public static boolean isWicketAjaxRequest(HttpServletRequest request) {
        String value = request.getParameter(WebRequest.PARAM_AJAX);
        if (Boolean.parseBoolean(value)) {
            return true;
        }

        value = request.getHeader(WebRequest.HEADER_AJAX);
        return Boolean.parseBoolean(value);
    }
}
