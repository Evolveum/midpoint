/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import org.apache.velocity.app.event.IncludeEventHandler;
import org.apache.velocity.context.Context;

/** Forbids `#include` and `#parse` directives as they can access unexpected files or resources in the system. */
public class RejectIncludes implements IncludeEventHandler {

    @Override
    public String includeEvent(Context context, String includeResourcePath, String currentResourcePath, String directiveName) {
        throw new SecurityException("Velocity include directive is not allowed in safe mode: " + includeResourcePath);
    }
}
