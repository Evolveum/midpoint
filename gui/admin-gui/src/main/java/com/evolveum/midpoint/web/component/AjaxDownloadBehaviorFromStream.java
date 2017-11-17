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

public abstract class AjaxDownloadBehaviorFromStream extends AbstractAjaxDownloadBehavior {

	private static final long serialVersionUID = 1L;
	private boolean addAntiCache;
	private String contentType = "text";
	private String fileName = null;

	
	public AjaxDownloadBehaviorFromStream() {
		super();
	}
	
	public AjaxDownloadBehaviorFromStream(boolean addAntiCache) {
		super(addAntiCache);
		
	}


	@Override
	public IResourceStream getResourceStream() {
		final InputStream byteStream = initStream();

		if (byteStream == null) {
			return null;
		}
		
		IResourceStream resourceStream = new AbstractResourceStream(){

			private static final long serialVersionUID = 1L;
			@Override
			public String getContentType() {
				return contentType;
			}

			@Override
			public InputStream getInputStream() throws ResourceStreamNotFoundException {
			    return byteStream;
            }

			@Override
			public void close() throws IOException {
				byteStream.close();
			}
			
		};
		return resourceStream;
	}
	
    protected abstract InputStream initStream();

    public String getFileName() {
        return fileName;
    }

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
