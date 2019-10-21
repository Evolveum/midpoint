/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author katka
 *
 */
@Component
public class SpringApplicationContext implements ApplicationContextAware {

    private static ApplicationContext appCtx;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringApplicationContext.appCtx = applicationContext;
    }

    public static <T extends Object> T getBean(Class<T> beanClass) {
        return appCtx.getBean(beanClass);
    }

}
