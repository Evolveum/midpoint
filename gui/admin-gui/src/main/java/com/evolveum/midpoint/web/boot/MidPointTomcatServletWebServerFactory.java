/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Custom tomcat factory that used to hack embedded Tomcat setup.
 * There seem to be no cleaner way to get to actual configured Tomcat instance.
 *
 * @author semancik
 */
public class MidPointTomcatServletWebServerFactory extends TomcatServletWebServerFactory {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointTomcatServletWebServerFactory.class);

    @Override

    protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {

        // We are setting up fake context here. This context does not really do anything.
        // But it is "mapped" to the root URL (/ ... or rather "" in Tomcat parlance).
        // This fake context is necessary. If there is no context at all then
        // CoyoteAdapter will not execute any Valves and returns 404 immediately.
        // So without this the TomcatRootValve will not work.

        RootRootContext rootRootContext = new RootRootContext();
        tomcat.getHost().addChild(rootRootContext);

        return super.getTomcatWebServer(tomcat);
    }



}
