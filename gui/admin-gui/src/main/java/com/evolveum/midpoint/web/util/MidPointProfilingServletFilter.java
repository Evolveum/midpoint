/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import java.io.IOException;
import java.text.DecimalFormat;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.ClientAbortException;

import com.evolveum.midpoint.util.aspect.ProfilingDataLog;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *  //TODO - After upgrading to javax.servlet version API 3.0, add response status code logging
 *
 *  In this filter, all incoming requests are captured and we measure server response times (using System.nanoTime() for now),
 *  this may be later adjusted using Java SIMON API (but this API is based on System.nanoTime() as well).
 *
 *  Right now, we are logging this request/response information
 *      Requested URL
 *      Request method (GET/POST)
 *      Request session id
 *
 *  Requests for .css or various image files are filtered and not recorded.
 *
 *  @author lazyman
 *  @author shood
 */
public class MidPointProfilingServletFilter implements Filter {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointProfilingServletFilter.class);
    private static DecimalFormat df = new DecimalFormat("0.00");

    protected FilterConfig config;

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (LOGGER.isTraceEnabled()) {
            long startTime = System.nanoTime();

            try {
            	chain.doFilter(request, response);
        	} catch (IOException | ServletException | RuntimeException | Error e) {
                logException(e);
        		throw e;
        	}

            long elapsedTime = System.nanoTime() - startTime;

            if(request instanceof HttpServletRequest){
                String uri = ((HttpServletRequest)request).getRequestURI();

                if(uri.startsWith("/midpoint/admin")){
                    prepareRequestProfilingEvent(request, elapsedTime, uri);
                }
            }
        } else {
        	try {
        		chain.doFilter(request, response);
        	} catch (IOException | ServletException | RuntimeException | Error e) {
        		logException(e);
        		throw e;
        	}
         }
    }

    private void prepareRequestProfilingEvent(ServletRequest request, long elapsed, String uri){
        String info = ((HttpServletRequest)request).getMethod();
        String sessionId = ((HttpServletRequest)request).getRequestedSessionId();

        ProfilingDataLog event = new ProfilingDataLog(info, uri, sessionId, elapsed, System.currentTimeMillis());
        ProfilingDataManager.getInstance().prepareRequestProfilingEvent(event);
    }

    private void logException(Throwable t) throws IOException, ServletException {
        if (t instanceof ClientAbortException) {
            if (LOGGER.isDebugEnabled()) {
                // client abort exceptions are quite OK as they are not an application/server problem
                LOGGER.debug("Encountered exception: {}: {}", t.getClass().getName(), t.getMessage(), t);
            }
            return;
        }

        LOGGER.error("Encountered exception: {}: {}", t.getClass().getName(), t.getMessage(), t);
    }
}
