/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * Stable, client-facing error text (no internal exception details).
 */
public final class McpPublicErrorMessages {

    public static final String NOT_FOUND = "The requested object was not found.";

    public static final String ACCESS_DENIED = "Access denied.";

    public static final String INTERNAL_ERROR = "An error occurred while processing the request.";

    public static final String UNEXPECTED_TOOL_FAILURE = "An unexpected error occurred while handling the tool request.";

    public static final String SERIALIZATION_FAILED = "Response serialization failed.";

    private McpPublicErrorMessages() {}
}
