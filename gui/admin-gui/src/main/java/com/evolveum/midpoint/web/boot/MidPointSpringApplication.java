/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.impl.factory.TextAreaPanelFactory;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.JettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.UndertowWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 * Modified by Lukas Skublik.
 */
@ImportResource(locations = {
        "classpath:ctx-common.xml",
        "classpath:ctx-configuration.xml",
        "classpath*:ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-repo-common.xml",
        "classpath:ctx-task.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-ucf-connid.xml",
        "classpath:ctx-ucf-builtin.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-security-enforcer.xml",
        "classpath:ctx-model.xml",
        "classpath:ctx-model-common.xml",
        "classpath:ctx-report.xml",
        "classpath*:ctx-workflow.xml",
        "classpath*:ctx-notifications.xml",
        "classpath:ctx-certification.xml",
        "classpath:ctx-interceptor.xml",
        "classpath*:ctx-overlay.xml",
        "classpath:ctx-init.xml",
        "classpath:ctx-webapp.xml"
})
@Profile("!test")
@SpringBootConfiguration
@ComponentScan(basePackages = {"com.evolveum.midpoint.web.security.factory", "com.evolveum.midpoint.gui", "com.evolveum.midpoint.gui.api"}, basePackageClasses = {TextAreaPanelFactory.class, GuiComponentRegistryImpl.class})
@EnableScheduling
public class MidPointSpringApplication extends AbstractSpringBootApplication {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointSpringApplication.class);

    private static ConfigurableApplicationContext applicationContext = null;
    private Context tomcatContext;

    public static void main(String[] args) {
        System.out.println("ClassPath: " + System.getProperty("java.class.path"));

        System.setProperty("xml.catalog.className", "com.evolveum.midpoint.prism.impl.schema.CatalogImpl");
        String mode = args != null && args.length > 0 ? args[0] : null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("PID:" + ManagementFactory.getRuntimeMXBean().getName() +
                    " Application mode:" + mode + " context:" + applicationContext);
        }

        if (applicationContext != null && "stop".equals(mode)) {
            System.exit(SpringApplication.exit(applicationContext, () -> 0));

        } else {

            applicationContext = configureApplication(new SpringApplicationBuilder()).run(args);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("PID:" + ManagementFactory.getRuntimeMXBean().getName() +
                        " Application started context:" + applicationContext);
            }

        }

    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return configureApplication(application);
    }

    private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder application) {
        String mpHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
        if (StringUtils.isEmpty(mpHome)) {
            LOGGER.info("{} system property is not set, using default configuration",
                    MidpointConfiguration.MIDPOINT_HOME_PROPERTY);

            mpHome = System.getProperty(MidpointConfiguration.USER_HOME_PROPERTY);
            if (!mpHome.endsWith("/")) {
                mpHome += "/";
            }
            mpHome += "midpoint";
            System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, mpHome);
        }

        System.setProperty("spring.config.additional-location", "${midpoint.home}/");

        application.bannerMode(Banner.Mode.LOG);

        return application.sources(MidPointSpringApplication.class);
    }

    private void setTomcatContext(Context context) {
        this.tomcatContext = context;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    @Scheduled(fixedDelayString = "${server.tomcat.session-manager-delay:10000}", initialDelayString = "${server.tomcat.session-manager-delay:10000}")
    public void invalidExpiredSessions() {
        Context context = this.tomcatContext;
        if (context != null) {
            Manager manager = context.getManager();
            if (manager != null) {
                try {
                    manager.backgroundProcess();
                } catch (Exception e) {
                    LOGGER.error("Couldn't execute backgroundProcess on session manager.", e);
                }
            }
        }
    }

    @Component
    @EnableConfigurationProperties(ServerProperties.class)
    public class ServerCustomization implements WebServerFactoryCustomizer<MidPointTomcatServletWebServerFactory> {

        @Value("${server.servlet.session.timeout}")
        private int sessionTimeout;
        @Value("${server.servlet.context-path}")
        private String servletPath;

        @Autowired
        private ServerProperties serverProperties;

        @Autowired
        private TaskManager taskManager;

        @Autowired
        private Environment env;


        @Override
        public void customize(MidPointTomcatServletWebServerFactory serverFactory) {

            ServletWebServerFactoryCustomizer webServletWebServerFactoryCustomizer = new ServletWebServerFactoryCustomizer(serverProperties);
            webServletWebServerFactoryCustomizer.customize(serverFactory);
            TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer = new TomcatWebServerFactoryCustomizer(env, serverProperties);
            tomcatWebServerFactoryCustomizer.customize(serverFactory);

            serverFactory.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED, "/error/401"));
            serverFactory.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN, "/error/403"));
            serverFactory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"));
            serverFactory.addErrorPages(new ErrorPage(HttpStatus.GONE, "/error/410"));
            serverFactory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error"));

            Session session = new Session();
            session.setTimeout(Duration.ofMinutes(sessionTimeout));
            serverFactory.setSession(session);

            if (serverFactory instanceof TomcatServletWebServerFactory) {
                TomcatContextCustomizer contextCustomizer = new TomcatContextCustomizer() {
                    @Override
                    public void customize(Context context) {
                        setTomcatContext(context);
                    }
                };
                List<TomcatContextCustomizer> contextCustomizers = new ArrayList<>();
                contextCustomizers.add(contextCustomizer);
                serverFactory.setTomcatContextCustomizers(contextCustomizers);

                // Tomcat valve used to redirect root URL (/) to real application URL (/midpoint/).
                // See comments in TomcatRootValve
                Valve rootValve = new TomcatRootValve(servletPath);
                serverFactory.addEngineValves(rootValve);

                Valve nodeIdHeaderValve = new NodeIdHeaderValve(taskManager);
                serverFactory.addEngineValves(nodeIdHeaderValve);
            }
        }
    }
}
