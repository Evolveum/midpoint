/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.IResourceStream;

import java.time.Duration;

public abstract class AbstractAjaxDownloadBehavior extends AbstractAjaxBehavior {

    private static final long serialVersionUID = 1L;
    private boolean addAntiCache;
    private String contentType = "text";
    private String fileName = null;

    public AbstractAjaxDownloadBehavior() {
        this(true);
    }

    public AbstractAjaxDownloadBehavior(boolean addAntiCache) {
        super();
        this.addAntiCache = addAntiCache;
    }

    /**
     * Call this method to initiate the download.
     */
    public void initiate(AjaxRequestTarget target) {
        String url = getCallbackUrl().toString();

        if (addAntiCache) {
            url = url + (url.contains("?") ? "&" : "?");
            url = url + "antiCache=" + System.currentTimeMillis();
        }

        // the timeout is needed to let Wicket release the channel
        target.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
    }

    public void onRequest() {

        IResourceStream resourceStream = getResourceStream();
        if (resourceStream == null) {
            return;        // We hope the error was already processed and will be shown.
        }

        ResourceStreamRequestHandler reqHandler = new ResourceStreamRequestHandler(resourceStream) {
            @Override
            public void respond(IRequestCycle requestCycle) {
                super.respond(requestCycle);
            }
        }.setContentDisposition(ContentDisposition.ATTACHMENT)
                .setCacheDuration(Duration.ofSeconds(1));
        if (StringUtils.isNotEmpty(getFileName())){
            reqHandler.setFileName(getFileName());
        }
        getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(reqHandler);
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public abstract IResourceStream getResourceStream();
}
