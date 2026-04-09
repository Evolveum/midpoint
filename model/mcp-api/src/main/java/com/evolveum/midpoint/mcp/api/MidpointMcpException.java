/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

public class MidpointMcpException extends RuntimeException {

    private final String code;
    private final int status;
    /** Optional short guidance for MCP clients (examples, next steps). */
    private final String hint;

    public MidpointMcpException(String code, int status, String message) {
        this(code, status, message, null, null);
    }

    public MidpointMcpException(String code, int status, String message, Throwable cause) {
        this(code, status, message, null, cause);
    }

    public MidpointMcpException(String code, int status, String message, String hint) {
        this(code, status, message, hint, null);
    }

    public MidpointMcpException(String code, int status, String message, String hint, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.hint = hint;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public String getHint() {
        return hint;
    }
}
