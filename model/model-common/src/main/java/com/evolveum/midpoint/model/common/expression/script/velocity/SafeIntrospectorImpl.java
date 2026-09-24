/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.velocity.util.introspection.Introspector;
import org.apache.velocity.util.introspection.SecureIntrospectorImpl;
import org.slf4j.Logger;

import com.evolveum.midpoint.prism.Safe;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Executes access checks related to methods that can be called from Velocity scripts.
 *
 * NOTE: We don't use Velocity-provided {@link SecureIntrospectorImpl} because it is not a good fit for us.
 *
 * - It has slightly different rules of what is considered safe.
 * - Its decision algorithm has very limited configurability.
 * - It does not do logging.
 *
 * We are inspired by it, but we implement our own version of it.
 */
class SafeIntrospectorImpl extends Introspector {

    /** Forbidden for all objects, even those that are otherwise allowed. */
    private static final Collection<String> FORBIDDEN_METHODS =
            List.of("wait", "notify", "notifyAll", "clone", "finalize", "getClass");

    /**
     * Allowed classes for which all methods (except {@link #FORBIDDEN_METHODS}) are allowed.
     * These classes are also allowed to be passed into the Velocity context.
     */
    static final Collection<Class<?>> ALLOWED_CLASSES = List.of(
            String.class,
            Number.class,
            Boolean.class,
            Enum.class,
            XMLGregorianCalendar.class,
            java.util.Date.class,
            java.time.LocalDateTime.class,
            java.time.LocalDate.class,
            java.time.LocalTime.class);

    private static final Collection<Class<?>> COLLECTION_LIKE_CLASSES = List.of(Collection.class, Map.class);

    /** Methods that are allowed for {@link #COLLECTION_LIKE_CLASSES}. */
    private static final Collection<String> ALLOWED_COLLECTION_LIKE_METHODS = List.of("isEmpty", "size");

    SafeIntrospectorImpl(Logger log) {
        super(log);
    }

    @Override
    public Method getMethod(Class<?> c, String name, Object[] params) throws IllegalArgumentException {

        if (AbstractVelocityScriptExecutor.isFullExecutionMode()) {
            return super.getMethod(c, name, params);
        }

        log.trace("getMethod: {}#{}({})", c.getName(), name, params != null ? params.length : 0);
        var method = super.getMethod(c, name, params);
        if (method == null) {
            log.trace("Method {}#{}({}) not found", c.getName(), name, params != null ? params.length : 0);
            return null;
        }
        var methodName = method.getName();
        if (FORBIDDEN_METHODS.contains(methodName)) {
            log.error("Method {}#{} is globally forbidden -> won't execute", c.getName(), methodName);
            return null;
        }
        if (ALLOWED_CLASSES.stream().anyMatch(allowedClass -> allowedClass.isAssignableFrom(c))) {
            log.trace("Method {}#{} is allowed because it belongs to an allowed class -> allowing execution",
                    c.getName(), methodName);
            return method;
        }
        boolean isCollectionLike = COLLECTION_LIKE_CLASSES.stream().anyMatch(clazz -> clazz.isAssignableFrom(c));
        if (isCollectionLike && ALLOWED_COLLECTION_LIKE_METHODS.contains(methodName)) {
            log.trace("Method {}#{} is allowed because it belongs to an allowed collection-like methods -> allowing execution",
                    c.getName(), methodName);
            return method;
        }
        // Note: the rest of collection access should be done through #foreach only
        // The rest of the methods are our own ones, so we can check for @Safe annotation
        if (!method.isAnnotationPresent(Safe.class)) {
            log.error("Method {}#{} is not annotated with @Safe -> won't execute", c.getName(), methodName);
            return null;
        }
        log.trace("Method {}#{} is annotated with @Safe -> allowing execution", c.getName(), methodName);
        return method;
    }

    @Override
    public Field getField(Class<?> c, String name) throws IllegalArgumentException {
        if (AbstractVelocityScriptExecutor.isFullExecutionMode()) {
            return super.getField(c, name);
        } else {
            // Used when accessing public fields in Java classes directly, i.e., without a getter. We don't allow that.
            return null;
        }
    }

    public Logger getLog() {
        return log;
    }
}
