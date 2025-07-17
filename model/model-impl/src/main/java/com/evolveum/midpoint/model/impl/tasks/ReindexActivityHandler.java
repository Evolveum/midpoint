/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static java.util.Collections.emptyList;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity handler for reindexing activity.
 * It simply executes empty modification delta on each repository object found.
 *
 * TODO implement also for sub-objects, namely certification cases.
 */
@Component
public class ReindexActivityHandler
        extends SimpleActivityHandler<
            ObjectType,
            ReindexActivityHandler.MyWorkDefinition,
            ReindexActivityHandler> {

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return ReindexingWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull QName getWorkDefinitionItemName() {
        return WorkDefinitionsType.F_REINDEXING;
    }

    @Override
    protected @NotNull Class<MyWorkDefinition> getWorkDefinitionClass() {
        return MyWorkDefinition.class;
    }

    @Override
    protected @NotNull WorkDefinitionSupplier getWorkDefinitionSupplier() {
        return MyWorkDefinition::new;
    }

    @Override
    protected @NotNull ExecutionSupplier<ObjectType, MyWorkDefinition, ReindexActivityHandler> getExecutionSupplier() {
        return MyRun::new;
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    protected @NotNull String getShortName() {
        return "Reindexing";
    }

    @Override
    public String getIdentifierPrefix() {
        return "reindexing";
    }

    static final class MyRun extends
            SearchBasedActivityRun<ObjectType, MyWorkDefinition, ReindexActivityHandler, AbstractActivityWorkStateType> {

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, ReindexActivityHandler> context, String shortName) {
            super(context, shortName);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .actionsExecutedStatisticsSupported(true)
                    .skipWritingOperationExecutionRecords(false); // because of performance
        }

        @Override
        public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
            if (!super.beforeRun(result)) {
                return false;
            }
            getActivityHandler().securityEnforcer.authorizeAll(getRunningTask(), result);
            ensureNoPreviewNorDryRun();
            return true;
        }

        @Override
        public boolean processItem(@NotNull ObjectType object,
                @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult result)
                throws CommonException {
            reindexObject(object, result);
            return true;
        }

        private void reindexObject(ObjectType object, OperationResult result) throws CommonException {
            getBeans().repositoryService.modifyObject(object.getClass(), object.getOid(), emptyList(),
                    RepoModifyOptions.createForceReindex(), result);
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
            var typedDefinition = (ReindexingWorkDefinitionType) info.getBean();
            objects = ObjectSetUtil.emptyIfNull(typedDefinition.getObjects());
        }

        @Override
        public @NotNull ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabel(sb, "objects", objects, indent+1);
        }
    }
}
