/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.prism.PrismContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * To be used in tests only. Mainly for access PrismContext from static assertion methods.
 */

@Component
public class TestSpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        context = ctx;
    }

    @SuppressWarnings("WeakerAccess")
    public static ApplicationContext getApplicationContext() {
        if (context == null) {
            throw new IllegalStateException("Spring application context could not be determined.");
        }
        return context;
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T getBean(Class<T> aClass) {
        String className = aClass.getSimpleName();
        String beanName = Character.toLowerCase(className.charAt(0)) + className.substring(1);
        return getBean(beanName, aClass);
    }

    private static <T> T getBean(String name, Class<T> aClass) {
        T bean = getApplicationContext().getBean(name, aClass);
        if (bean == null) {
            throw new IllegalStateException("Could not find " + name + " bean");
        }
        return bean;
    }

    public static PrismContext getPrismContext() {
        return getBean(PrismContext.class);
    }
}
