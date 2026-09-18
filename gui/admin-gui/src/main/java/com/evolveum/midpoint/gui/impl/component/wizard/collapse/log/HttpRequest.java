/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serializable;

/**
 * Representation of HTTP request for gui purposes.
 * @param method Used HTTP method for request.
 * @param url Url called by request.
 * @param body Request body.
 */
public record HttpRequest(HttpMethod method, String url, String body) implements Serializable {
}
