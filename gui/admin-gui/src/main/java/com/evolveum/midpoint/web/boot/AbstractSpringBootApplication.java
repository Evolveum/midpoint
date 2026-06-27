/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.boot;

import jakarta.servlet.DispatcherType;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WicketFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.web.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.ThreadDumpEndpointAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsEndpointAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.metrics.TomcatMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.HttpEncodingAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration;
import org.springframework.boot.web.error.ErrorPageRegistrar;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.evolveum.midpoint.init.StartupConfiguration;
import com.evolveum.midpoint.web.util.MidPointProfilingServletFilter;

/**
 * @author katka
 */
@ImportAutoConfiguration(classes = {
        EmbeddedTomcatAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        MultipartAutoConfiguration.class,
        HttpEncodingAutoConfiguration.class,
        EndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        WebMvcEndpointManagementContextConfiguration.class,
        ServletManagementContextAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class,
        HealthContributorRegistryAutoConfiguration.class,
        HealthContributorAutoConfiguration.class,
        ThreadDumpEndpointAutoConfiguration.class,
        HeapDumpWebEndpointAutoConfiguration.class,
        EnvironmentEndpointAutoConfiguration.class,
        InfoEndpointAutoConfiguration.class,
        MetricsAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class,
        CompositeMeterRegistryAutoConfiguration.class,
        TomcatMetricsAutoConfiguration.class,
        JvmMetricsAutoConfiguration.class,
        SystemMetricsAutoConfiguration.class,
        // Not present in Spring Boot 3
        //WebMvcMetricsAutoConfiguration.class,
        MetricsEndpointAutoConfiguration.class
})
public abstract class AbstractSpringBootApplication extends SpringBootServletInitializer {

    @Autowired StartupConfiguration startupConfiguration;

    @Bean
    public ServletListenerRegistrationBean<RequestContextListener> requestContextListener() {
        return new ServletListenerRegistrationBean<>(new RequestContextListener());
    }

    @Bean
    public FilterRegistrationBean<MidPointProfilingServletFilter> midPointProfilingServletFilter() {
        FilterRegistrationBean<MidPointProfilingServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MidPointProfilingServletFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<WicketFilter> wicket() {
        FilterRegistrationBean<WicketFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new WicketFilter());
        registration.setDispatcherTypes(DispatcherType.ERROR, DispatcherType.REQUEST, DispatcherType.FORWARD);
        registration.addUrlPatterns("/*");
        registration.addInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
        // By default Wicket is in "deployment" mode. To override this in development, add
        // "-Dwicket.configuration=development" in the run configuration.
        registration.addInitParameter(Application.CONFIGURATION, "deployment");
        registration.addInitParameter("applicationBean", "midpointApplication");
        registration.addInitParameter(WicketFilter.APP_FACT_PARAM, "org.apache.wicket.spring.SpringWebApplicationFactory");

        return registration;
    }

    // Overriding bean from org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration
    // This method is not very clean. We should probably subclass WebSecurityConfiguration instead.
    // This is the reason that global bean override is allowed in application.yml
    @Bean
    public FilterRegistrationBean<DelegatingFilterProxy> springSecurityFilterChain() {
        FilterRegistrationBean<DelegatingFilterProxy> registration = new FilterRegistrationBean<>();
        registration.setFilter(new DelegatingFilterProxy());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        return new MidPointErrorPageRegistrar();
    }
}
