/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processes.common;

import org.springframework.context.ApplicationContext;

/**
 * This was years-old hack to provide {@link ApplicationContext} and requested Spring beans to clients
 * that needed them. We are keeping the hack, but migrated it to a more suitable place.
 *
 * @see com.evolveum.midpoint.model.impl.expr.SpringApplicationContextHolder
 */
@Deprecated
public class SpringApplicationContextHolder {

    public static ApplicationContext getApplicationContext() {
        return com.evolveum.midpoint.model.impl.expr.SpringApplicationContextHolder.getApplicationContext();
    }

    public static<T> T getBean(Class<T> aClass) {
        return com.evolveum.midpoint.model.impl.expr.SpringApplicationContextHolder.getBean(aClass);
    }

    public static<T> T getBean(String name, Class<T> aClass) {
        return com.evolveum.midpoint.model.impl.expr.SpringApplicationContextHolder.getBean(name, aClass);
    }
}
