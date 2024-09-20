/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;

import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

public abstract class AjaxDownloadBehaviorFromStream extends AbstractAjaxDownloadBehavior {

    @Serial
    private static final long serialVersionUID = 1L;

    private String contentType;

    private String fileName;

    public AjaxDownloadBehaviorFromStream() {
        super();
    }

    @Override
    public IResourceStream getResourceStream() {
        final InputStream is = getInputStream();

        if (is == null) {
            return null;
        }

        return new AbstractResourceStream() {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public InputStream getInputStream() {
                return is;
            }

            @Override
            public void close() throws IOException {
                is.close();
            }
        };
    }

    protected abstract InputStream getInputStream();

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
