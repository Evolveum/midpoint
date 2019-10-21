/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.util.time.Time;
import org.springframework.security.web.DefaultRedirectStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WicketRedirectStrategy extends DefaultRedirectStrategy {

    @Override
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);

        response.setContentType("text/xml");

        response.setHeader("Ajax-Location", url);
        // disabled caching
        response.setHeader("Date", Long.toString(Time.now().getMilliseconds()));
        response.setHeader("Expires", Long.toString(Time.START_OF_UNIX_TIME.getMilliseconds()));
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, no-store");

        Writer writer = response.getWriter();
        writer.write("<ajax-response><redirect><![CDATA[" + url + "]]></redirect></ajax-response>");
    }

    public static boolean isWicketAjaxRequest(HttpServletRequest request) {
        String value = request.getParameter(WebRequest.PARAM_AJAX);
        if (Boolean.parseBoolean(value)) {
            return true;
        }

        value = request.getHeader(WebRequest.HEADER_AJAX);
        if (Boolean.parseBoolean(value)) {
            return true;
        }

        return false;
    }
}
