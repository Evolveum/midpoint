/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.handlers;

import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Registry of activity handlers for different work definition types (either standard or customer-provided).
 *
 * This is similar to the task handler registry. However, the task handlers were identified by URI, whereas
 * activity handlers have no such direct identifier. They are selected by work definition class
 * (like `RecomputationWorkDefinition`) that is itself found in the work definition factory by either work definition
 * bean type name (like `RecomputationWorkDefinitionType`) or legacy task handler URI (like `.../recompute/handler-3`).
 */
@Component
@Experimental
public class ActivityHandlerRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityHandlerRegistry.class);

    @Autowired WorkDefinitionFactory workDefinitionFactory;

    /**
     * Contains activity implementation objects keyed by work definition class (e.g. PropagationWorkDefinition).
     */
    @NotNull private final Map<Class<? extends WorkDefinition>, ActivityHandler<?, ?>> handlersMap = new ConcurrentHashMap<>();

    /**
     * Maps legacy handler URI to archetype OID. This is a temporary tool for the current GUI.
     */
    @NotNull private final Map<String, String> archetypeMap = new ConcurrentHashMap<>();

    /**
     * Registers both the work definition factory and the activity handler.
     */
    public void register(QName typeName, String legacyHandlerUri, Class<? extends WorkDefinition> definitionClass,
            WorkDefinitionFactory.WorkDefinitionSupplier supplier, ActivityHandler<?, ?> activityHandler) {
        workDefinitionFactory.registerSupplier(typeName, legacyHandlerUri, supplier);
        registerHandler(definitionClass, activityHandler);
        String defaultArchetypeOid = activityHandler.getDefaultArchetypeOid();
        if (legacyHandlerUri != null && defaultArchetypeOid != null) {
            registerArchetypeOid(legacyHandlerUri, defaultArchetypeOid);
        }
    }

    /**
     * Registers the activity handler.
     */
    public void registerHandler(Class<? extends WorkDefinition> definitionClass, ActivityHandler<?, ?> activityHandler) {
        LOGGER.trace("Registering {} for {}", activityHandler, definitionClass);
        handlersMap.put(definitionClass, activityHandler);
    }

    private void registerArchetypeOid(String legacyHandlerUri, String archetypeOid) {
        archetypeMap.put(legacyHandlerUri, archetypeOid);
    }

    /**
     * Unregisters work definition factory and activity handler.
     */
    public void unregister(QName typeName, String legacyHandlerUri, Class<? extends WorkDefinition> definitionClass) {
        workDefinitionFactory.unregisterSupplier(typeName, legacyHandlerUri);
        unregisterHandler(definitionClass);
        if (legacyHandlerUri != null) {
            unregisterArchetypeOid(legacyHandlerUri);
        }
    }

    /**
     * Unregisters the activity handler.
     */
    public void unregisterHandler(Class<? extends WorkDefinition> definitionClass) {
        LOGGER.trace("Unregistering activity handler for {}", definitionClass);
        handlersMap.remove(definitionClass);
    }

    private void unregisterArchetypeOid(String legacyHandlerUri) {
        archetypeMap.remove(legacyHandlerUri);
    }

    @NotNull
    public <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> AH getHandler(
            @NotNull ActivityDefinition<WD> activityDefinition) {
        WorkDefinition workDefinition = activityDefinition.getWorkDefinition();
        Class<? extends WorkDefinition> workDefinitionClass = workDefinition.getClass();
        //noinspection unchecked
        return (AH) requireNonNull(handlersMap.get(workDefinitionClass),
                        () -> new IllegalStateException("Couldn't find implementation for " + workDefinitionClass +
                                " in " + activityDefinition));
    }

    public @Nullable String getArchetypeOid(@NotNull String legacyHandlerUri) {
        return archetypeMap.get(legacyHandlerUri);
    }
}
