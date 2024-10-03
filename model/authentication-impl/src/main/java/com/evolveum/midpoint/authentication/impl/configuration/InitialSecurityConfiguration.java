/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.configuration;

import com.evolveum.midpoint.authentication.impl.MidpointAutowiredBeanFactoryObjectPostProcessor;
import com.evolveum.midpoint.authentication.impl.session.MidpointSessionRegistryImpl;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.impl.session.SessionAndRequestScopeImpl;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.core.session.SessionRegistry;

/**
 * Class with configuration that we need before we start creating of authentication filters.
 */
@Configuration
public class InitialSecurityConfiguration {

    @Bean
    public SessionRegistry sessionRegistry(RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher) {
        return new MidpointSessionRegistryImpl(removeUnusedSecurityFilterPublisher);
    }

    @Bean
    public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return factory ->
                factory.registerScope("sessionAndRequest", new SessionAndRequestScopeImpl());
    }

    @Primary
    @Bean
    public ObjectPostProcessor<Object> postProcessor(AutowireCapableBeanFactory autowireBeanFactory) {
        return new MidpointAutowiredBeanFactoryObjectPostProcessor(autowireBeanFactory);
    }

}
