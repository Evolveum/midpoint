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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

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
        addTypedParameters(values, definitions.getRoleAnalysisClustering());
        addTypedParameters(values, definitions.getRoleAnalysisPatternDetection());
        addTypedParameters(values, definitions.getShadowReclassification());
        addTypedParameters(values, definitions.getCertificationRemediation());
        addTypedParameters(values, definitions.getCertificationStartCampaign());
        addTypedParameters(values, definitions.getCertificationOpenNextStage());
        addTypedParameters(values, definitions.getCertificationCloseCurrentStage());
        addTypedParameters(values, definitions.getCertificationReiterateCampaign());
        addTypedParameters(values, definitions.getRepartitioning());
        addTypedParameters(values, definitions.getFocusTypeSuggestion());
        addTypedParameters(values, definitions.getObjectTypesSuggestion());
        addTypedParameters(values, definitions.getCorrelationSuggestion());
        addTypedParameters(values, definitions.getMappingsSuggestion());

        addTypedParameters(values, definitions.getCreateConnector());
        addTypedParameters(values, definitions.getDiscoverDocumentation());
        addTypedParameters(values, definitions.getProcessDocumentation());
        addTypedParameters(values, definitions.getDiscoverGlobalInformation());
        addTypedParameters(values, definitions.getDiscoverObjectClassInformation());
        addTypedParameters(values, definitions.getGenerateConnectorArtifact());

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

    /**
     * Replaces the query in "object set" in given work definition.
     *
     * Only selected activities are supported.
     *
     * Preliminary implementation.
     */
    public static void replaceObjectSetQuery(@NotNull WorkDefinitionsType def, @Nullable QueryType query) {
        if (updateQuery(def.getIterativeScripting(), query)) {
            return;
        }
        if (updateQuery(def.getIterativeChangeExecution(), query)) {
            return;
        }
        if (updateQuery(def.getRecomputation(), query)) {
            return;
        }
        if (updateQuery(def.getObjectIntegrityCheck(), query)) {
            return;
        }
        if (updateQuery(def.getReindexing(), query)) {
            return;
        }
        if (updateQuery(def.getTriggerScan(), query)) {
            return;
        }
        if (updateQuery(def.getFocusValidityScan(), query)) {
            return;
        }
        if (updateQuery(def.getDeletion(), query)) {
            return;
        }
        throw new IllegalArgumentException("The query cannot be replaced in the work definition: " + def);
    }

    private static <D extends ObjectSetBasedWorkDefinitionType> boolean updateQuery(
            D workDef, @Nullable QueryType query) {
        return updateQuery(
                workDef,
                ObjectSetBasedWorkDefinitionType::getObjects,
                ObjectSetBasedWorkDefinitionType::setObjects,
                query);
    }

    private static <D extends AbstractWorkDefinitionType> boolean updateQuery(
            D workDef,
            Function<D, ObjectSetType> objectsGetter,
            BiConsumer<D, ObjectSetType> objectsSetter,
            @Nullable QueryType query) {
        if (workDef == null) {
            return false;
        }
        ObjectSetType objects = objectsGetter.apply(workDef);
        if (objects == null) {
            objects = new ObjectSetType()
                    .query(query);
            objectsSetter.accept(workDef, objects);
        } else {
            objects.setQuery(query);
        }
        return true;
    }
}
