/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.boot;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;

/**
 * Fake root context. This context does not really do anything.
 * But it is "mapped" to the root URL (/ ... or rather "" in Tomcat parlance).
 * This fake context is necessary. If there is no context at all then
 * CoyoteAdapter will not execute any Valves and returns 404 immediately.
 * So without this the TomcatRootValve will not work.
 *
 * @author semancik
 */
public class RootRootContext extends StandardContext {

    public RootRootContext() {
        super();
        setPath(""); // this means "/"
        setDisplayName("RootRoot");
    }

    // HAck
    @Override
    public void resourcesStart() throws LifecycleException {
        super.resourcesStart();
        setConfigured(true);
    }

}
