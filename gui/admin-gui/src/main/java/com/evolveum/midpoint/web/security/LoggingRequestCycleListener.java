/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.error.PageError;
import org.apache.wicket.Application;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.settings.RequestLoggerSettings;

/**
 * @author Viliam Repan (lazyman)
 */
public class LoggingRequestCycleListener extends AbstractRequestCycleListener {

    private static final Trace LOGGER = TraceManager.getTrace(LoggingRequestCycleListener.class);

    private static final String REQUEST_LOGGER_NAME = "com.evolveum.midpoint.request.web";

    private static final Trace REQUEST_LOGGER = TraceManager.getTrace(REQUEST_LOGGER_NAME);

    public LoggingRequestCycleListener(Application application) {
        if (REQUEST_LOGGER.isDebugEnabled()) {
            RequestLoggerSettings requestLoggerSettings = application.getRequestLoggerSettings();
            requestLoggerSettings.setRequestLoggerEnabled(true);
            if (REQUEST_LOGGER.isTraceEnabled()) {
                requestLoggerSettings.setRecordSessionSize(true);
            }
        }
    }

    @Override
    public IRequestHandler onException(RequestCycle cycle, Exception ex) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: Exception: {}, handler {}", ex,
                    WebComponentUtil.debugHandler(cycle.getActiveRequestHandler()), ex);
        }
        LoggingUtils.logUnexpectedException(LOGGER, "Error occurred during page rendering", ex);
        return new RenderPageRequestHandler(new PageProvider(new PageError(ex)));
    }

    @Override
    public void onRequestHandlerScheduled(RequestCycle cycle, IRequestHandler handler) {
        if (handler instanceof RenderPageRequestHandler) {
            Class<? extends IRequestablePage> pageClass = ((RenderPageRequestHandler) handler).getPageClass();
            if (REQUEST_LOGGER.isTraceEnabled()) {
                REQUEST_LOGGER.trace("REQUEST CYCLE: Scheduled redirect to page {}", pageClass);
            }
            if (PageError.class.isAssignableFrom(pageClass)) {
                REQUEST_LOGGER.info("REQUEST CYCLE: Scheduled redirect to error page {}", pageClass);
            }
        } else {
            if (REQUEST_LOGGER.isTraceEnabled()) {
                REQUEST_LOGGER.trace("REQUEST CYCLE: Scheduled request handler {}",
                        WebComponentUtil.debugHandler(handler));
            }
        }
        super.onRequestHandlerScheduled(cycle, handler);
    }

    @Override
    public void onRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: Resolved request handler {}",
                    WebComponentUtil.debugHandler(handler));
        }
    }

    @Override
    public void onBeginRequest(RequestCycle cycle) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: Begin request: '{}', handler {}",
                    cycle.getRequest().getOriginalUrl(),
                    WebComponentUtil.debugHandler(cycle.getActiveRequestHandler()));
        }
        super.onBeginRequest(cycle);
    }

    @Override
    public void onEndRequest(RequestCycle cycle) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: End request: '{}', next handler: {}",
                    cycle.getRequest().getOriginalUrl(),
                    WebComponentUtil.debugHandler(cycle.getRequestHandlerScheduledAfterCurrent()));
        }
        super.onBeginRequest(cycle);
    }

    @Override
    public void onDetach(RequestCycle cycle) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: Detach, request: '{}', next handler: {}",
                    cycle.getRequest().getOriginalUrl(),
                    WebComponentUtil.debugHandler(cycle.getRequestHandlerScheduledAfterCurrent()));

        }
        super.onBeginRequest(cycle);
    }

    @Override
    public void onExceptionRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler,
                                                  Exception exception) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: Exception - Resolved request handler {}",
                    WebComponentUtil.debugHandler(handler), exception);
        }
    }

    @Override
    public void onRequestHandlerExecuted(RequestCycle cycle, IRequestHandler handler) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: Request handler executed {}",
                    WebComponentUtil.debugHandler(handler));
        }
    }

    @Override
    public void onUrlMapped(RequestCycle cycle, IRequestHandler handler, Url url) {
        if (REQUEST_LOGGER.isTraceEnabled()) {
            REQUEST_LOGGER.trace("REQUEST CYCLE: Url '{}' mapped, handler {}", url,
                    WebComponentUtil.debugHandler(handler));
        }
    }
}

