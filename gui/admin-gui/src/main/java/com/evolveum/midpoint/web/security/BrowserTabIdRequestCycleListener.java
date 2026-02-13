/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import java.io.Serial;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class BrowserTabIdRequestCycleListener implements IRequestCycleListener {

    private static final Trace LOGGER = TraceManager.getTrace(BrowserTabIdRequestCycleListener.class);

    public static final String PARAM_TAB_ID = "Tab-Id";

    public static final MetaDataKey<String> BROWSER_TAB_ID_KEY = new MetaDataKey<>() {

        @Serial private static final long serialVersionUID = 1L;
    };

    @Override
    public void onBeginRequest(RequestCycle cycle) {
//        var params = cycle.getRequest().getRequestParameters();
//        String tabId = params.getParameterValue(PARAM_TAB_ID).toOptionalString();
//
//        if (StringUtils.isEmpty(tabId)) {
//            tabId = UUID.randomUUID().toString();
//
//            LOGGER.info("New tab ID: {}", tabId);
//        } else {
//            LOGGER.info("Existing tab ID: {}", tabId);
//        }
//
//        cycle.setMetaData(BROWSER_TAB_ID_KEY, tabId);
//
//        WebResponse response = (WebResponse) cycle.getResponse();
//        response.setHeader(PARAM_TAB_ID, tabId);
    }
}

