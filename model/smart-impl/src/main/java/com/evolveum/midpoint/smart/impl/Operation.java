package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityProgress;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.activity.run.state.VirtualActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDelineation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

/** A smart integration operation, knowing a resource and an object class on it. */
class Operation {

    final ResourceType resource;
    private final ResourceSchema resourceSchema;
    private final ResourceObjectClassDefinition objectClassDefinition;
    final ServiceAdapter serviceAdapter;

    /** State of the activity that is executing this operation, if the operation runs in the background. */
    @Nullable private final CurrentActivityState<?> activityState;

    final Task task;
    final SmartIntegrationBeans b = SmartIntegrationBeans.get();
    final StateHolderFactory stateHolderFactory = new StateHolderFactory();

    public Operation(
            ResourceType resource,
            ResourceSchema resourceSchema,
            ResourceObjectClassDefinition objectClassDefinition,
            ServiceAdapter serviceAdapter,
            @Nullable CurrentActivityState<?> activityState,
            Task task) {
        this.resource = resource;
        this.resourceSchema = resourceSchema;
        this.objectClassDefinition = objectClassDefinition;
        this.serviceAdapter = serviceAdapter;
        this.activityState = activityState;
        this.task = task;
    }

    static Operation init(
            ServiceClient serviceClient, String resourceOid, QName objectClassName, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var serviceAdapter = ServiceAdapter.create(serviceClient);
        var resource = b().modelService
                .getObject(ResourceType.class, resourceOid, null, task, result)
                .asObjectable();
        var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
        var objectClassDefinition = resourceSchema.findObjectClassDefinitionRequired(objectClassName);
        return new Operation(resource, resourceSchema, objectClassDefinition, serviceAdapter, null, task);
    }

    static SmartIntegrationBeans b() {
        return SmartIntegrationBeans.get();
    }

    ObjectTypesSuggestionType suggestObjectTypes(ShadowObjectClassStatisticsType statistics) throws SchemaException {
        return serviceAdapter.suggestObjectTypes(objectClassDefinition, statistics, resourceSchema, resource);
    }

    FocusTypeSuggestionType suggestFocusType(ResourceObjectTypeDefinitionType typeDefBean)
            throws SchemaException, ConfigurationException {
        var typeIdentification = ResourceObjectTypeIdentification.of(typeDefBean);
        var delineation = ResourceObjectTypeDelineation.of(
                typeDefBean.getDelineation(), objectClassDefinition.getObjectClassName(), List.of(), objectClassDefinition);
        throw new UnsupportedOperationException("TODO: implement focus type suggestion");
//        return serviceAdapter.suggestFocusType(
//                typeIdentification,
//                objectClassDefinition,
//                typeDefinition.getDelineation(),
//                resource);
    }

    void checkIfCanRun() throws ActivityInterruptedException {
        if (!canRun()) {
            throw new ActivityInterruptedException();
        }
    }

    boolean canRun() {
        return !(task instanceof RunningTask runningTask) || runningTask.canRun();
    }

    /** Creates new {@link StateHolder} for the virtual child activity that is an externally-visible part of this operation. */
    class StateHolderFactory {
        int displayOrder = 1;

        public StateHolder create(String identifier, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
            var holder = new StateHolder(createOrFindChildActivityState(identifier, result));
            holder.setDisplayOrder(displayOrder++);
            holder.recordRealizationStart();
            return holder;
        }

        private @Nullable VirtualActivityState<?> createOrFindChildActivityState(String childIdentifier, OperationResult result) {
            if (activityState != null) {
                var state = activityState.createOrFindVirtualChildActivityState(childIdentifier);
                try {
                    state.initialize(result);
                } catch (ActivityRunException e) {
                    throw SystemException.unexpected(e);
                }
                return state;
            } else {
                return null;
            }
        }
    }

    /**
     * Holds a {@link VirtualActivityState} for a virtual child activity that is an externally-visible part of this operation.
     * It is used to record progress and statistics of the operation, even if there is no real "activity run" for that part.
     *
     * It is a special object because the whole operation may be executed in the foreground, in which case there are no
     * activities and no activity states. So, to make client code simpler, we use this class to hide these details.
     */
    static class StateHolder {
        private long lastUpdateTimestamp;
        private static final long UPDATE_INTERVAL = 3000;

        @Nullable VirtualActivityState<?> activityState;

        StateHolder(@Nullable VirtualActivityState<?> activityState) {
            this.activityState = activityState;
        }

        public void setDisplayOrder(int value) {
            if (activityState == null) {
                return;
            }
            try {
                activityState.setDisplayOrder(value);
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        void recordException(Throwable t) {
            if (activityState == null) {
                return;
            }
            try {
                activityState.recordException(t);
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        public void close(OperationResult result) {
            if (activityState == null) {
                return;
            }
            try {
                activityState.updateProgressAndStatisticsNoCommit();
                if (activityState.getResultStatus() == null) {
                    activityState.setResultStatus(OperationResultStatus.SUCCESS);
                }
                if (activityState.getResultStatus().isConsideredSuccess()) {
                    activityState.markComplete(
                            OperationResultStatus.SUCCESS, SmartIntegrationBeans.get().clock.currentTimeMillis());
                } else {
                    // Unlike traditional iterative activities, an error means that the activity is not complete.
                    // We may revise this policy later.
                }
                activityState.flushPendingTaskModificationsChecked(result);
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        void setExpectedProgress(int value) {
            if (activityState == null) {
                return;
            }
            activityState.getLiveProgress().setExpectedTotal(value);
            try {
                activityState.updateProgressAndStatisticsNoCommit();
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        void incrementProgress(OperationResult result) {
            if (activityState == null) {
                return;
            }
            activityState.getLiveProgress().increment(
                    new QualifiedItemProcessingOutcomeType()
                            .outcome(ItemProcessingOutcomeType.SUCCESS),
                    ActivityProgress.Counters.COMMITTED);
            try {
                activityState.updateProgressAndStatisticsNoCommit();
                flushIfNeeded(result);
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        com.evolveum.midpoint.schema.statistics.Operation recordProcessingStart(String itemName) {
            if (activityState == null) {
                return null;
            }
            try {
                var op = activityState.getLiveStatistics().getLiveItemProcessing().recordOperationStart(
                        new IterativeOperationStartInfo(
                                new IterationItemInformation(
                                        itemName, null, null, null)));
                activityState.updateProgressAndStatisticsNoCommit();
                return op;
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        void recordProcessingEnd(com.evolveum.midpoint.schema.statistics.Operation op) {
            if (op == null || activityState == null) {
                return;
            }
            op.succeeded();
            try {
                activityState.getLiveProgress().increment(
                        new QualifiedItemProcessingOutcomeType()
                                .outcome(ItemProcessingOutcomeType.SUCCESS),
                        ActivityProgress.Counters.COMMITTED);
                activityState.updateProgressAndStatisticsNoCommit();
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        void recordRealizationStart() {
            if (activityState == null) {
                return;
            }
            try {
                activityState.setRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL);
                activityState.recordRealizationStart(SmartIntegrationBeans.get().clock.currentTimeXMLGregorianCalendar());
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        void flush(OperationResult result) {
            if (activityState == null) {
                return;
            }
            try {
                activityState.updateProgressAndStatisticsNoCommit();
                activityState.flushPendingTaskModificationsChecked(result);
                lastUpdateTimestamp = System.currentTimeMillis();
            } catch (ActivityRunException e) {
                throw SystemException.unexpected(e);
            }
        }

        void flushIfNeeded(OperationResult result) {
            if (activityState == null) {
                return;
            }
            if (System.currentTimeMillis() - lastUpdateTimestamp > UPDATE_INTERVAL) {
                flush(result);
            }
        }
    }
}
