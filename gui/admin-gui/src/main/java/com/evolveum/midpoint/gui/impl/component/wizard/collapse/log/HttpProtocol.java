/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;


/**
 * he HTTP request/response pair executed for one log entry
 * @param request Http request in pair.
 * @param response Http response in pair.
 */
public record HttpProtocol(HttpRequest request, HttpResponse response) implements OperationLogProtocol {
}
