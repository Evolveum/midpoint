/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.model.common.SystemObjectCache;

import org.apache.catalina.Engine;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Response;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import java.io.File;

/**
 * Custom tomcat factory that used to hack embedded Tomcat setup.
 * There seem to be no cleaner way to get to actual configured Tomcat instance.
 *
 * @author semancik
 */
public class MidPointTomcatServletWebServerFactory extends TomcatServletWebServerFactory {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointTomcatServletWebServerFactory.class);

    private File baseDirectory;

    private String protocol = DEFAULT_PROTOCOL;

    private int backgroundProcessorDelay;

    private String servletPath;

    private SystemObjectCache systemObjectCache;

    public MidPointTomcatServletWebServerFactory(String servletPath, SystemObjectCache systemObjectCache){
        this.servletPath = servletPath;
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
        tomcat.getHost().addChild(rootRootContext);

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
        Connector connector = new Connector(this.protocol){
            @Override
            public Response createResponse() {
                if (protocolHandler instanceof AbstractAjpProtocol<?>) {
                    int packetSize = ((AbstractAjpProtocol<?>) protocolHandler).getPacketSize();
                    return new MidpointResponse(packetSize - org.apache.coyote.ajp.Constants.SEND_HEAD_LEN,
                            servletPath, systemObjectCache);
                } else {
                    return new MidpointResponse(servletPath, systemObjectCache);
                }
            }
        };
        tomcat.getService().addConnector(connector);
        customizeConnector(connector);
        tomcat.setConnector(connector);
        tomcat.getHost().setAutoDeploy(false);
        configureEngine(tomcat.getEngine());
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

}
