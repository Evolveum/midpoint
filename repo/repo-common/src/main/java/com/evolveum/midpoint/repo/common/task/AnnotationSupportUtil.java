/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.util.exception.SystemException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.function.Function;

/**
 * Takes care of instantiation of auxiliary objects like task/part executions and result handlers.
 */
class AnnotationSupportUtil {

    public static <A extends Annotation, T> Object createFromAnnotation(Object holder, Object constructorParameter,
            Object potentialEnclosingObject, Class<A> annotationClass, Function<A, Class<?>> valueFunction, String description) {
        try {
            Class<?> targetClass = valueFunction.apply(getRequiredAnnotation(holder, annotationClass));
            return instantiate(targetClass, constructorParameter, potentialEnclosingObject);
        } catch (Throwable t) {
            throw new SystemException("Cannot create " + description + " from @" + annotationClass.getSimpleName() +
                    " annotation of " + holder.getClass() + ": " + t.getMessage(), t);
        }
    }

    public static <A extends Annotation> A getRequiredAnnotation(Object holder, Class<A> annotationClass) {
        return java.util.Objects.requireNonNull(holder.getClass().getAnnotation(annotationClass),
                "The @" + annotationClass.getSimpleName() + " annotation is missing.");
    }

    static <T> T instantiate(Class<T> targetClass, Object constructorParameter,
            Object potentialEnclosingObject) throws IllegalAccessException, InvocationTargetException,
            InstantiationException, NoSuchMethodException {
        Constructor<T> constructor;
        try {
            constructor = targetClass.getDeclaredConstructor(constructorParameter.getClass());
            return constructor.newInstance(constructorParameter);
        } catch (NoSuchMethodException e) {
            if (targetClass.isMemberClass() && !Modifier.isStatic(targetClass.getModifiers())) {
                Constructor<T> altConstructor = targetClass.getDeclaredConstructor(
                        potentialEnclosingObject.getClass(), constructorParameter.getClass());
                return altConstructor.newInstance(potentialEnclosingObject, constructorParameter);
            } else {
                throw e;
            }
        }
    }
}
