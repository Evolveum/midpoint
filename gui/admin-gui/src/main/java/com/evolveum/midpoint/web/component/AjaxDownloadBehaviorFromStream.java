/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

import java.io.IOException;
import java.io.InputStream;

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

        return new AbstractResourceStream() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public InputStream getInputStream() {
                return byteStream;
            }

            @Override
            public void close() throws IOException {
                byteStream.close();
            }
        };
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
