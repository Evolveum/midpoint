/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
