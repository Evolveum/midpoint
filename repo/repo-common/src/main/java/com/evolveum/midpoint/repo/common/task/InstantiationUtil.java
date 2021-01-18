/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Takes care of instantiation of auxiliary objects like task/part executions and result handlers.
 */
class InstantiationUtil {

    static <T> T instantiate(Class<T> targetClass, Object constructorParameter,
            Object potentialEnclosingObject) throws IllegalAccessException, InvocationTargetException,
            InstantiationException, NoSuchMethodException {
        if (!targetClass.isMemberClass() || Modifier.isStatic(targetClass.getModifiers())) {
            Constructor<T> constructor = targetClass.getDeclaredConstructor(constructorParameter.getClass());
            return constructor.newInstance(constructorParameter);
        } else {
            Constructor<T> constructor = targetClass.getDeclaredConstructor(
                    potentialEnclosingObject.getClass(), constructorParameter.getClass());
            return constructor.newInstance(potentialEnclosingObject, constructorParameter);
        }
    }
}
