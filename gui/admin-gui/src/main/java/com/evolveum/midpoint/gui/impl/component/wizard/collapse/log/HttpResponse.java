/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serializable;

/**
 * Representation of HTTP response for GUI purposes.
 * @param statusCode status code of response.
 * @param body Response body.
 */
public record HttpResponse(int statusCode, String body) implements Serializable {
}
