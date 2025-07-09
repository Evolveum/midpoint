/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.test;

import java.lang.management.ManagementFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.impl.factory.panel.TextAreaPanelFactory;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.boot.AbstractSpringBootApplication;
import com.evolveum.midpoint.web.page.admin.certification.handlers.CertGuiHandlerRegistry;

/**
 * @author katka
 */

//"classpath:ctx-init.xml",
@ImportResource(locations = {
        "classpath:ctx-common.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath*:ctx-repository-test.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-repo-common.xml",
        "classpath*:ctx-task.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-ucf-connid.xml",
        "classpath:ctx-ucf-builtin.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-security-enforcer.xml",
        "classpath:ctx-model.xml",
        "classpath:ctx-model-test.xml",
        "classpath:ctx-model-common.xml",
        "classpath:ctx-init.xml",
        "classpath:ctx-authentication.xml",
        "classpath:ctx-report.xml",
        "classpath:ctx-smart-integration.xml",
        "classpath*:ctx-cases.xml",
        "classpath*:ctx-workflow.xml",
        "classpath*:ctx-notifications.xml",
        "classpath:ctx-certification.xml",
        "classpath:ctx-interceptor.xml",
        "classpath*:ctx-overlay.xml",
        "classpath:ctx-webapp.xml"
})
@Profile({ "test", "!default" })
@SpringBootConfiguration
@ComponentScan(
        basePackages = {
                "com.evolveum.midpoint.gui",
                "com.evolveum.midpoint.gui.api"
        },
        basePackageClasses = {
                TextAreaPanelFactory.class,
                GuiComponentRegistryImpl.class,
                CertGuiHandlerRegistry.class
        })
public class TestMidPointSpringApplication extends AbstractSpringBootApplication {

    private static final Trace LOGGER = TraceManager.getTrace(TestMidPointSpringApplication.class);

    public static int DEFAULT_PORT = 18080;

    private static ConfigurableApplicationContext applicationContext = null;

    public static void main(String[] args) {
        System.out.println("ClassPath: " + System.getProperty("java.class.path"));

        System.setProperty("xml.catalog.className", "com.evolveum.midpoint.prism.impl.schema.CatalogImpl");
        String mode = args != null && args.length > 0 ? args[0] : null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("PID:" + ManagementFactory.getRuntimeMXBean().getName() +
                    " Application mode:" + mode + " context:" + applicationContext);
        }

        if (applicationContext != null && "stop".equals(mode)) {
            System.exit(SpringApplication.exit(applicationContext, new ExitCodeGenerator() {

                @Override
                public int getExitCode() {

                    return 0;
                }
            }));

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

    //what is this for? why static?
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

        // cglib used by wicket does not support Java 15+ so we need use byte buddy generation for wicket.
        // We can remove this after cglib(wicket) fix issue with java 15+ or when wicket will use byte buddy as default.
        System.setProperty("wicket.ioc.useByteBuddy", "true");

        return application.sources(TestMidPointSpringApplication.class);
    }

    @Bean
    public TomcatServletWebServerFactory tomcatEmbeddedServletContainerFactory() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.setPort(DEFAULT_PORT);
        return tomcat;
    }

}
