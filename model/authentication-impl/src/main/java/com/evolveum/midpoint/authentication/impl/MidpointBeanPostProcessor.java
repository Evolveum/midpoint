/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import com.evolveum.midpoint.authentication.impl.handler.MidpointAccessDeniedHandler;

import com.evolveum.midpoint.authentication.impl.session.MidpointRegisterSessionAuthenticationStrategy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 */

@Component
class MidpointBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CsrfFilter) {
            CsrfFilter csrfFilter = (CsrfFilter) bean;
            csrfFilter.setAccessDeniedHandler(new MidpointAccessDeniedHandler());
        }
        if (bean instanceof RegisterSessionAuthenticationStrategy) {
            RegisterSessionAuthenticationStrategy strategy = (RegisterSessionAuthenticationStrategy) bean;
            return new MidpointRegisterSessionAuthenticationStrategy(strategy);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
