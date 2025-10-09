/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 */
public class InfraInitialSetup {

    private static final Trace LOGGER = TraceManager.getTrace(InfraInitialSetup.class);

    public void init() {
        SchemaDebugUtil.initializePrettyPrinter();
        // Make sure that JUL is loaded in the main classloader
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(InfraInitialSetup.class.getName());
    }
}
