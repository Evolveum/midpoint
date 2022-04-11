/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Used to obtain arbitrary Spring beans from withing scripts.
 *
 * (To be used only on rare occasions. All the standard beans should be accessible via {@link MidpointFunctions} API.)
 *
 * *USE AT YOUR OWN RISK.*
 */
@Experimental
@Component
public class SpringApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    public void setApplicationContext(@NotNull ApplicationContext ctx) throws BeansException {
        context = ctx;
    }

    public static @NotNull ApplicationContext getApplicationContext() {
        return Objects.requireNonNull(context, "Spring application context could not be determined.");
    }

    @SuppressWarnings("unused") // externally used
    public static<T> T getBean(Class<T> aClass) {
        String className = aClass.getSimpleName();
        String beanName = Character.toLowerCase(className.charAt(0)) + className.substring(1);
        return getBean(beanName, aClass);
    }

    @SuppressWarnings("WeakerAccess") // externally used
    public static<T> T getBean(String name, Class<T> aClass) {
        return Objects.requireNonNull(
                getApplicationContext().getBean(name, aClass),
                () -> "Could not find " + name + " bean");
    }
}
