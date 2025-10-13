/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.test;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.lang.reflect.Modifier;
import java.util.*;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Keeps the information about objects of particular types in repository, and does some asserts on it.
 */
public class ObjectsCounter {

    private final Collection<Class<? extends ObjectType>> typesToCount;

    private final Map<Class<? extends ObjectType>, Integer> lastState = new HashMap<>();

    @SafeVarargs
    public ObjectsCounter(Class<? extends ObjectType>... typesToCount) {
        this.typesToCount = List.of(typesToCount);
    }

    public void remember(OperationResult result) {
        lastState.clear();
        countObjects(lastState, result);
    }

    public void assertNoNewObjects(OperationResult result) {
        Map<Class<? extends ObjectType>, Integer> currentState = new HashMap<>();
        countObjects(currentState, result);
        assertThat(currentState).as("current objects counts").isEqualTo(lastState);
    }

    public void assertShadowOnlyIncrement(int expected, OperationResult result) {
        assertIncrement(ShadowType.class, expected, result);
    }

    public void assertUserOnlyIncrement(int expected, OperationResult result) {
        assertIncrement(UserType.class, expected, result);
    }

    private void assertIncrement(Class<? extends ObjectType> clazz, int expectedIncrement, OperationResult result) {
        assertIncrement(
                Map.of(clazz, expectedIncrement),
                result);
    }

    private void assertIncrement(Map<Class<? extends ObjectType>, Integer> expectedIncrement, OperationResult result) {
        Map<Class<? extends ObjectType>, Integer> currentState = new HashMap<>();
        countObjects(currentState, result);
        add(lastState, expectedIncrement);
        assertThat(lastState)
                .as("last state plus expected increment (" + expectedIncrement + ")")
                .isEqualTo(currentState);
    }

    @SuppressWarnings("SameParameterValue")
    private void add(
            Map<Class<? extends ObjectType>, Integer> countMap,
            Map<Class<? extends ObjectType>, Integer> incrementMap) {
        incrementMap.forEach(
                (clazz, increment) -> add(countMap, clazz, or0(increment)));

    }

    @SuppressWarnings("SameParameterValue")
    private void add(Map<Class<? extends ObjectType>, Integer> countMap, Class<? extends ObjectType> clazz, int increment) {
        countMap.compute(
                clazz,
                (aClass, oldValue) -> or0(oldValue) + increment);
    }

    private void countObjects(Map<Class<? extends ObjectType>, Integer> state, OperationResult result) {
        RepositoryService repositoryService = ModelCommonBeans.get().cacheRepositoryService;
        for (ObjectTypes type : ObjectTypes.values()) {
            Class<? extends ObjectType> clazz = type.getClassDefinition();
            if (!Modifier.isAbstract(clazz.getModifiers())
                    && typesToCount.stream().anyMatch(c -> c.isAssignableFrom(clazz))) {
                try {
                    state.put(
                            clazz,
                            repositoryService.countObjects(clazz, null, null, result));
                } catch (SchemaException e) {
                    throw SystemException.unexpected(e);
                }
            }
        }
    }
}
