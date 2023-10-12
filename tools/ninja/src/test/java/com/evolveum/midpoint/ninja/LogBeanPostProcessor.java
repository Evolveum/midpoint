/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@Component
public class LogBeanPostProcessor implements BeanPostProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(LogBeanPostProcessor.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        LOGGER.info("{}: {} before initialization", beanName, bean.getClass().getSimpleName());

        return bean;

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        LOGGER.info("{}: {} after initialization", beanName, bean.getClass().getSimpleName());

        return bean;
    }
}
