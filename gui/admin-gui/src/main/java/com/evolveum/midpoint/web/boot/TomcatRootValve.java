/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import jakarta.servlet.ServletException;
import java.io.IOException;

/**
 * Tomcat valve used to redirect root (/) URL to real application (/midpoint/).
 * This is needed in Spring boot deployment. Entire midPoint app is deployed
 * under http://.../midpoint/ URL root. But we want users to use http://.../
 * as well to access the application.
 * <p>
 * This could not be done with midPoint servlets or servlet filters. The entire
 * midPoint application is under /midpoint/, so the application won't even receive
 * requests to root URL. We need to use dirty Tomcat-specific tricks for this.
 *
 * @author semancik
 */
public class TomcatRootValve extends ValveBase {

    private static final Trace LOGGER = TraceManager.getTrace(TomcatRootValve.class);

    private String servletPath;

    public TomcatRootValve(String servletPath) {
        super();

        this.servletPath = servletPath == null ? "" : servletPath;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        Context context = request.getContext();
        if (context instanceof RootRootContext) {
            String uri = request.getDecodedRequestURI();
            if (uri.endsWith("favicon.ico")) {
                LOGGER.trace("Redirecting favicon request to real application (URI={})", request.getDecodedRequestURI());
                response.sendRedirect(servletPath + "/favicon.ico");
                return;
            } else {
                LOGGER.trace("Redirecting request to real application root (URI={})", request.getDecodedRequestURI());
                response.sendRedirect(servletPath + "/");
                return;
            }
        }

        getNext().invoke(request, response);
    }

}
