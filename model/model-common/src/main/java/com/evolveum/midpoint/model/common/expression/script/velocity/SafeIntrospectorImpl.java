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
import javax.xml.namespace.QName;

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
     * see {@link #TYPES_SAFE_TO_INTO_CONTEXT_PREDICATE}.
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

    /**
     * Allowed classes for which only some methods are allowed. The allowed methods are listed in the map.
     *
     * These classes are also allowed to be passed into the Velocity context (along with others),
     * see {@link #TYPES_SAFE_TO_INTO_CONTEXT_PREDICATE}.
     */
    private static final Map<Class<?>, List<String>> ALLOWED_METHODS_BY_TYPE = Map.of(
            Enum.class,
            List.of("name", "ordinal", "toString"),

            XMLGregorianCalendar.class,
            List.of("getYear", "getMonth", "getDay", "getHour", "getMinute",
                    "getSecond", "getMillisecond", "getFractionalSecond", "getTimezone", "toXMLFormat",
                    "toString"),

            QName.class,
            List.of("getNamespaceURI", "getLocalPart", "getPrefix"),

            Collection.class,
            List.of("isEmpty", "size"),

            Map.class,
            List.of("isEmpty", "size")
    );

    /**
     * These are safe to put into context (with subclasses) but not to call all methods on.
     *
     * TODO specify allowed methods to call for these classes.
     */
    private static final Collection<Class<?>> TYPES_SAFE_TO_PUT_INTO_CONTEXT = List.of(
            java.util.Date.class,
            java.time.LocalDateTime.class,
            java.time.LocalDate.class,
            java.time.LocalTime.class);

    static final Predicate<Class<?>> TYPES_SAFE_TO_INTO_CONTEXT_PREDICATE =
            c -> CLASSES_SAFE_TO_CALL.stream().anyMatch(allowedClass -> allowedClass.isAssignableFrom(c))
                    || ALLOWED_METHODS_BY_TYPE.keySet().stream().anyMatch(allowedClass -> allowedClass.isAssignableFrom(c))
                    || TYPES_SAFE_TO_PUT_INTO_CONTEXT.stream().anyMatch(allowedClass -> allowedClass.isAssignableFrom(c))
                    || isAnnotatedAsSafe(c);

    private static boolean isAnnotatedAsSafe(Class<?> clazz) {
        // This looks at all superclasses and intefaces of given class, so it is enough to annotate a base class or interface
        // to mark all subclasses safe.
        //
        // However, beware that it also looks for it as a meta-annotation (i.e. annotation of an annotation of the class).
        // So please don't use {@link Safe} as a meta-annotation for other annotations. It is not intended for that purpose.
        return AnnotationUtils.findAnnotation(clazz, Safe.class) != null;
    }

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

        for (Map.Entry<Class<?>, List<String>> entry : ALLOWED_METHODS_BY_TYPE.entrySet()) {
            Class<?> allowedClass = entry.getKey();
            List<String> allowedMethods = entry.getValue();
            if (allowedClass.isAssignableFrom(c) && allowedMethods.contains(methodName)) {
                log.trace("Method {}#{} is allowed because it belongs to an allowed class and method -> allowing execution",
                        c.getName(), methodName);
                return method;
            }
        }

        // The rest of the methods are our own ones, so we can check for @Safe annotation
        if (isSafeAnnotationPresentOnMethodOrRelatedMethod(c, method)) {
            log.trace("Method {}#{} is annotated with @Safe -> allowing execution", c.getName(), methodName);
            return method;
        }

        log.error("Method {}#{} is not annotated with @Safe -> won't execute", c.getName(), methodName);
        return null;
    }

    /**
     * In Velocity, the method is not always found on the class it is declared in, but SOMETIMES (strangely)
     * on one of its superclasses or interfaces. For example, {@code common_3.ObjectReferenceType#getType} is found
     * as {@code Referencable#getType}, not as {@code AbstractReferencable#getType}, where it's defined.
     *
     * To be deterministic, let's explicitly check for the method on the class in question.
     */
    private boolean isSafeAnnotationPresentOnMethodOrRelatedMethod(Class<?> c, Method method) {
        if (isAnnotatedAsSafe(method)) {
            return true; // This is hopefully the case
        }

        try {
            // Can take some time, but hopefully this is not called too often. We can cache the result if needed.
            var realMethod = c.getMethod(method.getName(), method.getParameterTypes());
            return isAnnotatedAsSafe(realMethod);
        } catch (NoSuchMethodException e) {
            log.error("Method {}#{} was found on a superclass or interface, but not on the class itself. This should not happen.",
                    c.getName(), method.getName(), e);
            return false;
        }
    }

    private static boolean isAnnotatedAsSafe(Method method) {
        return AnnotationUtils.findAnnotation(method, Safe.class) != null;
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
