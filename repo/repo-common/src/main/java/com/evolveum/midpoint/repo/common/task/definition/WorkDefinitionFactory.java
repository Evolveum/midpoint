/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.definition;

import com.evolveum.midpoint.schema.util.task.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionWrapper;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkDefinitionFactory {

    private final Map<QName, WorkDefinitionSupplier> byTypeName = new ConcurrentHashMap<>();
    private final Map<String, WorkDefinitionSupplier> byHandlerUri = new ConcurrentHashMap<>();

    public void registerSupplier(QName typeName, String handlerUri, WorkDefinitionSupplier supplier) {
        byTypeName.put(typeName, supplier);
        if (handlerUri != null) {
            byHandlerUri.put(handlerUri, supplier);
        }
    }

    public void unregisterSupplier(QName typeName, String handlerUri) {
        byTypeName.remove(typeName);
        if (handlerUri != null) {
            byHandlerUri.remove(handlerUri);
        }
    }

    WorkDefinition getWorkFromBean(WorkDefinitionsType definitions) throws SchemaException {
        List<WorkDefinitionWrapper> actions = WorkDefinitionUtil.getWorkDefinitions(definitions);
        if (actions.isEmpty()) {
            return null;
        } else if (actions.size() > 1) {
            throw new SchemaException("Ambiguous definition: " + actions);
        } else {
            return getWorkFromBean(actions.get(0));
        }
    }

    private WorkDefinition getWorkFromBean(WorkDefinitionWrapper definitionWrapper) {
        QName typeName = definitionWrapper.getBeanTypeName();
        WorkDefinitionSupplier supplier = MiscUtil.requireNonNull(
                byTypeName.get(typeName),
                () -> new IllegalStateException("No work definition supplier for " + typeName));
        return supplier.provide(definitionWrapper);
    }

    WorkDefinition getWorkFromTaskLegacy(Task task) {
        String handlerUri = task.getHandlerUri();
        if (handlerUri == null) {
            return null;
        }

        WorkDefinitionSupplier supplier = byHandlerUri.get(handlerUri);
        if (supplier == null) {
            return null;
        }

        return supplier.provide(LegacyWorkDefinitionSource.create(handlerUri, task.getExtensionOrClone()));
    }

    @FunctionalInterface
    public interface WorkDefinitionSupplier {
        WorkDefinition provide(@NotNull WorkDefinitionSource source);
    }
}
