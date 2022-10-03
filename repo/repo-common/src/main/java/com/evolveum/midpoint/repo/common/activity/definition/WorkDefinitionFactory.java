/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskHandler;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates {@link WorkDefinition} instances from their serialized form,
 * either "new" (activity definition bean) or "legacy" (task extension).
 */
@Component
public class WorkDefinitionFactory {

    @Autowired ActivityBasedTaskHandler activityBasedTaskHandler;

    private final Map<QName, WorkDefinitionSupplier> byTypeName = new ConcurrentHashMap<>();
    private final Map<String, WorkDefinitionSupplier> byLegacyHandlerUri = new ConcurrentHashMap<>();

    /**
     * Takes care of registering legacy URI in the generic task handler as well.
     */
    public void registerSupplier(QName typeName, String legacyHandlerUri, WorkDefinitionSupplier supplier) {
        byTypeName.put(typeName, supplier);
        if (legacyHandlerUri != null) {
            byLegacyHandlerUri.put(legacyHandlerUri, supplier);
            activityBasedTaskHandler.registerLegacyHandlerUri(legacyHandlerUri);
        }
    }

    public void unregisterSupplier(QName typeName, String legacyHandlerUri) {
        byTypeName.remove(typeName);
        if (legacyHandlerUri != null) {
            byLegacyHandlerUri.remove(legacyHandlerUri);
            activityBasedTaskHandler.unregisterLegacyHandlerUri(legacyHandlerUri);
        }
    }

    WorkDefinition getWorkFromBean(WorkDefinitionsType definitions) throws SchemaException, ConfigurationException {
        List<WorkDefinitionWrapper> actions = WorkDefinitionUtil.getWorkDefinitions(definitions);
        if (actions.isEmpty()) {
            return null;
        } else if (actions.size() > 1) {
            throw new SchemaException("Ambiguous definition: " + actions);
        } else {
            return getWorkFromBean(actions.get(0));
        }
    }

    private WorkDefinition getWorkFromBean(WorkDefinitionWrapper definitionWrapper)
            throws SchemaException, ConfigurationException {
        QName typeName = definitionWrapper.getBeanTypeName();
        WorkDefinitionSupplier supplier = MiscUtil.requireNonNull(
                byTypeName.get(typeName),
                () -> new IllegalStateException("No work definition supplier for " + typeName));
        return supplier.provide(definitionWrapper);
    }

    WorkDefinition getWorkFromTaskLegacy(Task task) throws SchemaException, ConfigurationException {
        String handlerUri = task.getHandlerUri();
        if (handlerUri == null) {
            return null;
        }

        WorkDefinitionSupplier supplier = byLegacyHandlerUri.get(handlerUri);
        if (supplier == null) {
            return null;
        }

        return supplier.provide(
                LegacyWorkDefinitionSource.create(
                        task.getRawTaskObjectClonedIfNecessary().asObjectable()));
    }

    @FunctionalInterface
    public interface WorkDefinitionSupplier {
        WorkDefinition provide(@NotNull WorkDefinitionSource source) throws SchemaException, ConfigurationException;
    }
}
