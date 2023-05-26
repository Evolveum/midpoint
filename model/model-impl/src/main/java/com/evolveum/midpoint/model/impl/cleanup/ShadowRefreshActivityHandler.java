/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.cleanup;

import com.evolveum.midpoint.repo.common.activity.run.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.model.impl.tasks.scanner.ScanActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        handlerRegistry.register(
                ShadowRefreshWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ShadowRefreshWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI, MyWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, ShadowRefreshActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, ShadowRefreshActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "shadow-refresh";
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return new ActivityStateDefinition<>(
                ScanWorkStateType.COMPLEX_TYPE,
                ActivityStatePersistenceType.PERPETUAL_EXCEPT_STATISTICS
        );
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    public static final class MyActivityRun
            extends ScanActivityRun<ShadowType, MyWorkDefinition, ShadowRefreshActivityHandler> {

        MyActivityRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, ShadowRefreshActivityHandler> context) {
            super(context, "Shadow refresh");
            setInstanceReady();
        }

        @Override
        public boolean doesRequireDirectRepositoryAccess() {
            return true;
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .actionsExecutedStatisticsSupported(true);
        }

        @Override
        public void beforeRun(OperationResult result) {
            super.beforeRun(result);
            ensureNoPreviewNorDryRun();
        }

        @Override
        public void customizeQuery(SearchSpecification<ShadowType> searchSpecification, OperationResult result) {
            var configuredQuery = searchSpecification.getQuery();
            if (!ObjectQueryUtil.hasFilter(configuredQuery)) {
                searchSpecification.setQuery(
                        ObjectQueryUtil.replaceFilter(
                                configuredQuery,
                                getBeans().prismContext.queryFor(ShadowType.class)
                                        .exists(ShadowType.F_PENDING_OPERATION)
                                        .buildFilter()));
            }
        }

        @Override
        public boolean processItem(@NotNull ShadowType object,
                @NotNull ItemProcessingRequest<ShadowType> request, RunningTask workerTask, OperationResult result)
                throws CommonException, ActivityRunException {
            getModelBeans().provisioningService.refreshShadow(object.asPrismObject(), null, workerTask, result);
            return true;
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
                objects = ObjectSetUtil.fromConfiguration(typedDefinition.getShadows());
            }
            ObjectSetUtil.assumeObjectType(objects, ShadowType.COMPLEX_TYPE);
        }

        @Override
        public ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabel(sb, "objects", objects, indent + 1);
        }
    }
}
