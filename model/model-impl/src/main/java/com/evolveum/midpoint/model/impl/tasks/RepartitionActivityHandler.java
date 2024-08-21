/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static java.util.Collections.emptyList;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.BulkActionExecutionOptions;
import com.evolveum.midpoint.model.api.BulkActionExecutionResult;
import com.evolveum.midpoint.model.impl.scripting.NonIterativeScriptingActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.run.*;

import com.evolveum.midpoint.schema.expression.VariablesMap;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
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
public class RepartitionActivityHandler
        extends ModelActivityHandler<RepartitionActivityHandler.MyWorkDefinition,RepartitionActivityHandler> {

    private static final String OP_EXECUTE = RepartitionActivityHandler.class.getName() + ".execute";


    @PostConstruct
    public void register() {
        handlerRegistry.register(
                RepartitioningWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_REPARTITIONING,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                RepartitioningWorkDefinitionType.COMPLEX_TYPE, MyWorkDefinition.class);
    }


    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    public String getIdentifierPrefix() {
        return "repartitioning";
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, RepartitionActivityHandler, ?> createActivityRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, RepartitionActivityHandler> context, @NotNull OperationResult result) {
        return new MyRun(context, "repartitioning");
    }

    static final class MyRun extends
            LocalActivityRun<MyWorkDefinition, RepartitionActivityHandler, AbstractActivityWorkStateType> {

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, RepartitionActivityHandler> context, String shortName) {
            super(context);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .actionsExecutedStatisticsSupported(true)
                    .skipWritingOperationExecutionRecords(false); // because of performance
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult parentResult) throws ActivityRunException, CommonException {
            OperationResult result = parentResult.createSubresult(OP_EXECUTE);
            try {
                getBeans().repositoryService.createPartitionsForExistingData(result);
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
            }
            return standardRunResult(result.getStatus());
        }

    }

    public static class MyWorkDefinition extends AbstractWorkDefinition {

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) {
            return AffectedObjectsInformation.ObjectSet.notSupported(); // not feasibly describable
        }

    }
}
