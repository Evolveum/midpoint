/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.handlers;

import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.task.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinition;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Registry of activity handlers for different work definition types (either standard or customer-provided).
 *
 * This is similar to the task handler registry. However, the task handlers were identified by URI, whereas
 * activity handlers are identified by work definition type name (QName).
 */
@Component
@Experimental
public class ActivityHandlerRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityHandlerRegistry.class);

    /**
     * Contains activity implementation objects keyed by work definition type names
     * (e.g. c:RecomputationDefinitionType or ext:MockDefinitionType).
     */
    private final Map<QName, ActivityHandler<?>> handlersMap = new ConcurrentHashMap<>();

    public void register(QName workDefinitionTypeName, ActivityHandler<?> activityHandler) {
        LOGGER.trace("Registering {} for {}", activityHandler, workDefinitionTypeName);
        handlersMap.put(workDefinitionTypeName, activityHandler);
    }

    public void unregister(QName workDefinitionTypeName) {
        LOGGER.trace("Unregistering activity handler for {}", workDefinitionTypeName);
        handlersMap.remove(workDefinitionTypeName);
    }

    @NotNull
    public <WD extends WorkDefinition> ActivityHandler<WD> getHandler(@NotNull ActivityDefinition<WD> activityDefinition) {
        QName workDefinitionTypeName = activityDefinition.getWorkDefinitionTypeName();
        //noinspection unchecked
        return (ActivityHandler<WD>)
                requireNonNull(handlersMap.get(workDefinitionTypeName),
                        () -> new IllegalStateException("Couldn't find implementation for " + workDefinitionTypeName +
                                " in " + activityDefinition));
    }
}
