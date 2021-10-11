/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import javax.servlet.DispatcherType;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WicketFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.ThreadDumpEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.evolveum.midpoint.init.StartupConfiguration;
import com.evolveum.midpoint.model.api.authentication.NodeAuthenticationEvaluator;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.MidPointProfilingServletFilter;

import ro.isdc.wro.http.WroFilter;

/**
 * @author katka
 *
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
        HealthIndicatorAutoConfiguration.class,
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
        WebMvcMetricsAutoConfiguration.class,
        MetricsEndpointAutoConfiguration.class
})
public abstract class AbstractSpringBootApplication extends SpringBootServletInitializer {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointSpringApplication.class);

    @Autowired StartupConfiguration startupConfiguration;
    @Autowired NodeAuthenticationEvaluator nodeAuthenticator;


    @Bean
    public ServletListenerRegistrationBean<RequestContextListener> requestContextListener() {
        return new ServletListenerRegistrationBean<>(new RequestContextListener());
    }

    @Bean
    public FilterRegistrationBean<MidPointProfilingServletFilter> midPointProfilingServletFilter() {
        FilterRegistrationBean<MidPointProfilingServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MidPointProfilingServletFilter());
        //            registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
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
        registration.addInitParameter(Application.CONFIGURATION, "deployment");     // deployment development
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
    public FilterRegistrationBean<WroFilter> webResourceOptimizer(WroFilter wroFilter) {
        FilterRegistrationBean<WroFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(wroFilter);
        registration.addUrlPatterns("/wro/*");
        return registration;
    }

    @Bean
    public ServletRegistrationBean<CXFServlet> cxfServlet() {
        ServletRegistrationBean<CXFServlet> registration = new ServletRegistrationBean<>();
        registration.setServlet(new CXFServlet());
        registration.addInitParameter("service-list-path", "midpointservices");
        registration.setLoadOnStartup(1);
        registration.addUrlMappings("/model/*", "/ws/*");

        return registration;
    }

    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        return new MidPointErrorPageRegistrar();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
