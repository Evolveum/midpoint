/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxNewWindowNotifyingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;
import org.danekja.java.util.function.serializable.SerializableConsumer;

import java.util.UUID;

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
