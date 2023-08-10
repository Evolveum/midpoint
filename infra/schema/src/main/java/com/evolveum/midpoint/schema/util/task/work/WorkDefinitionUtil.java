/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorkDefinitionUtil {

    public static @NotNull List<WorkDefinitionBean> getWorkDefinitionBeans(@Nullable WorkDefinitionsType definitions) {
        List<WorkDefinitionBean> values = new ArrayList<>();
        if (definitions == null) {
            return values;
        }
        addTypedParameters(values, definitions.getRecomputation());
        addTypedParameters(values, definitions.getImport());
        addTypedParameters(values, definitions.getReconciliation());
        addTypedParameters(values, definitions.getLiveSynchronization());
        addTypedParameters(values, definitions.getAsynchronousUpdate());
        addTypedParameters(values, definitions.getCleanup());
        addTypedParameters(values, definitions.getDeletion());
        addTypedParameters(values, definitions.getReportExport());
        addTypedParameters(values, definitions.getReportImport());
        addTypedParameters(values, definitions.getDistributedReportExport());
        addTypedParameters(values, definitions.getIterativeScripting());
        addTypedParameters(values, definitions.getNonIterativeScripting());
        addTypedParameters(values, definitions.getFocusValidityScan());
        addTypedParameters(values, definitions.getTriggerScan());
        addTypedParameters(values, definitions.getShadowRefresh());
        addTypedParameters(values, definitions.getIterativeChangeExecution());
        addTypedParameters(values, definitions.getExplicitChangeExecution());
        addTypedParameters(values, definitions.getNonIterativeChangeExecution()); // legacy
        addTypedParameters(values, definitions.getReindexing());
        addTypedParameters(values, definitions.getShadowCleanup());
        addTypedParameters(values, definitions.getObjectIntegrityCheck());
        addTypedParameters(values, definitions.getShadowIntegrityCheck());
        addTypedParameters(values, definitions.getActivityAutoScaling());
        addTypedParameters(values, definitions.getNoOp());
        addTypedParameters(values, definitions.getPropagation());
        addTypedParameters(values, definitions.getMultiPropagation());
        addTypedParameters(values, definitions.getRoleMembershipManagement());

        addUntypedParameters(values, definitions.getExtension());
        return values;
    }

    private static void addUntypedParameters(List<WorkDefinitionBean> values, ExtensionType extension) {
        if (extension == null) {
            return;
        }
        SchemaRegistry schemaRegistry = PrismContext.get().getSchemaRegistry();
        PrismContainerValue<?> pcv = extension.asPrismContainerValue();
        for (Item<?, ?> item : pcv.getItems()) {
            ItemDefinition<?> definition = item.getDefinition();
            if (definition != null &&
                    schemaRegistry.isAssignableFromGeneral(AbstractWorkDefinitionType.COMPLEX_TYPE, definition.getTypeName())) {
                for (PrismValue value : item.getValues()) {
                    if (value instanceof PrismContainerValue<?>) {
                        values.add(new WorkDefinitionBean.Untyped((PrismContainerValue<?>) value));
                    }
                }
            }
        }
    }

    private static void addTypedParameters(List<WorkDefinitionBean> values, AbstractWorkDefinitionType realValue) {
        if (realValue != null) {
            values.add(new WorkDefinitionBean.Typed(realValue));
        }
    }

    public static Collection<QName> getWorkDefinitionTypeNames(WorkDefinitionsType definitions) {
        List<WorkDefinitionBean> workDefinitionBeanCollection = getWorkDefinitionBeans(definitions);
        return workDefinitionBeanCollection.stream()
                .map(WorkDefinitionBean::getBeanTypeName)
                .collect(Collectors.toSet());
    }
}
