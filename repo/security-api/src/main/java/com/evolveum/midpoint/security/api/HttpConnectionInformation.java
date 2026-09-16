/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.api;

import java.io.Serializable;

/**
 * Information about the HTTP connection. These can be obtained any time from the servlet Request object.
 *
 * @author semancik
 */
public class HttpConnectionInformation implements Serializable {

    private String remoteHostAddress;

    private String localHostName;

    /**
     * The actual (sensitive) HTTP session identifier assigned by the servlet container.
     * This value MUST be treated as confidential and must not be exposed in logs,
     * error reports, or any external output.
     *
     * If you need a session reference safe for audit or display, use {@link #getPublicSessionId()}
     * instead. If the real session id must be recorded, protect it (for example: redact or hash)
     * and store it only in secure audit storage.
     */
    private String sessionId;
    /**
     * Public session identifier intended for external use (for example: audit records,
     * UI display, or logs). This identifier is deliberately non-sensitive and safe to
     * expose outside the application. Do not assume it can be used to access a session;
     * use {@link #getSessionId()} for the actual (sensitive) session identifier.
     */
    private String publicSessionId;

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

    /**
     * @see #sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @see #sessionId
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @see #publicSessionId
     */
    public String getPublicSessionId() {
        return publicSessionId;
    }

    /**
     * @see #publicSessionId
     */
    public void setPublicSessionId(String publicSessionId) {
        this.publicSessionId = publicSessionId;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }
}
