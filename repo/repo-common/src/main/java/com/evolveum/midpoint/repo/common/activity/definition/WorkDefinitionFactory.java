/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskHandler;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

/**
 * Creates {@link WorkDefinition} instances from their serialized (bean) form. Since 4.8 it is limited to the
 * "new" format, i.e., activity definition bean. The legacy style (task extension) is no longer supported for activity-based
 * tasks.
 */
@Component
public class WorkDefinitionFactory {

    @Autowired ActivityBasedTaskHandler activityBasedTaskHandler;

    private final Map<QName, WorkDefinitionSupplier> byTypeName = new ConcurrentHashMap<>();

    /**
     * Takes care of registering legacy URI in the generic task handler as well.
     */
    public void registerSupplier(QName typeName, WorkDefinitionSupplier supplier) {
        byTypeName.put(typeName, supplier);
    }

    public void unregisterSupplier(QName typeName) {
        byTypeName.remove(typeName);
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

    @FunctionalInterface
    public interface WorkDefinitionSupplier {
        WorkDefinition provide(@NotNull WorkDefinitionSource source) throws SchemaException, ConfigurationException;
    }
}
