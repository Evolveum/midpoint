/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.util.Assert;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author skublik
 */

public class MidpointAutowiredBeanFactoryObjectPostProcessor implements ObjectPostProcessor<Object>, DisposableBean, SmartInitializingSingleton {
    private static final Trace LOGGER = TraceManager.getTrace(MidpointAutowiredBeanFactoryObjectPostProcessor.class);
    private final AutowireCapableBeanFactory autowiredBeanFactory;
    private final List<DisposableBean> disposableBeans = new ArrayList();
    private final List<SmartInitializingSingleton> smartSingletons = new ArrayList();

    public MidpointAutowiredBeanFactoryObjectPostProcessor(AutowireCapableBeanFactory autowiredBeanFactory) {
        Assert.notNull(autowiredBeanFactory, "autowiredBeanFactory cannot be null");
        this.autowiredBeanFactory = autowiredBeanFactory;
    }

    public <T> T postProcess(T object) {
        if (object == null) {
            return null;
        } else {
            Object result = null;

            try {
                result = this.autowiredBeanFactory.initializeBean(object, object.toString());
            } catch (RuntimeException var5) {
                Class<?> type = object.getClass();
                throw new RuntimeException("Could not postProcess " + object + " of type " + type, var5);
            }

            this.autowiredBeanFactory.autowireBean(object);
            if (result instanceof DisposableBean) {
                this.disposableBeans.add((DisposableBean)result);
            }

            if (result instanceof SmartInitializingSingleton) {
                this.smartSingletons.add((SmartInitializingSingleton)result);
            }

            return (T) result;
        }
    }

    public void afterSingletonsInstantiated() {
        Iterator var1 = this.smartSingletons.iterator();

        while(var1.hasNext()) {
            SmartInitializingSingleton singleton = (SmartInitializingSingleton)var1.next();
            singleton.afterSingletonsInstantiated();
        }

    }

    public void destroyAndRemoveFilters(List<Filter> filters) {
        synchronized (this) {
            for (Filter filter : filters) {
                if (filter instanceof DisposableBean && this.disposableBeans.contains(filter)) {
                    filter.destroy();
                    disposableBeans.remove(filter);
                }
            }
        }
    }

    public void destroy() {
        synchronized (this) {
            Iterator<DisposableBean> disposableBeansIterator = this.disposableBeans.iterator();

            while (disposableBeansIterator.hasNext()) {
                DisposableBean disposableBean = disposableBeansIterator.next();
                destroyBean(disposableBean);
            }
        }
    }

    private void destroyBean(DisposableBean disposableBean){
        try {
            disposableBean.destroy();
        } catch (Exception e) {
            this.LOGGER.error("Couldn't destroy bean :" + disposableBean, e);
        }
    }
}
