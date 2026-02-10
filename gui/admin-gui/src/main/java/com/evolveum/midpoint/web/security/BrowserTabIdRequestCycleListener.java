/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import org.apache.wicket.Session;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;


public class BrowserTabIdRequestCycleListener implements IRequestCycleListener {

    @Override
    public void onBeginRequest(RequestCycle cycle) {
        var params = cycle.getRequest().getRequestParameters();
        String tabId = params.getParameterValue("tabId") != null ?
                params.getParameterValue("tabId").toOptionalString() : null;
        if (tabId != null && !tabId.isEmpty()) { //todo actually each ajax call should know its tabId
            Session.get().setAttribute("tabId", tabId);
        }
    }
}

