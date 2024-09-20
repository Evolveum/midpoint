/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import java.io.File;

import com.google.common.base.Strings;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.webresources.ExtractingRoot;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

/**
 * Custom tomcat factory that used to hack embedded Tomcat setup.
 * There seem to be no cleaner way to get to actual configured Tomcat instance.
 *
 * @author semancik
 */
public class MidPointTomcatServletWebServerFactory extends TomcatServletWebServerFactory {

    private File baseDirectory;

    private String protocol = DEFAULT_PROTOCOL;

    private int backgroundProcessorDelay;

    private final String contextPath;

    private final SystemObjectCache systemObjectCache;

    private String jvmRoute;

    /**
     * Logger, uses TomcatServletWebServerFactory for backwards compatibility
     */
    private static final Log LOG = LogFactory.getLog(TomcatServletWebServerFactory.class);

    public MidPointTomcatServletWebServerFactory(String contextPath, SystemObjectCache systemObjectCache) {
        this.contextPath = contextPath;
        this.systemObjectCache = systemObjectCache;
    }

    @Override
    protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {

        // We are setting up fake context here. This context does not really do anything.
        // But it is "mapped" to the root URL (/ ... or rather "" in Tomcat parlance).
        // This fake context is necessary. If there is no context at all then
        // CoyoteAdapter will not execute any Valves and returns 404 immediately.
        // So without this the TomcatRootValve will not work.

        RootRootContext rootRootContext = new RootRootContext();
        try {
            tomcat.getHost().addChild(rootRootContext);
        } catch (Exception e) {
            String error = e.getMessage();
            if (error != null && error.contains("Child name [] is not unique")) {
                // Safely ignored, this covers Boot config: server.servlet.context-path=/
                LOG.debug("Ignoring duplicate root, probably root context is explicitly configured");
            } else {
                throw e;
            }
        }

        return super.getTomcatWebServer(tomcat);
    }

    @Override
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        super.setBaseDirectory(baseDirectory);
    }

    public void setProtocol(String protocol) {
        super.setProtocol(protocol);
        this.protocol = protocol;
    }

    @Override
    public void setBackgroundProcessorDelay(int delay) {
        super.setBackgroundProcessorDelay(delay);
        this.backgroundProcessorDelay = delay;
    }

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        Tomcat tomcat = new Tomcat();
        File baseDir = (this.baseDirectory != null) ? this.baseDirectory : createTempDir("tomcat");
        tomcat.setBaseDir(baseDir.getAbsolutePath());
        Connector connector = new Connector(this.protocol) {
            @Override
            public Response createResponse() {
                if (protocolHandler instanceof AbstractAjpProtocol<?>) {
                    int packetSize = ((AbstractAjpProtocol<?>) protocolHandler).getPacketSize();
                    return new MidpointResponse(packetSize - org.apache.coyote.ajp.Constants.SEND_HEAD_LEN,
                            contextPath, systemObjectCache);
                } else {
                    return new MidpointResponse(contextPath, systemObjectCache);
                }
            }
        };
        tomcat.getService().addConnector(connector);
        customizeConnector(connector);
        tomcat.setConnector(connector);
        Host host = tomcat.getHost();
        host.setAutoDeploy(false);
        // Removing info/stack from low-level Tomcat error page: https://stackoverflow.com/a/70196883/658826
        if (host instanceof StandardHost) {
            ErrorReportValve errorReportValve = new ErrorReportValve();
            errorReportValve.setShowReport(false);
            errorReportValve.setShowServerInfo(false);
            ((StandardHost) host).addValve(errorReportValve);
        }
        Engine engine = tomcat.getEngine();
        if (!Strings.isNullOrEmpty(jvmRoute)) {
            engine.setJvmRoute(jvmRoute);
        }
        configureEngine(engine);
        for (Connector additionalConnector : getAdditionalTomcatConnectors()) {
            tomcat.getService().addConnector(additionalConnector);
        }
        prepareContext(tomcat.getHost(), initializers);
        return getTomcatWebServer(tomcat);
    }

    private void configureEngine(Engine engine) {
        engine.setBackgroundProcessorDelay(this.backgroundProcessorDelay);
        for (Valve valve : getEngineValves()) {
            engine.getPipeline().addValve(valve);
        }
    }

    @Override
    protected void postProcessContext(Context context) {
        context.setResources(new ExtractingRoot());
    }

    public void setJvmRoute(String jvmRoute) {
        this.jvmRoute = jvmRoute;
    }
}
