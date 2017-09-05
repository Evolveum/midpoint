/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.aspect.ProfilingDataLog;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.DecimalFormat;

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

        if(LOGGER.isTraceEnabled()){
            long startTime = System.nanoTime();

            try {
            	chain.doFilter(request, response);
        	} catch (IOException | ServletException | RuntimeException | Error e) {
        		LOGGER.error("Encountered exception: {}: {}", e.getClass().getName(), e.getMessage(), e);
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
        		LOGGER.error("Encountered exception: {}: {}", e.getClass().getName(), e.getMessage(), e);
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


}
