/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.GenericProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Executes a set of change requests, each consisting of a set of deltas (presenting a single model operation).
 *
 * This activity is used to execute changes in background.
 */
@Component
public class ExplicitChangeExecutionActivityHandler
        extends ModelActivityHandler<
        ExplicitChangeExecutionActivityHandler.MyWorkDefinition,
        ExplicitChangeExecutionActivityHandler> {

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ExplicitChangeExecutionWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_EXPLICIT_CHANGE_EXECUTION,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ExplicitChangeExecutionWorkDefinitionType.COMPLEX_TYPE, MyWorkDefinition.class);
    }

    @Override
    public String getIdentifierPrefix() {
        return "explicit-change-execution";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, ExplicitChangeExecutionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, ExplicitChangeExecutionActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    final static class MyActivityRun
            extends PlainIterativeActivityRun<
            ChangeExecutionRequest,
            MyWorkDefinition,
            ExplicitChangeExecutionActivityHandler,
            AbstractActivityWorkStateType> {

        MyActivityRun(
                @NotNull ActivityRunInstantiationContext<MyWorkDefinition, ExplicitChangeExecutionActivityHandler> context) {
            super(context, "Explicit change execution");
            setInstanceReady();
        }

        @Override
        public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
            if (!super.beforeRun(result)) {
                return false;
            }
            ensureNoDryRun();
            return true;
        }

        @Override
        public boolean processItem(
                @NotNull ItemProcessingRequest<ChangeExecutionRequest> processingRequest,
                @NotNull RunningTask workerTask,
                @NotNull OperationResult result) throws CommonException {

            ChangeExecutionRequest execRequest = processingRequest.getItem();
            if (!execRequest.isEmpty()) {
                getActivityHandler().beans.modelService.executeChanges(
                        execRequest.getParsedDeltas(),
                        execRequest.executionOptions,
                        getRunningTask(),
                        result);
            } else {
                result.recordNotApplicable("No deltas to execute");
            }
            return true;
        }

        @Override
        public void iterateOverItemsInBucket(OperationResult result) {
            for (ChangeExecutionRequest request : getWorkDefinition().requests) {
                boolean canContinue = coordinator.submit(
                        new ChangeProcessingRequest(request, this),
                        result);
                if (!canContinue) {
                    break;
                }
            }
        }

        @Override
        public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
            return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition {

        @NotNull private final List<ChangeExecutionRequest> requests = new ArrayList<>();

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (ExplicitChangeExecutionWorkDefinitionType) info.getBean();
            Collection<ObjectDeltaType> rootDeltas = typedDefinition.getDelta();
            ModelExecuteOptions rootOptions =
                    ModelExecuteOptions.fromModelExecutionOptionsType(typedDefinition.getExecutionOptions());
            List<ChangeExecutionRequestType> rootRequests = typedDefinition.getRequest();

            boolean singleRequest = !rootDeltas.isEmpty() || rootOptions != null;
            boolean multipleRequests = !rootRequests.isEmpty();

            configCheck(!singleRequest || !multipleRequests,
                    "You must specify either 'single-request' and 'multiple-requests' "
                            + "form of configuration, not both");
            if (singleRequest) {
                requests.add(
                        new ChangeExecutionRequest(1, null, rootDeltas, rootOptions));
            } else {
                AtomicInteger number = new AtomicInteger(1);
                for (ChangeExecutionRequestType requestBean : rootRequests) {
                    requests.add(
                            new ChangeExecutionRequest(
                                    number.getAndIncrement(),
                                    requestBean.getName(),
                                    requestBean.getDelta(),
                                    ModelExecuteOptions.fromModelExecutionOptionsType(requestBean.getExecutionOptions())));
                }
            }
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) {
            return AffectedObjectsInformation.ObjectSet.notSupported(); // not easily determinable
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabel(sb, "requests", requests, indent+1);
        }
    }

    /** Semi-parsed and ordered change execution request (deltas + options). */
    record ChangeExecutionRequest(
            int number,
            String name,
            @NotNull Collection<ObjectDeltaType> deltas,
            ModelExecuteOptions executionOptions) implements Serializable {

        private Collection<ObjectDelta<? extends ObjectType>> getParsedDeltas() throws SchemaException {
            List<ObjectDelta<? extends ObjectType>> parsedDeltas = new ArrayList<>();
            for (ObjectDeltaType deltaBean : deltas) {
                parsedDeltas.add(
                        DeltaConvertor.createObjectDelta(deltaBean));
            }
            return parsedDeltas;
        }

        public boolean isEmpty() {
            return deltas.isEmpty();
        }

        public @NotNull String getIterationItemName() {
            return Objects.requireNonNullElseGet(
                    name,
                    () -> "#" + number); // not providing any text here - because of i18n
        }
    }

    /**
     * Wrapper for {@link ChangeExecutionRequest} objects. (This is needed for the activity framework to process them.)
     */
    private static class ChangeProcessingRequest
            extends GenericProcessingRequest<ChangeExecutionRequest> {

        ChangeProcessingRequest(@NotNull ChangeExecutionRequest item,
                @NotNull IterativeActivityRun<ChangeExecutionRequest, ?, ?, ?> activityRun) {
            super(item.number, item, activityRun);
        }

        @Override
        public @NotNull IterationItemInformation getIterationItemInformation() {
            // Here we may attempt to provide object OID - but we'd need to analyze the deltas
            return new IterationItemInformation(
                    item.getIterationItemName(),
                    null,
                    null,
                    null);
        }
    }
}
