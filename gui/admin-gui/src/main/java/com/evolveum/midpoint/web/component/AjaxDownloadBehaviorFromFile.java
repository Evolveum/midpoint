/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.file.Files;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.time.Duration;

public abstract class AjaxDownloadBehaviorFromFile extends AbstractAjaxBehavior {

    private static final Trace LOGGER = TraceManager.getTrace(AjaxDownloadBehaviorFromFile.class);

	private boolean addAntiCache;
	private String contentType = "text";
    private boolean removeFile = true;

	public AjaxDownloadBehaviorFromFile() {
		this(true);
	}

	public AjaxDownloadBehaviorFromFile(boolean addAntiCache) {
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
		final File file = initFile();
		IResourceStream resourceStream = new FileResourceStream(new File(file));
		getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
				new ResourceStreamRequestHandler(resourceStream) {

                    @Override
					public void respond(IRequestCycle requestCycle) {
                        try {
						    super.respond(requestCycle);
                        } finally {
                            if (removeFile) {
                                LOGGER.debug("Removing file '{}'.", new Object[]{file.getAbsolutePath()});
                                Files.remove(file);
                            }
                        }
					}
				}.setFileName(file.getName()).setContentDisposition(ContentDisposition.ATTACHMENT)
						.setCacheDuration(Duration.ONE_SECOND));
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

    public void setRemoveFile(boolean removeFile) {
        this.removeFile = removeFile;
    }

    protected abstract File initFile();
}
