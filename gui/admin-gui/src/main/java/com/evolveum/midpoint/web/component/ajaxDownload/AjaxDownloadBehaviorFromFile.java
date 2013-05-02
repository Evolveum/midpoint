/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.ajaxDownload;

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
						super.respond(requestCycle);
                        if (removeFile) {
						    Files.remove(file);
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
