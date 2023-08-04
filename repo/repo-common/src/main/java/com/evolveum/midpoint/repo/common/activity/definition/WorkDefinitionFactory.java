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

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RecomputationWorkDefinitionType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskHandler;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Creates {@link WorkDefinition} instances from their serialized (bean) form. Since 4.8 it is limited to the
 * "new" format, i.e., activity definition bean. The legacy style (task extension) is no longer supported for activity-based
 * tasks.
 */
@Component
public class WorkDefinitionFactory {

    @Autowired ActivityBasedTaskHandler activityBasedTaskHandler;

    /** Suppliers indexed by work definition type name (e.g. {@link RecomputationWorkDefinitionType}). */
    private final Map<QName, WorkDefinitionTypeInfo> byTypeName = new ConcurrentHashMap<>();

    /** Maps e.g. `c:reconciliation` to `c:ReconciliationWorkDefinitionType`. Everything is qualified. */
    private final Map<QName, QName> itemToTypeNameMap = new ConcurrentHashMap<>();

    synchronized public void registerSupplier(
            @NotNull QName typeName,
            @NotNull QName itemName,
            @NotNull WorkDefinitionSupplier supplier) {
        Preconditions.checkArgument(QNameUtil.isQualified(typeName), "Unqualified type name: %s", typeName);
        Preconditions.checkArgument(QNameUtil.isQualified(itemName), "Unqualified item name: %s", itemName);
        stateCheck(
                byTypeName.put(typeName, new WorkDefinitionTypeInfo(supplier, itemName)) == null,
                "Work definition type information already registered for type name %s",
                typeName);
        stateCheck(
                itemToTypeNameMap.put(itemName, typeName) == null,
                "Work definition type already registered for item name %s",
                itemName);
    }

    synchronized public void unregisterSupplier(@NotNull QName typeName) {
        var info = MiscUtil.stateNonNull(
                byTypeName.remove(typeName),
        "No work definition type information for %s", typeName);
        itemToTypeNameMap.remove(info.itemName);
    }

    /** Transforms configuration bean (user-friendly form) into parsed {@link WorkDefinition} object. */
    WorkDefinition getWorkFromBean(@Nullable WorkDefinitionsType definitions, @NotNull ConfigurationItemOrigin origin)
            throws SchemaException, ConfigurationException {
        List<WorkDefinitionBean> beans = WorkDefinitionUtil.getWorkDefinitionBeans(definitions);
        if (beans.isEmpty()) {
            return null;
        } else if (beans.size() > 1) {
            throw new ConfigurationException("Ambiguous definition: " + beans);
        } else {
            return getWorkFromBean(beans.get(0), origin);
        }
    }

    private WorkDefinition getWorkFromBean(@NotNull WorkDefinitionBean definitionBean, @NotNull ConfigurationItemOrigin origin)
            throws SchemaException, ConfigurationException {
        QName typeName = definitionBean.getBeanTypeName();
        var info = MiscUtil.requireNonNull(
                byTypeName.get(typeName),
                () -> new IllegalStateException("No work definition type information for " + typeName));
        return info.supplier()
                .provide(definitionBean, info.itemName());
    }

    /** Note that itemName is always qualified. */
    private record WorkDefinitionTypeInfo(
            @NotNull WorkDefinitionSupplier supplier,
            @NotNull QName itemName) {
    }

    @FunctionalInterface
    public interface WorkDefinitionSupplier {
        WorkDefinition provide(@NotNull WorkDefinitionBean source, @NotNull QName activityName)
                throws SchemaException, ConfigurationException;
    }
}
