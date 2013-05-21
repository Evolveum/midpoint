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

package com.evolveum.midpoint.web.component.login;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.caching.IStaticCacheableResource;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.resource.IResourceStream;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

/**
 * @author lazyman
 */
public class FlagImageResource extends DynamicImageResource implements IStaticCacheableResource {

    private static final Trace LOGGER = TraceManager.getTrace(FlagImageResource.class);
    private URL url;

    public FlagImageResource(URL url) {
        Validate.notNull(url, "URL must not be null.");

        this.url = url;
    }

    @Override
    protected byte[] getImageData(Attributes attributes) {
        byte[] bytes = new byte[0];
        InputStream stream = null;
        try {
            bytes = IOUtils.toByteArray(url.openStream());
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get image from '{}'", ex, url);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return bytes;
    }

    @Override
    public Serializable getCacheKey() {
        //todo implement
        return null;
    }

    @Override
    public IResourceStream getCacheableResourceStream() {
        //todo implement
        return null;
    }

    @Override
    public boolean isCachingEnabled() {
        return false;
    }
}
