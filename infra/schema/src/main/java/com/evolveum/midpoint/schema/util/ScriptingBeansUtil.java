/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.castSafely;

/**
 * Utility methods related to {@link ScriptingExpressionType} beans.
 */
public class ScriptingBeansUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptingBeansUtil.class);

    /** Map of types to corresponding element names. We assume there are no conflicts. */
    private static final Map<Class<? extends ScriptingExpressionType>, QName> ELEMENTS = new HashMap<>();

    static {
        ObjectFactory objectFactory = new ObjectFactory();

        for (Method method : objectFactory.getClass().getDeclaredMethods()) {
            if (method.getReturnType() == JAXBElement.class) {
                JAXBElement<? extends ScriptingExpressionType> jaxbElement;
                try {
                    //noinspection unchecked
                    jaxbElement = (JAXBElement<? extends ScriptingExpressionType>) method.invoke(objectFactory, new Object[1]);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Couldn't execute " + method);
                }

                QName elementName = jaxbElement.getName();
                Class<? extends ScriptingExpressionType> elementType = jaxbElement.getDeclaredType();
                if (ELEMENTS.containsKey(elementType)) {
                    throw new IllegalStateException(
                            "More than one JAXBElement for %s: %s, %s".formatted(
                                    elementType, elementName, ELEMENTS.get(elementType)));
                } else {
                    ELEMENTS.put(elementType, elementName);
                }
            }
        }
        LOGGER.trace("Elements map: {}", ELEMENTS);
    }

    /**
     * Sometimes we have to convert "bare" {@link ScriptingExpressionType} instance to the {@link JAXBElement} version,
     * with the correct element name.
     */
    private static <T extends ScriptingExpressionType> JAXBElement<T> toJaxbElement(@NotNull T expression) {
        Class<? extends ScriptingExpressionType> clazz = expression.getClass();
        //noinspection unchecked
        return new JAXBElement<>(
                MiscUtil.argNonNull(
                        ELEMENTS.get(clazz),
                        "Unsupported expression type: %s", clazz),
                (Class<T>) clazz,
                expression);
    }

    public static @NotNull String getActionType(@NotNull ActionExpressionType action) {
        if (action.getType() != null) {
            return action.getType();
        } else {
            return toJaxbElement(action).getName().getLocalPart();
        }
    }

    public static <T> T getBeanPropertyValue(ActionExpressionType action, String propertyName, Class<T> clazz)
            throws SchemaException {
        try {
            try {
                Object rawValue = PropertyUtils.getSimpleProperty(action, propertyName);
                return castSafely(rawValue, clazz);
            } catch (NoSuchMethodException e) {
                if (Boolean.class.equals(clazz)) {
                    // Note that getSimpleProperty looks for "getX" instead of our "isX" getter for Boolean (not boolean) props.
                    //noinspection unchecked
                    return (T) getBeanBooleanPropertyValue(action, propertyName);
                } else {
                    // This can occur when dynamic parameters are used: the action is of generic type, not the specific one.
                    return null;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SchemaException("Couldn't access '" + propertyName + "' in '" + action + "'", e);
        }
    }

    private static Boolean getBeanBooleanPropertyValue(ActionExpressionType action, String propertyName)
            throws IllegalAccessException, InvocationTargetException, SchemaException {
        try {
            String methodName = "is" + StringUtils.capitalize(propertyName);
            Object rawValue = MethodUtils.invokeExactMethod(action, methodName, new Object[0]);
            return castSafely(rawValue, Boolean.class);
        } catch (NoSuchMethodException e) {
            // This can occur when dynamic parameters are used: the action is of generic type, not the specific one.
            return null;
        }
    }

    public static @NotNull ExecuteScriptType createExecuteScriptCommand(@NotNull ScriptingExpressionType expression) {
        return new ExecuteScriptType()
                .scriptingExpression(
                        toJaxbElement(expression));
    }

    public static @NotNull ExecuteScriptType asExecuteScriptCommand(@NotNull Object object) {
        if (object instanceof ExecuteScriptType executeScript) {
            return executeScript;
        } else if (object instanceof ScriptingExpressionType scriptingExpression) {
            return createExecuteScriptCommand(scriptingExpression);
        } else {
            throw new IllegalArgumentException("Expected ExecuteScriptType or ScriptingExpressionType, got " + object);
        }
    }
}
