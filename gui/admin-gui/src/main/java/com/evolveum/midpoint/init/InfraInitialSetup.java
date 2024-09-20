/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
