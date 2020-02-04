/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import javax.servlet.Servlet;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Custom configuration (factory) for embedded tomcat factory.
 * This is necessary, as the tomcat factory is hacking tomcat setup.
 * @see MidPointTomcatServletWebServerFactory
 *
 * @author semancik
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
@Import(BeanPostProcessorsRegistrar.class)
public class EmbeddedTomcatAutoConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(EmbeddedTomcatAutoConfiguration.class);

    @Profile("!test")
    @Configuration
    @ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
    @ConditionalOnMissingBean(value = TomcatServletWebServerFactory.class, search = SearchStrategy.CURRENT)
    public static class EmbeddedTomcat {

        @Value( "${server.tomcat.ajp.enabled:false}" )
        private boolean enableAjp;

        @Value( "${server.tomcat.ajp.port:9090}" )
        private int port;

        @Bean
        public TomcatServletWebServerFactory tomcatEmbeddedServletContainerFactory() {
            MidPointTomcatServletWebServerFactory tomcat = new MidPointTomcatServletWebServerFactory();

            if(enableAjp) {
                Connector ajpConnector = new Connector("AJP/1.3");
                ajpConnector.setPort(port);
                ajpConnector.setSecure(false);
                ajpConnector.setScheme("http");
                ajpConnector.setAllowTrace(false);
                tomcat.addAdditionalTomcatConnectors(ajpConnector);
            }

            return tomcat;
        }

    }
}
