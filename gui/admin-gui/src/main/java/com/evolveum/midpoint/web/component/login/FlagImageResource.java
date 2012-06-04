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
        //todo caching...
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IResourceStream getCacheableResourceStream() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static class CacheKey {

//        private static final URL url;
    }
}
