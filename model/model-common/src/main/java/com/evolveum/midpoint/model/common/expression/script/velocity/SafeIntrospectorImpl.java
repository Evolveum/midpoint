/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.velocity.util.introspection.Introspector;
import org.apache.velocity.util.introspection.SecureIntrospectorImpl;
import org.slf4j.Logger;

import com.evolveum.midpoint.prism.Safe;

import org.springframework.core.annotation.AnnotationUtils;

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
     * When deciding about methods, we check for class equality, as the subclasses may have additional methods
     * that we don't want to allow.
     *
     * Immutability is required.
     *
     * These classes are also allowed to be passed into the Velocity context (along with others),
     * see {@link #SAFE_TYPE_TO_PUT_INTO_CONTEXT}.
     */
    private static final Collection<Class<?>> CLASSES_SAFE_TO_CALL = List.of(
            String.class,
            Byte.class,
            Character.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            BigInteger.class,
            BigDecimal.class,
            Boolean.class);

    /** These are safe to put into context (with subclasses) but not to call methods on. */
    private static final Collection<Class<?>> CLASSES_SAFE_TO_PUT_INTO_CONTEXT = List.of(
            Enum.class,
            java.util.Date.class,
            java.time.LocalDateTime.class,
            java.time.LocalDate.class,
            java.time.LocalTime.class,
            XMLGregorianCalendar.class);

    private static final List<String> ALLOWED_ENUM_METHODS = List.of("name", "ordinal", "toString");

    private static final List<String> ALLOWED_XML_GREGORIAN_CALENDAR_METHODS =
            List.of("getYear", "getMonth", "getDay", "getHour", "getMinute",
                    "getSecond", "getMillisecond", "getFractionalSecond", "getTimezone", "toXMLFormat",
                    "toString");

    static final Predicate<Class<?>> SAFE_TYPE_TO_PUT_INTO_CONTEXT =
            c -> CLASSES_SAFE_TO_CALL.stream().anyMatch(allowedClass -> allowedClass.isAssignableFrom(c))
                    || CLASSES_SAFE_TO_PUT_INTO_CONTEXT.stream().anyMatch(allowedClass -> allowedClass.isAssignableFrom(c))
                    || isAnnotatedAsSafe(c);

    private static boolean isAnnotatedAsSafe(Class<?> clazz) {
        // This looks at all superclasses and intefaces of given class, so it is enough to annotate a base class or interface
        // to mark all subclasses safe.
        //
        // However, beware that it also looks at _annotations_ of the class, so if any of them is marked as @Safe,
        // the class will be considered safe as well. This is not a problem, but it is something to be aware of.
        // Do not mark any annotation as @Safe unless you really mean it.
        return AnnotationUtils.findAnnotation(clazz, Safe.class) != null;
    }

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
        if (CLASSES_SAFE_TO_CALL.contains(c)) {
            log.trace("Method {}#{} is allowed because it belongs to an allowed class -> allowing execution",
                    c.getName(), methodName);
            return method;
        }
        boolean isCollectionLike = COLLECTION_LIKE_CLASSES.stream().anyMatch(clazz -> clazz.isAssignableFrom(c));
        if (Enum.class.isAssignableFrom(c) && ALLOWED_ENUM_METHODS.contains(methodName)
                || XMLGregorianCalendar.class.isAssignableFrom(c) && ALLOWED_XML_GREGORIAN_CALENDAR_METHODS.contains(methodName)
                || isCollectionLike && ALLOWED_COLLECTION_LIKE_METHODS.contains(methodName)) {
            log.trace("Method {}#{} is allowed because it belongs to specifically allowed methods -> allowing execution",
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
