/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Duration;

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
        ResourceStreamRequestHandler reqHandler = new ResourceStreamRequestHandler(resourceStream) {
            @Override
            public void respond(IRequestCycle requestCycle) {
                super.respond(requestCycle);
            }
        }.setContentDisposition(ContentDisposition.ATTACHMENT)
                .setCacheDuration(Duration.ONE_SECOND);
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
