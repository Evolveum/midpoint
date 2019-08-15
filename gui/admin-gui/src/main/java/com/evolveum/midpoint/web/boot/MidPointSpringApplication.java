/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.gui.impl.factory.TextAreaPanelFactory;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.catalina.Valve;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Duration;

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
@ComponentScan(basePackages = {"com.evolveum.midpoint.gui","com.evolveum.midpoint.gui.api"}, basePackageClasses = {TextAreaPanelFactory.class, GuiComponentRegistryImpl.class})
public class MidPointSpringApplication extends AbstractSpringBootApplication {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(MidPointSpringApplication.class);
	
	private static ConfigurableApplicationContext applicationContext = null;
	
	 public static void main(String[] args) {
	    	System.out.println("ClassPath: "+ System.getProperty("java.class.path"));
	    	
	        System.setProperty("xml.catalog.className", "com.evolveum.midpoint.prism.impl.schema.CatalogImpl");
	        String mode = args != null && args.length > 0 ? args[0] : null;
	        
	        if(LOGGER.isDebugEnabled()){
	            LOGGER.debug("PID:" + ManagementFactory.getRuntimeMXBean().getName() +
	                    " Application mode:" + mode + " context:" + applicationContext);
	        }
	        
	        if (applicationContext != null && mode != null && "stop".equals(mode)) {
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
    
    private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder application) {
        String mpHome = System.getProperty(MIDPOINT_HOME_PROPERTY);
        if (StringUtils.isEmpty(mpHome)) {
            LOGGER.info("{} system property is not set, using default configuration", MIDPOINT_HOME_PROPERTY);

            mpHome = System.getProperty(USER_HOME_PROPERTY_NAME);
            if (!mpHome.endsWith("/")) {
                mpHome += "/";
            }
            mpHome += "midpoint";
            System.setProperty(MIDPOINT_HOME_PROPERTY, mpHome);
        }
        
        System.setProperty("spring.config.additional-location", "${midpoint.home}/");

        application.bannerMode(Banner.Mode.LOG);

        return application.sources(MidPointSpringApplication.class);
    }
	
    @Component
    public class ServerCustomization implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    	
    	@Value("${server.servlet.session.timeout}")
    	private int sessionTimeout;
    	@Value("${server.servlet.context-path}")
		private String servletPath;
    	
    	@Autowired 
    	ServerProperties serverProperties;
		
    	@Override
    	public void customize(ConfigurableServletWebServerFactory server) {
    		
    		ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer = new ServletWebServerFactoryCustomizer(this.serverProperties);
        	servletWebServerFactoryCustomizer.customize(server);
    		
    		server.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED,
                  "/error/401"));
    		server.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN,
                  "/error/403"));
    		server.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND,
                  "/error/404"));
    		server.addErrorPages(new ErrorPage(HttpStatus.GONE,
                  "/error/410"));
    		server.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR,
                  "/error"));
    
    		Session session = new Session(); 
    		session.setTimeout(Duration.ofMinutes(sessionTimeout));
    		server.setSession(session);
    		
    		if (server instanceof TomcatServletWebServerFactory) {
    			customizeTomcat((TomcatServletWebServerFactory) server);
    		}            
    	}
    
    	private void customizeTomcat(TomcatServletWebServerFactory tomcatFactory) {
    		// Tomcat valve used to redirect root URL (/) to real application URL (/midpoint/).
    		// See comments in TomcatRootValve
    		Valve rootValve = new TomcatRootValve(servletPath);
    		tomcatFactory.addEngineValves(rootValve);
    	}

    }
}