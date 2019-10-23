/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.ajax.AjaxNewWindowNotifyingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Created by lazyman on 13/03/2017.
 */
public class NewWindowNotifyingBehavior extends AjaxNewWindowNotifyingBehavior {

    private static final Trace LOG = TraceManager.getTrace(NewWindowNotifyingBehavior.class);

    @Override
    protected void onNewWindow(AjaxRequestTarget target) {
        LOG.debug("Page version already used in different tab, refreshing page");
        WebPage page = (WebPage) getComponent();
        //fix for MID-4649; windowName parameter causes recursive reloading of the page
        PageParameters pageParameters = page.getPageParameters();
        if (pageParameters != null && pageParameters.getPosition("windowName") > -1 ){
            pageParameters = pageParameters.remove("windowName");
        }
        page.setResponsePage(page.getPageClass(), pageParameters);
    }
}
