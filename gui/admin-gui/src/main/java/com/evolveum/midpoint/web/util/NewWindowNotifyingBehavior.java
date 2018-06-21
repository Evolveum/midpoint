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
