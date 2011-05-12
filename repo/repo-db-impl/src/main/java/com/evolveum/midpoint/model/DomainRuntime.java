/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;

/**
 * Represents {@link Domain} runtime properties.
 * 
 * @author $author$
 */
public class DomainRuntime implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6165382826498480331L;

    /**
     * Build server base URL.
     *
     * @param domain
     *            the domain
     * @param requestUrl
     *            the HTTP request URL
     * @param requestContextPath
     *            the HTTP request context path
     * @return the base URL of the server
     */
    public static URL buildServerBaseUrl(final Domain domain,
            final URL requestUrl, final String requestContextPath) {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(domain.getName())) {
            sb.append(domain.getName()).append('.');
        }
        sb.append(domain.getName());

        try {
            return new URL(requestUrl.getProtocol(), sb.toString(), requestUrl.getPort(), requestContextPath + "/");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
    /**
     * Server base URL.
     */
    private URL serverBaseUrl;
    /**
     * midPoint Server URL.
     */
    private URL openidServerUrl;

    /**
     * @return the serverBaseUrl
     */
    public URL getServerBaseUrl() {
        return serverBaseUrl;
    }

    /**
     * @param serverBaseUrl
     *            the serverBaseUrl to set
     */
    public void setServerBaseUrl(final URL serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    /**
     * @return the openIDMServerUrl
     */
    public URL getmidPointServerUrl() {
        return openidServerUrl;
    }

    /**
     * @param openIDMServerUrl
     *            the openIDMServerUrl to set
     */
    public void setmidPointServerUrl(final URL openidServerUrl) {
        this.openidServerUrl = openidServerUrl;
    }
}
