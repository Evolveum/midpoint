/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.cleanup;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScanActivityExecution;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Scanner that looks for pending operations in the shadows and updates the status.
 *
 * @author Radovan Semancik
 */
@Component
public class ShadowRefreshActivityHandler
        extends ModelActivityHandler<ShadowRefreshActivityHandler.MyWorkDefinition, ShadowRefreshActivityHandler> {

    public static final String LEGACY_HANDLER_URI = ModelPublicConstants.SHADOW_REFRESH_TASK_HANDLER_URI;
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(ShadowRefreshWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                MyWorkDefinition.class, MyWorkDefinition::new, this, ARCHETYPE_OID);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(ShadowRefreshWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                MyWorkDefinition.class);
    }

    @Override
    public @NotNull MyActivityExecution createExecution(
            @NotNull ExecutionInstantiationContext<MyWorkDefinition, ShadowRefreshActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityExecution(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "import";
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return new ActivityStateDefinition<>(
                ScanWorkStateType.COMPLEX_TYPE,
                ActivityStatePersistenceType.PERPETUAL_EXCEPT_STATISTICS // TODO deduplicate with persistentStatistics(false)
        );
    }

    public static class MyActivityExecution
            extends AbstractScanActivityExecution<ShadowType, MyWorkDefinition, ShadowRefreshActivityHandler> {

        MyActivityExecution(@NotNull ExecutionInstantiationContext<MyWorkDefinition, ShadowRefreshActivityHandler> context) {
            super(context, "Shadow refresh");
            setRequiresDirectRepositoryAccess();
        }

        @Override
        public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
            // Non-persistent statistics is a temporary solution for MID-6934.
            // We should decide whether we want to have aggregate statistics for this kind of tasks.
            return super.getDefaultReportingOptions()
                    .enableActionsExecutedStatistics(true)
                    .persistentStatistics(false);
        }

        @Override
        protected ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult opResult) {
            if (ObjectQueryUtil.hasFilter(configuredQuery)) {
                return configuredQuery;
            } else {
                return ObjectQueryUtil.replaceFilter(
                        configuredQuery,
                        getPrismContext().queryFor(ShadowType.class)
                                .exists(ShadowType.F_PENDING_OPERATION)
                                .buildFilter());
            }
        }

        @Override
        protected @NotNull ItemProcessor<PrismObject<ShadowType>> createItemProcessor(OperationResult opResult) {
            return createDefaultItemProcessor(
                    (object, request, workerTask, result) -> {
                        getModelBeans().provisioningService.refreshShadow(object, null, workerTask, result);
                        return true;
                    }
            );
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;

        MyWorkDefinition(WorkDefinitionSource source) {
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacySource = (LegacyWorkDefinitionSource) source;
                objects = ObjectSetUtil.fromLegacySource(legacySource);
            } else {
                ShadowRefreshWorkDefinitionType typedDefinition = (ShadowRefreshWorkDefinitionType)
                        ((WorkDefinitionWrapper.TypedWorkDefinitionWrapper) source).getTypedDefinition();
                objects = typedDefinition.getShadows() != null ?
                        typedDefinition.getShadows() : new ObjectSetType(PrismContext.get());
            }
            ObjectSetUtil.assumeObjectType(objects, ShadowType.COMPLEX_TYPE);
        }

        @Override
        public ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent + 1);
        }
    }
}
