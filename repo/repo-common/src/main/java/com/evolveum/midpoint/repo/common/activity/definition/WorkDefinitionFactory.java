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

import com.evolveum.midpoint.xml.ns._public.common.common_3.RecomputationWorkDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskHandler;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;
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

    /** Suppliers indexed by work definition type name (e.g. {@link RecomputationWorkDefinitionType}). */
    private final Map<QName, WorkDefinitionSupplier> byTypeName = new ConcurrentHashMap<>();

    public void registerSupplier(@NotNull QName typeName, @NotNull WorkDefinitionSupplier supplier) {
        byTypeName.put(typeName, supplier);
    }

    public void unregisterSupplier(@NotNull QName typeName) {
        byTypeName.remove(typeName);
    }

    /** Transforms configuration bean (user-friendly form) into parsed {@link WorkDefinition} object. */
    WorkDefinition getWorkFromBean(WorkDefinitionsType definitions) throws SchemaException, ConfigurationException {
        List<WorkDefinitionBean> beans = WorkDefinitionUtil.getWorkDefinitionBeans(definitions);
        if (beans.isEmpty()) {
            return null;
        } else if (beans.size() > 1) {
            throw new SchemaException("Ambiguous definition: " + beans);
        } else {
            return getWorkFromBean(beans.get(0));
        }
    }

    private WorkDefinition getWorkFromBean(WorkDefinitionBean definitionBean)
            throws SchemaException, ConfigurationException {
        QName typeName = definitionBean.getBeanTypeName();
        WorkDefinitionSupplier supplier = MiscUtil.requireNonNull(
                byTypeName.get(typeName),
                () -> new IllegalStateException("No work definition supplier for " + typeName));
        return supplier.provide(definitionBean);
    }

    @FunctionalInterface
    public interface WorkDefinitionSupplier {
        WorkDefinition provide(@NotNull WorkDefinitionBean source) throws SchemaException, ConfigurationException;
    }
}
