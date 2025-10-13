/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.schema.util.DiagnosticContext;

/**
 * @author semancik
 *
 */
public interface DiagnosticContextManager {

    DiagnosticContext createNewContext();

    void processFinishedContext(DiagnosticContext ctx);

}
