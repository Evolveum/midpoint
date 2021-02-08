/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api.test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

/**
 * DO NOT USE in production code. This is only for testing purposes: provides a no-op implementation of Task interface
 * to be used when task-quartz-impl is not available.
 *
 * TODO move to src/main/test tree.
 *
 * @author lazyman
 */
public class NullTaskImpl implements Task {

    public static final Task INSTANCE = new NullTaskImpl();

    @Override
    public void addDependent(String taskIdentifier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsynchronous() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskExecutionStatus getExecutionStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeWaiting() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeRunnable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInitialExecutionStatus(TaskExecutionStatus value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskPersistenceStatus getPersistenceStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTransient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPersistent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskRecurrence getRecurrenceStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSingle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRecurring() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduleType getSchedule() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getScheduleInterval() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasScheduleInterval() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskBinding getBinding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTightlyBound() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLooselyBound() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBinding(TaskBinding value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBindingImmediate(TaskBinding value, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHandlerUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHandlerUri(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHandlerUriImmediate(String value, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UriStack getOtherHandlersUriStack() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTaskIdentifier() {
        return null;
    }

    @Override
    public PrismObject<? extends FocusType> getOwner() {
        return null;
    }

    @Override
    public void setOwner(PrismObject<? extends FocusType> owner) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getChannel() {
        return null;
    }

    @Override
    public void setChannel(String channelUri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setChannelImmediate(String channelUri, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismObject<UserType> getRequestee() {
        return null;
    }

    @Override
    public void setRequesteeTransient(PrismObject<UserType> user) {

    }

    @Override public LensContextType getModelOperationContext() {
        return null;
    }

    @Override public void setModelOperationContext(LensContextType modelOperationContext) {
    }

    @Override
    public TaskErrorHandlingStrategyType getErrorHandlingStrategy() {
        return null;
    }

    @Override
    public String getOid() {
        return null;
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectReferenceType getObjectRefOrClone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectRef(ObjectReferenceType objectRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectRef(String oid, QName type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectTransient(PrismObject object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getObjectOid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationResult getResult() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResultTransient(OperationResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResult(OperationResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResultImmediate(OperationResult result, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getLastRunStartTimestamp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getLastRunFinishTimestamp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getNextRunStartTime(OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolyStringType getName() {
        return null;
    }

    @Override
    public void setName(PolyStringType value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNameImmediate(PolyStringType value, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> getExtensionItemOrClone(ItemName propertyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <C extends Containerable> void setExtensionContainer(PrismContainer<C> item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensionReference(PrismReference reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensionProperty(PrismProperty<?> property) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addExtensionProperty(PrismProperty<?> property) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void setExtensionPropertyValue(QName propertyName, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void setExtensionPropertyValueTransient(QName propertyName, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Containerable> void setExtensionContainerValue(QName containerName, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensionItem(Item item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyExtension(ItemDelta itemDelta) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getProgress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProgress(Long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProgressImmediate(Long progress, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProgressTransient(Long value) {
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getUpdatedOrClonedTaskObject() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getUpdatedTaskObject() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getClonedTaskObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String debugDump() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String debugDump(int indent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushPendingModifications(OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCategory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeRecurringSimple(int interval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeRecurringCron(String cronLikeSpecification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeSingle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNodeAsObserved() {
        return null;
    }

    @Override
    public OperationResultStatusType getResultStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ThreadStopActionType getThreadStopAction() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isResilient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCategory(String category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addArchetypeInformation(String archetypeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addArchetypeInformationIfMissing(String archetypeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismContainer<? extends ExtensionType> getExtensionOrClone() {
        return null;
    }

    @NotNull
    @Override
    public PrismContainer<? extends ExtensionType> getOrCreateExtension() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismContainer<? extends ExtensionType> getExtensionClone() {
        return null;
    }

    @Override
    public boolean hasExtension() {
        return false;
    }

    @Override
    public <T> PrismProperty<T> getExtensionPropertyOrClone(ItemName propertyName) {
        return null;
    }

    @Override
    public <T> T getExtensionPropertyRealValue(ItemName propertyName) {
        return null;
    }

    @Override
    public <T extends Containerable> T getExtensionContainerRealValueOrClone(ItemName containerName) {
        return null;
    }

    @Override
    public PrismReference getExtensionReferenceOrClone(ItemName name) {
        return null;
    }

    @Override
    public void setDescriptionImmediate(String value, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteExtensionProperty(PrismProperty<?> property) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setThreadStopAction(ThreadStopActionType value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeRecurring(ScheduleType schedule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeSingle(ScheduleType schedule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task createSubtask() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding,
                               Collection<ItemDelta<?,?>> extensionDeltas) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?,?> delta) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void finishHandler(OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public List<Task> listSubtasks(boolean persistentOnly, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> listPrerequisiteTasks(OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startWaitingForTasksImmediate(OperationResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getDependents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDependent(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> listDependents(OperationResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task getParentTask(OperationResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskWaitingReason getWaitingReason() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeWaiting(TaskWaitingReason reason) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeWaiting(TaskWaitingReason reason, TaskUnpauseActionType unpauseAction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getCompletionTimestamp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectRefImmediate(ObjectReferenceType value, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addExtensionReference(PrismReference reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> listSubtasksDeeply(boolean persistentOnly, OperationResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ItemDelta<?,?>> getPendingModifications() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolicyRuleType getPolicyRule() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Long getExpectedTotal() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setExpectedTotal(Long value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setExpectedTotalImmediate(Long value, OperationResult parentResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void recordState(String message) {
    }

    @Override
    public void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName, ProvisioningOperation operation, boolean success, int count, long duration) {
    }

    @Override
    public void recordNotificationOperation(String transportName, boolean success, long duration) {
    }

    @Override
    public void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName, long duration) {
    }

    @Override
    public void recordSynchronizationOperationLegacy(SynchronizationInformation.LegacyCounters originalStateIncrement, SynchronizationInformation.LegacyCounters newStateIncrement) {
    }

    @Override
    public void onSyncItemProcessingStart(@NotNull String processingIdentifier, @Nullable SynchronizationSituationType situationBefore) {
    }

    @Override
    public void onSynchronizationStart(@Nullable String processingIdentifier, @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
    }

    @Override
    public void onSynchronizationExclusion(@Nullable String processingIdentifier, @NotNull SynchronizationExclusionReasonType exclusionReason) {
    }

    @Override
    public void onSynchronizationSituationChange(@Nullable String processingIdentifier, String shadowOid, @Nullable SynchronizationSituationType situation) {
    }

    @Override
    public void onSyncItemProcessingEnd(@NotNull String processingIdentifier,
            SynchronizationInformation.@NotNull Status status) {
    }

    @Override
    public void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
    }

    @Override
    public void resetSynchronizationInformation(SynchronizationInformationType value) {
    }

    @Override
    public void resetIterativeTaskInformation(IterativeTaskInformationType value) {
    }

    @Override
    public void recordIterativeOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid, long started, Throwable exception) {
    }

    @Override
    public void recordIterativeOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid) {
    }

    @Override
    public void recordIterativeOperationEnd(ShadowType shadow, long started, Throwable exception) {
    }

    @Override
    public void recordIterativeOperationStart(ShadowType shadow) {
    }

    @Override
    public void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception) {
    }

    @Override
    public void resetActionsExecutedInformation(ActionsExecutedInformationType value) {
    }

    @Override
    public void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception) {
    }

    @Override
    public <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> objectOld, Class<T> objectTypeClass, String oid, ChangeType delete, String channel, Throwable o) {
    }

    @Override
    public void markObjectActionExecutedBoundary() {
    }

    @Override
    public OperationStatsType getStoredOperationStats() {
        return null;
    }

    @Override public void modify(ItemDelta<?, ?> delta) {
    }

    @Override public void modify(Collection<ItemDelta<?, ?>> deltas) {

    }

    @Override public void modifyAndFlush(ItemDelta<?, ?> delta, OperationResult parentResult) {
    }

    @Override
    public TaskExecutionConstraintsType getExecutionConstraints() {
        return null;
    }

    @Override
    public String getGroup() {
        return null;
    }

    @NotNull
    @Override
    public Collection<String> getGroups() {
        return emptySet();
    }

    @NotNull
    @Override
    public Map<String, Integer> getGroupsWithLimits() {
        return emptyMap();
    }

    @NotNull
    @Override
    public List<String> getLastFailures() {
        return Collections.emptyList();
    }

    @Override
    public void close(OperationResult taskResult, boolean saveState, OperationResult parentResult) {
    }

    @Override
    public TaskWorkManagementType getWorkManagement() {
        return null;
    }

    @Override
    public TaskWorkStateType getWorkState() {
        return null;
    }

    @Override
    public TaskUnpauseActionType getUnpauseAction() {
        return null;
    }

    @Override
    public TaskExecutionStatusType getStateBeforeSuspend() {
        return null;
    }

    @Override
    public boolean isPartitionedMaster() {
        return false;
    }

    @Override
    public TaskKindType getKind() {
        return null;
    }

    @Override
    public String getExecutionGroup() {
        return null;
    }

    @Override
    public OperationStatsType getAggregatedLiveOperationStats() {
        return null;
    }

    @Override
    public ObjectReferenceType getSelfReference() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public Collection<? extends TriggerType> getTriggers() {
        return null;
    }

    @Override
    public Collection<? extends AssignmentType> getAssignments() {
        return null;
    }

    @Override
    public Collection<Task> getPathToRootTask(OperationResult parentResult) throws SchemaException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTaskTreeId(OperationResult result) throws SchemaException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectReferenceType getOwnerRef() {
        return null;
    }

    @NotNull
    @Override
    public Collection<String> getCachingProfiles() {
        return emptySet();
    }

    @Override
    public String getOperationResultHandlingStrategyName() {
        return null;
    }

    @Override
    public void setExecutionConstraints(TaskExecutionConstraintsType value) {
    }

    @Override
    public TaskExecutionEnvironmentType getExecutionEnvironment() {
        return null;
    }

    @Override
    public void setExecutionEnvironment(TaskExecutionEnvironmentType value) {
    }

    @Override
    public void setExecutionEnvironmentTransient(TaskExecutionEnvironmentType value) {
    }

    @Override
    public boolean isScavenger() {
        return false;
    }

    @NotNull
    @Override
    public Collection<TracingRootType> getTracingRequestedFor() {
        return emptySet();
    }

    @Override
    public void addTracingRequest(TracingRootType point) {
    }

    @Override
    public void removeTracingRequests() {

    }

    @Override
    public TracingProfileType getTracingProfile() {
        return null;
    }

    @Override
    public void setTracingProfile(TracingProfileType tracingProfile) {
    }
}
