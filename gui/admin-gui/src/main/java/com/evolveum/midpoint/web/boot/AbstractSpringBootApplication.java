/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.boot;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import javax.servlet.DispatcherType;

import org.apache.catalina.Valve;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WicketFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.evolveum.midpoint.gui.impl.factory.TextAreaPanelFactory;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
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
        HealthIndicatorAutoConfiguration.class
})
public abstract class AbstractSpringBootApplication extends SpringBootServletInitializer{
	
	 private static final Trace LOGGER = TraceManager.getTrace(MidPointSpringApplication.class);

	    protected static final String MIDPOINT_HOME_PROPERTY = "midpoint.home";
	    protected static final String USER_HOME_PROPERTY_NAME = "user.home";
	    
	    
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
//	        registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
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

	   
}
