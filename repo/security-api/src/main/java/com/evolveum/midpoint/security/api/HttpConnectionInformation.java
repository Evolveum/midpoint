/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import java.io.Serializable;

/**
 * Information about the HTTP connection. These can be obtained any time from the servlet Request object.
 *
 * @author semancik
 * @author mederly
 */
public class HttpConnectionInformation implements Serializable {

    private String remoteHostAddress;
    private String localHostName;
    private String sessionId;
    private String serverName;

    public String getRemoteHostAddress() {
        return remoteHostAddress;
    }

    public void setRemoteHostAddress(String remoteHostAddress) {
        this.remoteHostAddress = remoteHostAddress;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }
}
