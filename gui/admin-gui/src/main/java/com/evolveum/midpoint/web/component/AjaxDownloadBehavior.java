package com.evolveum.midpoint.web.component;

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

public abstract class AjaxDownloadBehavior extends AbstractAjaxBehavior {

	private boolean addAntiCache;

	public AjaxDownloadBehavior() {
		this(true);
	}

	public AjaxDownloadBehavior(boolean addAntiCache) {
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
						Files.remove(file);
					}
				}.setFileName(file.getName())
				.setContentDisposition(ContentDisposition.ATTACHMENT)
				.setCacheDuration(Duration.ONE_SECOND));
	}

	protected abstract File initFile();
}
