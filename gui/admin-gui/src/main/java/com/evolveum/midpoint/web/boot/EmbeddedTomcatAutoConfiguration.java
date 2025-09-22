/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.servlet.Servlet;

import com.google.common.base.Strings;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.apache.coyote.ajp.AjpNioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

import org.springframework.util.unit.DataSize;

/**
 * Custom configuration (factory) for embedded tomcat factory.
 * This is necessary, as the tomcat factory is hacking tomcat setup.
 *
 * @author semancik
 * @see MidPointTomcatServletWebServerFactory
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
@Import(BeanPostProcessorsRegistrar.class)
public class EmbeddedTomcatAutoConfiguration {

    @Profile("!test")
    @Configuration
    @ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
    @ConditionalOnMissingBean(value = TomcatServletWebServerFactory.class, search = SearchStrategy.CURRENT)
    public static class EmbeddedTomcat {

        private static final Trace LOGGER = TraceManager.getTrace(EmbeddedTomcat.class);

        @Value("${server.tomcat.ajp.enabled:false}")
        private boolean enableAjp;

        @Value("${server.tomcat.ajp.port:9090}")
        private int port;

        @Value("${server.tomcat.ajp.address:}")
        private String address;

        @Value("${server.tomcat.ajp.jvmRoute:}")
        private String jvmRoute;

        @Value("${server.tomcat.ajp.secret:}")
        private String secret;

        @Value("${server.tomcat.ajp.max-part-header-size:}")
        private DataSize ajpMaxPartHeaderSize;

        @Value("${server.tomcat.ajp.max-part-count:}")
        private Integer ajpMaxPartCount;

        @Value("${server.tomcat.max-part-header-size:}")
        private DataSize tomcatMaxPartHeaderSize;

        @Value("${server.tomcat.max-part-count:}")
        private Integer tomcatMaxPartCount;

        @Value("${server.servlet.context-path}")
        private String contextPath;

        @Autowired
        private SystemObjectCache systemObjectCache;

        @Bean
        public TomcatServletWebServerFactory tomcatEmbeddedServletContainerFactory() throws UnknownHostException {
            MidPointTomcatServletWebServerFactory tomcat = new MidPointTomcatServletWebServerFactory(contextPath, systemObjectCache);

            if (enableAjp) {
                Connector ajpConnector = new Connector("AJP/1.3");
                AjpNioProtocol protocol = (AjpNioProtocol) ajpConnector.getProtocolHandler();
                protocol.setSecret(secret);
                if (!Strings.isNullOrEmpty(address)) {
                    protocol.setAddress(InetAddress.getByName(address));
                }
                ajpConnector.setPort(port);
                ajpConnector.setSecure(false);
                ajpConnector.setScheme("http");
                ajpConnector.setAllowTrace(false);

                if (getMaxPartHeaderSize() != null) {
                    ajpConnector.setMaxPartHeaderSize((int) getMaxPartHeaderSize().toBytes());
                }
                if (getMaxPartCount() != null) {
                    ajpConnector.setMaxPartCount(getMaxPartCount());
                }

                tomcat.addAdditionalTomcatConnectors(ajpConnector);
                tomcat.setJvmRoute(jvmRoute); // will be set on Engine there
            }

            return tomcat;
        }

        private DataSize getMaxPartHeaderSize() {
            return ajpMaxPartHeaderSize != null ? ajpMaxPartHeaderSize : tomcatMaxPartHeaderSize;
        }

        private Integer getMaxPartCount() {
            return ajpMaxPartCount != null ? ajpMaxPartCount : tomcatMaxPartCount;
        }

    }
}
