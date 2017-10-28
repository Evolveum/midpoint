/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.gui.impl.util.ReportPeerQueryInterceptor;
import com.evolveum.midpoint.prism.schema.CatalogImpl;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.web.util.MidPointProfilingServletFilter;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WicketFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;
import ro.isdc.wro.http.WroFilter;

import javax.servlet.DispatcherType;

/**
 * Created by Viliam Repan (lazyman).
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
@ImportAutoConfiguration(classes = {
        EmbeddedServletContainerAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServerPropertiesAutoConfiguration.class
})
@SpringBootConfiguration
public class MidPointSpringApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        System.setProperty("xml.catalog.className", CatalogImpl.class.getName());

        configureApplication(new SpringApplicationBuilder()).run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return configureApplication(application);
    }

    private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder application) {
        System.setProperty("spring.config.location", "${midpoint.home}/");

        return application.sources(MidPointSpringApplication.class);
    }

    @Bean
    public ServletListenerRegistrationBean requestContextListener() {
        return new ServletListenerRegistrationBean(new RequestContextListener());
    }

    @Bean
    public FilterRegistrationBean midPointProfilingServletFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new MidPointProfilingServletFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean wicket() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new WicketFilter());
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        registration.addUrlPatterns("/*");
        registration.addInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
        registration.addInitParameter(Application.CONFIGURATION, "development");     // development
        registration.addInitParameter("applicationBean", "midpointApplication");
        registration.addInitParameter(WicketFilter.APP_FACT_PARAM, "org.apache.wicket.spring.SpringWebApplicationFactory");

        return registration;
    }

    @Bean
    public FilterRegistrationBean springSecurityFilterChain() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new DelegatingFilterProxy());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean webResourceOptimizer(WroFilter wroFilter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(wroFilter);
        registration.addUrlPatterns("/wro/*");
        return registration;
    }

    @Bean
    public ServletRegistrationBean cxfServlet() {
        ServletRegistrationBean registration = new ServletRegistrationBean();
        registration.setServlet(new CXFServlet());
        registration.addInitParameter("service-list-path", "midpointservices");
        registration.setLoadOnStartup(1);
        registration.addUrlMappings("/model/*", "/ws/*");

        return registration;
    }

    @Bean
    public ServletRegistrationBean reportPeerQueryInterceptor() {
        ServletRegistrationBean registration = new ServletRegistrationBean();
        registration.setServlet(new ReportPeerQueryInterceptor());
        registration.addUrlMappings("/report");

        return registration;
    }

    @Bean
    public ServerProperties serverProperties() {
        return new ServerCustomization();
    }

    private static class ServerCustomization extends ServerProperties {

        @Override
        public void customize(ConfigurableEmbeddedServletContainer container) {

            super.customize(container);
            container.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED,
                    "/error/401"));
            container.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN,
                    "/error/403"));
            container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND,
                    "/error/404"));
            container.addErrorPages(new ErrorPage(HttpStatus.GONE,
                    "/error/410"));
            container.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR,
                    "/error"));
        }
    }
}
