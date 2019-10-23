package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.web.boot.WebSecurityConfig;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.accept.ContentNegotiationStrategy;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MidpointWebSecurityConfiguration extends WebSecurityConfiguration {

    @Autowired(required = false)
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Autowired
    ApplicationContext context;

    @Override
    public Filter springSecurityFilterChain() throws Exception {
        Filter filter = super.springSecurityFilterChain();
        MidpointFilterChainProxy mpFilter = objectObjectPostProcessor.postProcess(new MidpointFilterChainProxy(((FilterChainProxy) filter).getFilterChains()));
        mpFilter.afterPropertiesSet();
        return mpFilter;
    }


}
