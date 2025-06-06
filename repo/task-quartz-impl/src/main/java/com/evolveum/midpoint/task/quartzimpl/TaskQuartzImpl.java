/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static com.evolveum.midpoint.util.MiscUtil.*;

import static java.util.Collections.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.statistics.Statistics;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Implementation of a Task.
 *
 * Responsibilities:
 *
 * 1. Maintains the task content:
 *
 *   - prism object (TaskType)
 *   - separately stored live and extra parts: operation result, statistics, tracing
 *   - pending changes + quartz synchronization related flags
 *   - other (like requestee)
 *
 * 2. Synchronizes access to this content in multi-threaded environment (see below).
 *
 * 3. DOES NOT implement state transitions nor retrieval methods. These are delegated to the other (helper) classes.
 *
 * 4. Custom logic present (i.e. exceptions to the above point):
 *     - instantiation,
 *     - {@link #refresh(OperationResult)},
 *     - subtasks handling,
 *     - path-to-root computing.
 *
 * ---
 * A few notes about concurrency:
 *
 * This class is a frequent source of concurrency-related issues: see e.g. MID-3954, MID-4088, MID-5111, MID-5113,
 * MID-5131, MID-5135. Therefore we decided to provide more explicit synchronization to it starting in midPoint 4.0.
 *
 * There are the following synchronization objects - please use *in this order*:
 *
 * 1. quartzAccess: synchronizes execution of Quartz-related actions
 * 2. pendingModification: synchronizes modifications queue
 * 3. prismAccess: synchronizes access to the prism object (that is not thread-safe by itself; and that caused all mentioned issues)
 *
 * Note that prismAccess could be replaced by taskPrism object; but unfortunately taskPrism is changed in updateTaskInstance().
 * Quartz and Pending modification synchronization is perhaps not so useful, because we do not expect two threads to modify
 * a task at the same time. But let's play it safe.
 *
 * prismAccess by itself is NOT sufficient, though. TODO explain
 *
 * TODO notes for developers (do not nest synchronization blocks)
 *
 * TODO what about the situation where a task tries to close/suspend itself and (at the same time) task manager tries to do the same?
 *  Maybe the task manager should act on a clone of the task
 */
public class TaskQuartzImpl implements Task {

    private static final int TIGHT_BINDING_INTERVAL_LIMIT = 10;

    @NotNull private TaskExecutionMode executionMode = TaskExecutionMode.PRODUCTION;

    /** Synchronizes Quartz-related operations. */
    private final Object quartzAccess = new Object();

    /** Synchronizes access to the task prism object. */
    private final Object prismAccess = new Object();

    /** Task prism object: here all relevant data is stored. Except for the most dynamic ones (see below). */
    private PrismObject<TaskType> taskPrism;

    /** Various statistics related to the task execution. Too dynamic and thread sensitive to be stored in taskPrism. */
    @NotNull protected final Statistics statistics;

    /**
     * An object in activity execution that should collect statistics about synchronization. (If we run in an activity.)
     */
    private SynchronizationStatisticsCollector synchronizationStatisticsCollector;

    /**
     * An object in activity execution that should collect information about actions executed. (If we run in an activity.)
     */
    private ActionsExecutedCollector actionsExecutedCollector;

    /**
     * Task result is stored here as well as in task prism.
     *
     * This one is the live value of this task's result. All operations working with this task
     * should work with this value. This value is explicitly updated from the value in prism
     * when fetching task from repo (or creating anew).
     *
     * The value in taskPrism is updated when necessary, e.g. when getting taskPrism
     * (for example, used when persisting task to repo), etc, see the code.
     *
     * Note that this means that we SHOULD NOT get operation result from the prism - we should
     * use task.getResult() instead!
     *
     * This result can be null if the task was created from taskPrism retrieved from repo without fetching the result.
     * Such tasks should NOT be used to execute handlers, as the result information would be lost.
     *
     * Basically, the result should be initialized only when a new transient task is created. It should be then persisted
     * into the repository. Tasks that are to execute handlers should be fetched from the repository with their results.
     *
     * FIXME what about synchronization?
     */
    protected OperationResult taskResult;

    /**
     * True if the task result is present in the bean but not complete. Applicable only if taskResult is null.
     */
    private boolean taskResultIncomplete;

    private PrismObject<UserType> requestee; // temporary information

    /** Useful beans implementing task management operations. */
    @NotNull protected final TaskBeans beans;

    /** Some operations are still available only through the task manager itself. */
    @NotNull protected final TaskManagerQuartzImpl taskManager;

    /**
     * Whether to recreate quartz trigger on next flushPendingModifications and/or synchronizeWithQuartz.
     */
    private boolean recreateQuartzTrigger;

    /**
     * Modifications that are to be written to repository on next suitable occasion.
     *
     * *BEWARE:* Although using synchronized list we still have to synchronize on pendingModifications while iterating over it.
     */
    @NotNull
    private final List<ItemDelta<?, ?>> pendingModifications = Collections.synchronizedList(new ArrayList<>());

    /**
     * Points where tracing is requested (for this task).
     */
    @Experimental
    private final Set<TracingRootType> tracingRequestedFor = new HashSet<>();

    /**
     * The profile to be used for tracing - it is copied into operation result at specified tracing point(s).
     */
    @Experimental
    private TracingProfileType tracingProfile;

    /** ConnId operations listeners. */
    @Experimental
    @NotNull private final Set<ConnIdOperationsListener> connIdOperationsListeners = ConcurrentHashMap.newKeySet();

    /** TODO */
    private SimulationTransaction simulationTransaction;

    private static final Trace LOGGER = TraceManager.getTrace(TaskQuartzImpl.class);

    //region Constructors

    TaskQuartzImpl(@NotNull TaskManagerQuartzImpl taskManager, @NotNull PrismObject<TaskType> taskPrism) {
        this.taskManager = taskManager;
        this.beans = taskManager.getBeans();
        this.taskPrism = taskPrism;
        statistics = new Statistics();
        setDefaults();
        updateTaskResult();
    }

    /**
     * Creates a new task instance i.e. from scratch.
     *
     * @param operationName if null, default op. name will be used
     */
    public static TaskQuartzImpl createNew(@NotNull TaskManagerQuartzImpl taskManager, String operationName) {
        TaskType taskBean = new TaskType()
                .taskIdentifier(taskManager.getBeans().taskPersister.generateTaskIdentifier().toString())
                .executionState(TaskExecutionStateType.RUNNABLE)
                .schedulingState(TaskSchedulingStateType.READY)
                .progress(0L)
                .result(createTaskResult(operationName));
        return new TaskQuartzImpl(taskManager, taskBean.asPrismObject());
    }

    /**
     * Creates a new task instance from provided task prism object.
     *
     * NOTE: if the result in prism is null, task result will be kept null as well
     * (meaning it was not fetched from the repository).
     */
    public static TaskQuartzImpl createFromPrismObject(@NotNull TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskObject) {
        return new TaskQuartzImpl(taskManager, taskObject);
    }

    private void setDefaults() {
        if (getBinding() == null) {
            setBindingTransient(bindingFromSchedule(getSchedule()));
        }
    }

    private static OperationResultType createTaskResult(String operationName) {
        if (operationName == null) {
            return createUnnamedTaskResult().createOperationResultType();
        } else {
            return new OperationResult(operationName).createOperationResultType();
        }
    }

    @NotNull
    public static OperationResult createUnnamedTaskResult() {
        return new OperationResult(DOT_INTERFACE + "run");
    }
    //endregion

    //region Result handling
    private void updateTaskResult() {
        synchronized (prismAccess) {
            PrismProperty<OperationResultType> resultInPrism = taskPrism.findProperty(TaskType.F_RESULT);
            if (resultInPrism != null && !resultInPrism.isEmpty()) {
                taskResult = OperationResult.createOperationResult(resultInPrism.getRealValue());
            } else {
                taskResult = null;
                taskResultIncomplete = resultInPrism != null && resultInPrism.isIncomplete();
            }
        }
    }

    private void updateTaskPrismResult() {
        // TODO in fact, we should synchronize on taskResult here - and synchronization
        //  on prismAccess should be done by callers (where applicable)
        synchronized (prismAccess) {
            if (taskResult != null) {
                clearPrismResultIncompleteFlag();
                taskPrism.asObjectable().setResult(taskResult.createOperationResultType());
                taskPrism.asObjectable().setResultStatus(taskResult.getStatus().createStatusType());
            } else {
                taskPrism.asObjectable().setResult(null);
                if (taskResultIncomplete) {
                    // We must not clear result status if the result is present but not known.
                } else {
                    taskPrism.asObjectable().setResultStatus(null);
                }
            }
        }
    }
    //endregion

    //region Main getters and setters

    private boolean isLiveRunningInstance() {
        return this instanceof RunningTask;
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getRawTaskObjectClonedIfNecessary() {
        if (isLiveRunningInstance()) {
            return getRawTaskObjectClone();
        } else {
            return taskPrism;
        }
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getRawTaskObjectClone() {
        synchronized (prismAccess) {
            return taskPrism.clone();
        }
    }


    /**
     * Returns the backing task prism object. Not supported for running task instances.
     */
    public PrismObject<TaskType> getRawTaskObject() {
        if (isLiveRunningInstance()) {
            throw new IllegalStateException("Cannot get task object from live running task instance");
        } else {
            return taskPrism;
        }
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getUpdatedTaskObject() {
        if (isLiveRunningInstance()) {
            throw new IllegalStateException("Cannot get task object from live running task instance");
        } else {
            updateTaskPrismResult();
            return taskPrism;
        }
    }

    TaskQuartzImpl cloneAsStaticTask() {
        return TaskQuartzImpl.createFromPrismObject(taskManager, getRawTaskObjectClone());
    }

    public boolean isRecreateQuartzTrigger() {
        return recreateQuartzTrigger;
    }

    public void setRecreateQuartzTrigger(boolean recreateQuartzTrigger) {
        this.recreateQuartzTrigger = recreateQuartzTrigger;
    }
    //endregion

    //region Pending modifications
    void addPendingModification(ItemDelta<?, ?> delta) {
        if (delta != null) {
            synchronized (pendingModifications) {
                ItemDeltaCollectionsUtil.merge(pendingModifications, delta);
            }
        }
    }

    @Override
    public void modify(@NotNull ItemDelta<?, ?> delta) throws SchemaException {
        LOGGER.debug("Applying {} to {}", delta, this);
        if (isPersistent()) {
            // If we not cloned the delta, another thread might attempt to modify it, failing with CME
            // (conflicting with delta.applyTo below). Note that deltas are not thread-safe. See MID-7264.
            // An alternative would be to guard this code by prismAccess, along with the delta application below.
            // But it would make delta accessible to a different thread nevertheless. The caller should be cautious
            // not to access the delta after the call. We don't want to impose this restriction to the callers.
            addPendingModification(delta.clone());
        }
        synchronized (prismAccess) {
            delta.applyTo(taskPrism);
        }
    }

    @Override
    public void flushPendingModifications(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        if (isTransient()) {
            synchronized (pendingModifications) {
                pendingModifications.clear();
            }
            return;
        }
        List<ItemDelta<?, ?>> currentPendingModification;
        synchronized (pendingModifications) {
            currentPendingModification = new ArrayList<>(pendingModifications);
            pendingModifications.clear();
        }
        modifyRepository(currentPendingModification, result);
        if (recreateQuartzTrigger) { // just in case there were no pending modifications
            synchronizeWithQuartz(result);
        }
    }

    int getPendingModificationsCount() {
        synchronized (pendingModifications) {
            return pendingModifications.size();
        }
    }

    private void modifyRepository(ItemDelta<?, ?> delta, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        if (delta != null) {
            modifyRepository(singleton(delta), parentResult);
        }
    }

    private void modifyRepository(Collection<ItemDelta<?, ?>> deltas, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        if (isPersistent() && !deltas.isEmpty()) {
            try {
                beans.repositoryService.modifyObject(TaskType.class, getOid(), deltas, result);
                beans.listenerRegistry.notifyTaskUpdated(this, result);
            } finally {
                synchronizeWithQuartzIfNeeded(deltas, result);
            }
        }
    }

    private void modifyRepositoryWithoutQuartz(Collection<ItemDelta<?, ?>> deltas,
            ModificationPrecondition<TaskType> precondition, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {
        if (isPersistent()) {
            beans.repositoryService.modifyObject(TaskType.class, getOid(), deltas, precondition, null, result);
            beans.listenerRegistry.notifyTaskUpdated(this, result);
        }
    }

    @Override
    public void applyDeltasImmediate(Collection<ItemDelta<?, ?>> itemDeltas, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        if (isPersistent()) {
            beans.repositoryService.modifyObject(TaskType.class, getOid(), CloneUtil.cloneCollectionMembers(itemDeltas), result);

        }
        applyModificationsTransient(itemDeltas);
        synchronizeWithQuartzIfNeeded(pendingModifications, result);
    }

    private DeltaFactory deltaFactory() {
        return beans.prismContext.deltaFactory();
    }

    @Override
    public void applyModificationsTransient(Collection<ItemDelta<?, ?>> modifications) throws SchemaException {
        synchronized (prismAccess) {
            ItemDeltaCollectionsUtil.applyTo(modifications, taskPrism);
        }
    }
    //endregion

    //region Quartz integration
    public void synchronizeWithQuartz(OperationResult result) {
        synchronized (quartzAccess) {
            beans.taskSynchronizer.synchronizeTask(this, result);
            recreateQuartzTrigger = false;
        }
    }

    public void synchronizeWithQuartzWithTriggerRecreation(OperationResult result) {
        synchronized (quartzAccess) {
            recreateQuartzTrigger = true;
            synchronizeWithQuartz(result);
        }
    }

    private static final Set<QName> QUARTZ_RELATED_PROPERTIES = new HashSet<>();

    static {
        // TODO why not scheduling status? Maybe because we do the synchronization explicitly then
        QUARTZ_RELATED_PROPERTIES.add(TaskType.F_BINDING);
        QUARTZ_RELATED_PROPERTIES.add(TaskType.F_SCHEDULE);
        QUARTZ_RELATED_PROPERTIES.add(TaskType.F_HANDLER_URI);
    }

    private void synchronizeWithQuartzIfNeeded(Collection<ItemDelta<?, ?>> deltas, OperationResult parentResult) {
        synchronized (quartzAccess) {
            if (recreateQuartzTrigger) {
                synchronizeWithQuartz(parentResult);
            } else {
                for (ItemDelta<?, ?> delta : deltas) {
                    if (delta.getParentPath().isEmpty() && QUARTZ_RELATED_PROPERTIES.contains(delta.getElementName())) {
                        synchronizeWithQuartz(parentResult);
                        break;
                    }
                }
            }
        }
    }
    //endregion

    //region Access to prism items
    @Nullable <X> PropertyDelta<X> createPropertyDeltaIfPersistent(ItemName name, X value) {
        return isPersistent() ? deltaFactory().property().createReplaceDeltaOrEmptyDelta(
                taskManager.getTaskObjectDefinition(), name, value) : null;
    }

    @Nullable <X> PropertyDelta<X> createPropertyDelta(ItemName name, X value) {
        return deltaFactory().property().createReplaceDeltaOrEmptyDelta(taskManager.getTaskObjectDefinition(), name, value);
    }

    @Nullable <X extends Containerable> ContainerDelta<X> createContainerDeltaIfPersistent(ItemName name, X value)
            throws SchemaException {
        if (isPersistent()) {
            //noinspection unchecked
            X clonedValue = value != null ? (X) value.asPrismContainerValue().clone().asContainerable() : null;
            return deltaFactory().container().createModificationReplace(name, TaskType.class, clonedValue);
        } else {
            return null;
        }
    }

    @Nullable
    private <X extends Containerable> ContainerDelta<X> createContainerValueAddDeltaIfPersistent(ItemName name, X value)
            throws SchemaException {
        if (isPersistent()) {
            //noinspection unchecked
            X clonedValue = value != null ? (X) value.asPrismContainerValue().clone().asContainerable() : null;
            return deltaFactory().container().createModificationAdd(name, TaskType.class, clonedValue);
        } else {
            return null;
        }
    }

    @Nullable
    private ReferenceDelta createReferenceValueAddDeltaIfPersistent(ItemName name, Referencable value) {
        if (isPersistent()) {
            PrismReferenceValue clonedValue = value != null ? value.asReferenceValue().clone() : null;
            return deltaFactory().reference().createModificationAdd(TaskType.class, name, clonedValue);
        } else {
            return null;
        }
    }

    @Nullable
    private ReferenceDelta createReferenceDeltaIfPersistent(ItemName name, ObjectReferenceType value) {
        return isPersistent() ? deltaFactory().reference().createModificationReplace(name,
                taskManager.getTaskObjectDefinition(), value != null ? value.clone().asReferenceValue() : null) : null;
    }

    private <T> T cloneIfRunning(T value) {
        return isLiveRunningInstance() ? CloneUtil.clone(value) : value;
    }

    private <X> X getProperty(ItemPath name) {
        synchronized (prismAccess) {
            PrismProperty<X> property = taskPrism.findProperty(name);
            return property != null ? property.getRealValue() : null;
        }
    }

    protected <X> void setProperty(ItemName name, X value) {
        addPendingModification(setPropertyAndCreateDeltaIfPersistent(name, value));
    }

    @Override
    public ActivityStateType getActivityStateOrClone(ItemPath path) {
        synchronized (prismAccess) {
            return cloneIfRunning(ActivityStateUtil.getActivityState(taskPrism.asObjectable(), path));
        }
    }

    @Override
    public <C extends Containerable> C getContainerableOrClone(ItemPath path, Class<C> type) {
        synchronized (prismAccess) {
            PrismContainer<C> container = taskPrism.findContainer(path);
            return container != null && !container.hasNoValues() ? cloneIfRunning(container.getRealValue(type)) : null;
        }
    }

    private <C extends Containerable> C getContainerableOrClone(ItemName name) {
        synchronized (prismAccess) {
            PrismContainer<C> container = taskPrism.findContainer(name);
            return container != null && !container.hasNoValues() ? cloneIfRunning(container.getRealValue()) : null;
        }
    }

    @Override
    public boolean doesItemExist(ItemPath path) {
        synchronized (prismAccess) {
            return taskPrism.findItem(path) != null;
        }
    }

    private <X extends Containerable> void setContainerable(ItemName name, X value) {
        try {
            addPendingModification(setContainerableAndCreateDeltaIfPersistent(name, value));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't set the task container '" + name + "': " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <X extends Containerable> void addContainerable(ItemName name, X value) {
        try {
            addPendingModification(addContainerableAndCreateDeltaIfPersistent(name, value));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't add the task container '" + name + "' value: " + e.getMessage(), e);
        }
    }

    private void addReferencable(ItemName name, Referencable value) {
        addPendingModification(addReferencableAndCreateDeltaIfPersistent(name, value));
    }

    private <X> void setPropertyTransient(ItemName name, X value) {
        synchronized (prismAccess) {
            try {
                taskPrism.setPropertyRealValue(name, value);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't set the task property '" + name + "': " + e.getMessage(), e);
            }
        }
    }

    private <X extends Containerable> void setContainerableTransient(ItemName name, X value) {
        synchronized (prismAccess) {
            try {
                taskPrism.setContainerRealValue(name, value);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't set the task container '" + name + "': " + e.getMessage(), e);
            }
        }
    }

    private <X extends Containerable> void addContainerableTransient(ItemName name, X value) {
        if (value == null) {
            return;
        }
        synchronized (prismAccess) {
            try {
                //noinspection unchecked
                taskPrism.findOrCreateContainer(name).add(value.asPrismContainerValue());
            } catch (SchemaException e) {
                throw new SystemException("Couldn't add the task container '" + name + "' value: " + e.getMessage(), e);
            }
        }
    }

    private void addReferencableTransient(ItemName name, Referencable value) {
        if (value == null) {
            return;
        }
        synchronized (prismAccess) {
            try {
                taskPrism.findOrCreateReference(name).add(value.asReferenceValue());
            } catch (SchemaException e) {
                throw new SystemException("Couldn't add the task reference '" + name + "' value: " + e.getMessage(), e);
            }
        }
    }

    private <X> void setPropertyImmediate(ItemName name, X value, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        try {
            modifyRepository(setPropertyAndCreateDeltaIfPersistent(name, value), result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException("Unexpected ObjectAlreadyExistsException while modifying '" + name + "' property: " +
                    ex.getMessage(), ex);
        }
    }

    private <X> PropertyDelta<X> setPropertyAndCreateDeltaIfPersistent(ItemName name, X value) {
        setPropertyTransient(name, value);
        return createPropertyDeltaIfPersistent(name, value);
    }

    private <X extends Containerable> ContainerDelta<X> setContainerableAndCreateDeltaIfPersistent(ItemName name, X value)
            throws SchemaException {
        setContainerableTransient(name, value);
        return createContainerDeltaIfPersistent(name, value);
    }

    private <X extends Containerable> ContainerDelta<X> addContainerableAndCreateDeltaIfPersistent(ItemName name, X value)
            throws SchemaException {
        addContainerableTransient(name, value);
        return createContainerValueAddDeltaIfPersistent(name, value);
    }

    private ReferenceDelta addReferencableAndCreateDeltaIfPersistent(ItemName name, Referencable value) {
        addReferencableTransient(name, value);
        return createReferenceValueAddDeltaIfPersistent(name, value);
    }

    private PrismReferenceValue getReferenceValue(ItemName name) {
        synchronized (prismAccess) {
            PrismReference reference = taskPrism.findReference(name);
            return reference != null ? reference.getValue() : null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private ObjectReferenceType getReference(ItemName name) {
        PrismReferenceValue value = getReferenceValue(name);
        return value != null ? new ObjectReferenceType().setupReferenceValue(value) : null;
    }

    @SuppressWarnings("SameParameterValue")
    private void setReference(ItemName name, ObjectReferenceType value) {
        addPendingModification(setReferenceAndCreateDeltaIfPersistent(name, value));
    }

    private void setReferenceTransient(ItemName name, ObjectReferenceType value) {
        synchronized (prismAccess) {
            try {
                taskPrism.findOrCreateReference(name).replace(value != null ? value.clone().asReferenceValue() : null);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't set the task reference '" + name + "': " + e.getMessage(), e);
            }
        }
    }

    private ReferenceDelta setReferenceAndCreateDeltaIfPersistent(ItemName name, ObjectReferenceType value) {
        setReferenceTransient(name, value);
        return createReferenceDeltaIfPersistent(name, value);
    }

    @Override
    public long getLegacyProgress() {
        return defaultIfNull(getProperty(TaskType.F_PROGRESS), 0L);
    }

    @Override
    public void setLegacyProgress(Long value) {
        setProperty(TaskType.F_PROGRESS, value);
    }

    @Override
    public void incrementLegacyProgressTransient() {
        synchronized (prismAccess) {
            setProgressTransient(getLegacyProgress() + 1);
        }
    }

    @Override
    public void setLegacyProgressImmediate(Long value, OperationResult result) throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_PROGRESS, value, result);
    }

    public void setProgressTransient(Long value) {
        setPropertyTransient(TaskType.F_PROGRESS, value);
    }

    @Override
    public OperationStatsType getStoredOperationStatsOrClone() {
        return getContainerableOrClone(TaskType.F_OPERATION_STATS);
    }

    public void setOperationStats(OperationStatsType value) {
        setContainerable(TaskType.F_OPERATION_STATS, value);
    }

    public void setOperationStatsTransient(OperationStatsType value) {
        setContainerableTransient(TaskType.F_OPERATION_STATS, value != null ? value.clone() : null);
    }

    @Override
    @Nullable
    public Long getExpectedTotal() {
        return getProperty(TaskType.F_EXPECTED_TOTAL);
    }

    @Override
    public void setExpectedTotal(Long value) {
        setProperty(TaskType.F_EXPECTED_TOTAL, value);
    }

    /*
     * Result setters set also result status type!
     */

    @Override
    public OperationResult getResult() {
        return taskResult;
    }

    @Override
    public void setResult(OperationResult result) {
        addPendingModification(setResultAndPrepareDelta(result));
        setProperty(TaskType.F_RESULT_STATUS, result != null ? result.getStatus().createStatusType() : null);
    }

    public void setResultImmediate(OperationResult result, OperationResult opResult)
            throws ObjectNotFoundException, SchemaException {
        try {
            modifyRepository(setResultAndPrepareDelta(result), opResult);
            setPropertyImmediate(TaskType.F_RESULT_STATUS, result != null ? result.getStatus().createStatusType() : null, opResult);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
    }

    public void setResultTransient(OperationResult result) {
        synchronized (prismAccess) {
            taskResult = result;
            taskResultIncomplete = false;
            if (result != null) {
                clearPrismResultIncompleteFlag();
            }
            taskPrism.asObjectable().setResult(result != null ? result.createOperationResultType() : null);
            taskPrism.asObjectable().setResultStatus(result != null ? result.getStatus().createStatusType() : null);
        }
    }

    /** Must be guarded by {@link #prismAccess}! */
    private void clearPrismResultIncompleteFlag() {
        PrismProperty<Object> existingResult = taskPrism.findProperty(TaskType.F_RESULT);
        if (existingResult != null) {
            existingResult.setIncomplete(false);
        }
    }

    private PropertyDelta<?> setResultAndPrepareDelta(OperationResult result) {
        setResultTransient(result);
        if (isPersistent()) {
            return createPropertyDeltaIfPersistent(TaskType.F_RESULT, result != null ? result.createOperationResultType() : null);
        } else {
            return null;
        }
    }

    /*
     *  Result status
     *
     *  We read the status from current 'taskResult', not from prism - to be sure to get the most current value.
     *  However, when updating, we update the result in prism object in order for the result to be stored correctly in
     *  the repo (useful for displaying the result in task list).
     */

    @Override
    public OperationResultStatusType getResultStatus() {
        if (taskResult == null) {
            // TODO is it OK to fall back to task prism here?
            synchronized (prismAccess) {
                return taskPrism.asObjectable().getResultStatus();
            }
        } else {
            return taskResult.getStatus().createStatusType();
        }
    }

    @Override
    public String getHandlerUri() {
        return getProperty(TaskType.F_HANDLER_URI);
    }

    @Override
    public void setHandlerUri(String value) {
        setProperty(TaskType.F_HANDLER_URI, value);
    }

    /** Derives default binding form schedule */
    private static TaskBindingType bindingFromSchedule(ScheduleType schedule) {
        if (schedule != null && schedule.getInterval() != null && schedule.getInterval() > 0 && schedule.getInterval() <= TIGHT_BINDING_INTERVAL_LIMIT) {
            return TaskBindingType.TIGHT;
        } else {
            return TaskBindingType.LOOSE;
        }
    }

    @Override
    public @NotNull TaskPersistenceStatus getPersistenceStatus() {
        return getOid() != null ? TaskPersistenceStatus.PERSISTENT : TaskPersistenceStatus.TRANSIENT;
    }

    @Override
    public boolean isPersistent() {
        return getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT;
    }

    @Override
    public boolean isTransient() {
        return getPersistenceStatus() == TaskPersistenceStatus.TRANSIENT;
    }

    @Override
    public boolean isAsynchronous() {   // overridden in RunningTask
        return getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT;
    }

    @Override
    public String getOid() {
        synchronized (prismAccess) {
            return taskPrism.getOid();
        }
    }

    public synchronized void setOid(String oid) {
        synchronized (prismAccess) {
            taskPrism.setOid(oid);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return getProperty(TaskType.F_TASK_IDENTIFIER);
    }

    @Override
    public String getTaskRunIdentifier() {
        return getPropertyRealValue(
                ItemPath.create(
                        TaskType.F_ACTIVITY_STATE,
                        TaskActivityStateType.F_TREE,
                        ActivityTreeStateType.F_TASK_RUN_IDENTIFIER),
                String.class);
    }

    public void setTaskIdentifier(String value) {
        setProperty(TaskType.F_TASK_IDENTIFIER, value);
    }

    /*
     * Execution and scheduling state
     *
     * IMPORTANT: do not set this attribute explicitly (due to the need of synchronization with Quartz scheduler).
     * Use task life-cycle methods, like close(), suspendTask(), resumeTask(), and so on.
     */

    @Override
    public TaskExecutionStateType getExecutionState() {
        return getProperty(TaskType.F_EXECUTION_STATE);
    }

    @Override
    public TaskSchedulingStateType getSchedulingState() {
        return getProperty(TaskType.F_SCHEDULING_STATE);
    }

    public void setExecutionState(@NotNull TaskExecutionStateType value) {
        setProperty(TaskType.F_EXECUTION_STATE, value);
    }

    public void setSchedulingState(@NotNull TaskSchedulingStateType value) {
        setProperty(TaskType.F_SCHEDULING_STATE, value);
    }

    @Override
    public void setInitialExecutionAndScheduledState(TaskExecutionStateType executionState,
            TaskSchedulingStateType schedulingState) {
        stateCheck(isTransient(), "Initial execution/scheduling state can be set only on transient tasks.");
        synchronized (prismAccess) { // maybe not really necessary
            setProperty(TaskType.F_EXECUTION_STATE, executionState);
            setProperty(TaskType.F_SCHEDULING_STATE, schedulingState);
        }
    }

    @Override
    public void setInitiallyWaitingForPrerequisites() {
        synchronized (prismAccess) { // maybe not really necessary
            setInitialExecutionAndScheduledState(TaskExecutionStateType.WAITING, TaskSchedulingStateType.WAITING);
            setWaitingReason(TaskWaitingReasonType.OTHER_TASKS);
        }
    }

    /** Quartz should be updated by the caller. */
    public void setExecutionAndSchedulingStateImmediate(TaskExecutionStateType newExecState,
            TaskSchedulingStateType newSchedulingState, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        try {
            setExecutionAndSchedulingStateImmediate(newExecState, newSchedulingState, null, result);
        } catch (PreconditionViolationException e) {
            throw new SystemException(e);
        }
    }

    /** Quartz should be updated by the caller. */
    public void setExecutionAndSchedulingStateImmediate(TaskExecutionStateType newExecState,
            TaskSchedulingStateType newSchedulingState, TaskSchedulingStateType oldSchedulingState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {
        try {
            List<ItemDelta<?, ?>> deltas = Arrays.asList(
                    createPropertyDelta(TaskType.F_EXECUTION_STATE, newExecState),
                    createPropertyDelta(TaskType.F_SCHEDULING_STATE, newSchedulingState));
            modifyRepositoryWithoutQuartz(deltas,
                    t -> oldSchedulingState == null || oldSchedulingState == t.asObjectable().getSchedulingState(), result);
            // This is intentionally placed after repo change, to ensure consistent state even after precondition violation.
            applyModificationsTransient(deltas);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
    }

    @Override
    public TaskWaitingReasonType getWaitingReason() {
        return getProperty(TaskType.F_WAITING_REASON);
    }

    public void setWaitingReason(TaskWaitingReasonType value) {
        setProperty(TaskType.F_WAITING_REASON, value);
    }

    @Override
    public @NotNull TaskRecurrenceType getRecurrence() {
        synchronized (prismAccess) {
            return TaskTypeUtil.getEffectiveRecurrence(taskPrism.asObjectable());
        }
    }

    // checks latest start time (useful for recurring tightly coupled tasks)
    public boolean stillCanStart() {
        synchronized (prismAccess) {
            ScheduleType schedule = taskPrism.asObjectable().getSchedule();
            if (schedule != null && schedule.getLatestStartTime() != null) {
                long lst = schedule.getLatestStartTime().toGregorianCalendar().getTimeInMillis();
                return lst >= System.currentTimeMillis();
            } else {
                return true;
            }
        }
    }

    @Override
    public TaskExecutionConstraintsType getExecutionConstraints() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getExecutionConstraints();
        }
    }

    @Override
    public void setExecutionConstraints(TaskExecutionConstraintsType value) {
        setContainerable(TaskType.F_EXECUTION_CONSTRAINTS, value);
    }

    @Override
    public String getGroup() {
        synchronized (prismAccess) {
            TaskExecutionConstraintsType executionConstraints = getExecutionConstraints();
            return executionConstraints != null ? executionConstraints.getGroup() : null;
        }
    }

    @NotNull
    @Override
    public Collection<String> getGroups() {
        // This cannot be moved to the interface because of the synchronization required.
        synchronized (prismAccess) {
            return new HashSet<>(getGroupsWithLimits().keySet());
        }
    }

    @NotNull
    @Override
    public Map<String, Integer> getGroupsWithLimits() {
        synchronized (prismAccess) {
            TaskExecutionConstraintsType executionConstraints = getExecutionConstraints();
            if (executionConstraints == null) {
                return emptyMap();
            }
            Map<String, Integer> rv = new HashMap<>();
            if (executionConstraints.getGroup() != null) {
                rv.put(executionConstraints.getGroup(), executionConstraints.getGroupTaskLimit());
            }
            for (TaskExecutionGroupConstraintType sg : executionConstraints.getSecondaryGroup()) {
                if (sg.getGroup() != null) { // shouldn't occur but it's a user configurable field, so be prepared for the worst
                    rv.put(sg.getGroup(), sg.getGroupTaskLimit());
                }
            }
            return rv;
        }
    }

    @Override
    public ScheduleType getSchedule() {
        return getContainerableOrClone(TaskType.F_SCHEDULE);
    }

    @Override
    public Integer getScheduleInterval() {
        synchronized (prismAccess) {
            ScheduleType schedule = getSchedule();
            return schedule != null ? schedule.getInterval() : null;
        }
    }

    @Override
    public boolean hasScheduleInterval() {
        Integer scheduleInterval = getScheduleInterval();
        return scheduleInterval != null && scheduleInterval != 0;
    }

    @Override
    public void setSchedule(ScheduleType value) {
        synchronized (prismAccess) {
            setContainerable(TaskType.F_SCHEDULE, value);
        }
    }

    @Override
    public ThreadStopActionType getThreadStopAction() {
        return getProperty(TaskType.F_THREAD_STOP_ACTION);
    }

    @Override
    public void setThreadStopAction(ThreadStopActionType value) {
        setProperty(TaskType.F_THREAD_STOP_ACTION, value);
    }

    @Override
    public TaskBindingType getBinding() {
        return getProperty(TaskType.F_BINDING);
    }

    public void setBinding(TaskBindingType value) {
        setProperty(TaskType.F_BINDING, value);
    }

    @SuppressWarnings("SameParameterValue")
    private void setBindingTransient(TaskBindingType value) {
        setPropertyTransient(TaskType.F_BINDING, value);
    }

    @Override
    public PrismObject<? extends FocusType> getOwner(OperationResult result) {
        PrismReferenceValue ownerRef = getReferenceValue(TaskType.F_OWNER_REF);
        if (ownerRef == null) {
            return null; // Shouldn't occur (this is checked on instantiation)
        }
        return resolveOwnerRef(ownerRef, result);
    }

    private PrismObject<? extends FocusType> resolveOwnerRef(PrismReferenceValue ownerRef, OperationResult result) {
        if (ownerRef.getObject() != null) {
            return ownerRef.getObject();
        }

        try {
            // todo use type from the reference instead
            PrismObject<FocusType> owner =
                    beans.repositoryService.getObject(FocusType.class, ownerRef.getOid(), null, result);
            synchronized (prismAccess) {
                ownerRef.setObject(owner);
            }
            return owner;
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logExceptionAsWarning(LOGGER, "The owner of task {} cannot be found (owner OID: {})",
                    e, this, ownerRef.getOid());
            return null;
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "The owner of task {} cannot be retrieved (owner OID: {})",
                    e, this, ownerRef.getOid());
            return null;
        }
    }

    @Override
    public void setOwner(PrismObject<? extends FocusType> owner) {
        stateCheck(isTransient(), "setOwner method can be called only on transient tasks!");

        synchronized (prismAccess) {
            if (owner == null) {
                taskPrism.getValue().removeReference(TaskType.F_OWNER_REF);
            } else {
                try {
                    taskPrism.findOrCreateReference(TaskType.F_OWNER_REF)
                            .getValue()
                            .setObject(owner);
                } catch (SchemaException e) {
                    throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void setOwnerRef(ObjectReferenceType ownerRef) {
        stateCheck(isTransient(), "setOwnerRef method can be called only on transient tasks!");
        setReference(TaskType.F_OWNER_REF, ownerRef);
    }

    public void checkOwnerRefPresent() throws SchemaException {
        PrismReferenceValue ownerRef = getReferenceValue(TaskType.F_OWNER_REF);
        schemaCheck(ownerRef != null, "No ownerRef present in %s", this);
    }

    @Override
    public String getChannel() {
        return getProperty(TaskType.F_CHANNEL);
    }

    @Override
    public void setChannel(String value) {
        setProperty(TaskType.F_CHANNEL, value);
    }

    /**
     * Beware: this returns cloned object reference!
     */
    @Override
    public ObjectReferenceType getObjectRefOrClone() {
        return cloneIfRunning(getObjectRefInternal());
    }

    private ObjectReferenceType getObjectRefInternal() {
        return getReference(TaskType.F_OBJECT_REF);
    }

    @Override
    public void setObjectRef(ObjectReferenceType value) {
        setReference(TaskType.F_OBJECT_REF, value);
    }

    @Override
    public void setObjectRef(String oid, QName type) {
        setObjectRef(new ObjectReferenceType().oid(oid).type(type));
    }

    @Override
    public String getObjectOid() {
        ObjectReferenceType ref = getObjectRefInternal();
        return ref != null ? ref.getOid() : null;
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        // Shortcut
        ObjectReferenceType objectRef = getObjectRefInternal();
        if (objectRef == null) {
            return null;
        }
        if (objectRef.asReferenceValue().getObject() != null) {
            PrismObject<?> object = objectRef.asReferenceValue().getObject();
            if (object.canRepresent(type)) {
                //noinspection unchecked
                return (PrismObject<T>) object;
            } else {
                throw new IllegalArgumentException(
                        "Requested object type " + type + ", but the type of object in the task is " + object.getClass());
            }
        }

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "getObject");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        try {
            PrismObject<T> object = beans.repositoryService.getObject(type, objectRef.getOid(), null, result);
            synchronized (prismAccess) {
                objectRef.asReferenceValue().setObject(object);
                return object.clone();
            }
        } catch (Throwable ex) {
            result.recordFatalError(ex);
            throw ex;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public PolyStringType getName() {
        synchronized (prismAccess) {
            // not using getProperty because of PolyString vs PolyStringType dichotomy
            return taskPrism.asObjectable().getName();
        }
    }

    @Override
    public void setName(PolyStringType value) {
        addPendingModification(setNameAndPrepareDelta(value));
    }

    @Override
    public void setName(String value) {
        addPendingModification(setNameAndPrepareDelta(new PolyStringType(value)));
    }

    @Override
    public void setNameImmediate(PolyStringType value, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        modifyRepository(setNameAndPrepareDelta(value), result);
    }

    public void setNameTransient(PolyStringType name) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().setName(name);
        }
    }

    private PropertyDelta<?> setNameAndPrepareDelta(PolyStringType value) {
        setNameTransient(value);
        return createPropertyDeltaIfPersistent(TaskType.F_NAME, value.toPolyString());
    }

    @Override
    public String getDescription() {
        return getProperty(TaskType.F_DESCRIPTION);
    }

    @Override
    public void setDescription(String value) {
        setProperty(TaskType.F_DESCRIPTION, value);
    }

    @Override
    public void setDescriptionImmediate(String value, OperationResult result) throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_DESCRIPTION, value, result);
    }

    @Override
    public String getParent() {
        return getProperty(TaskType.F_PARENT);
    }

    @Override
    public TaskQuartzImpl getParentTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (getParent() == null) {
            return null;
        } else {
            return beans.taskRetriever.getTaskByIdentifier(getParent(), result);
        }
    }

    public void setParent(String value) {
        setProperty(TaskType.F_PARENT, value);
    }

    @Override
    public List<String> getDependents() {
        synchronized (prismAccess) {
            return new ArrayList<>(taskPrism.asObjectable().getDependent());
        }
    }

    @Override
    public List<Task> listDependents(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listDependents");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        try {
            return beans.taskRetriever.listDependents(this, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void addDependent(String value) {
        addPendingModification(addDependentAndPrepareDelta(value));
    }

    private void addDependentTransient(String name) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().getDependent().add(name);
        }
    }

    private PropertyDelta<?> addDependentAndPrepareDelta(String value) {
        addDependentTransient(value);
        return isPersistent() ? deltaFactory().property().createAddDelta(
                taskManager.getTaskObjectDefinition(), TaskType.F_DEPENDENT, value) : null;
    }

    private void addTriggerTransient(TriggerType trigger) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().getTrigger().add(trigger);
        }
    }

    public void addTrigger(TriggerType trigger) throws SchemaException {
        addPendingModification(addTriggerAndPrepareDelta(trigger));
    }

    private ItemDelta<?, ?> addTriggerAndPrepareDelta(TriggerType trigger) throws SchemaException {
        addTriggerTransient(trigger.clone());
        return isPersistent() ?
                beans.prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_TRIGGER).add(trigger.clone())
                        .asItemDelta()
                : null;
    }
    //endregion

    //region Dealing with extension
    @Override
    public PrismContainer<? extends ExtensionType> getExtensionOrClone() {
        synchronized (prismAccess) {
            //noinspection unchecked
            return cloneIfRunning((PrismContainer<ExtensionType>) taskPrism.getExtension());
        }
    }

    @NotNull
    @Override
    public PrismContainer<? extends ExtensionType> getOrCreateExtension() throws SchemaException {
        synchronized (prismAccess) {
            //noinspection unchecked
            return cloneIfRunning((PrismContainer<ExtensionType>) taskPrism.getOrCreateExtension());
        }
    }

    @Nullable
    @Override
    public PrismContainer<? extends ExtensionType> getExtensionClone() {
        //noinspection unchecked
        return CloneUtil.clone((PrismContainer<ExtensionType>) taskPrism.getExtension());
    }

    @Override
    public <T> PrismProperty<T> getExtensionPropertyOrClone(ItemName name) {
        synchronized (prismAccess) {
            return cloneIfRunning(getExtensionPropertyUnsynchronized(name));
        }
    }

    private <T> PrismProperty<T> getExtensionPropertyUnsynchronized(ItemName name) {
        PrismContainer<?> extension = taskPrism.getExtension();
        return extension != null ? extension.findProperty(name) : null;
    }

    // todo should return clone for running task?
    @Override
    public <T> T getPropertyRealValue(ItemPath path, Class<T> expectedType) {
        synchronized (prismAccess) {
            PrismProperty<T> property = taskPrism.findProperty(path);
            return property != null && !property.isEmpty() ? property.getRealValue(expectedType) : null;
        }
    }

    @Override
    public <T> T getPropertyRealValueOrClone(ItemPath path, Class<T> expectedType) {
        synchronized (prismAccess) {
            PrismProperty<T> property = taskPrism.findProperty(path);
            return property != null && !property.isEmpty() ? cloneIfRunning(property.getRealValue(expectedType)) : null;
        }
    }

    @Override
    public <T> T getItemRealValueOrClone(ItemPath path, Class<T> expectedType) {
        synchronized (prismAccess) {
            Item<?, ?> item = taskPrism.findItem(path);
            return item != null && !item.isEmpty() ? cloneIfRunning(item.getRealValue(expectedType)) : null;
        }
    }

    @Override
    public ObjectReferenceType getReferenceRealValue(ItemPath path) {
        synchronized (prismAccess) {
            PrismReference reference = taskPrism.findReference(path);
            return reference != null && !reference.isEmpty() ? ObjectTypeUtil.createObjectRef(reference.getValue()) : null;
        }
    }

    @Override
    public Collection<ObjectReferenceType> getReferenceRealValues(ItemPath path) {
        synchronized (prismAccess) {
            PrismReference reference = taskPrism.findReference(path);
            if (reference != null) {
                return ObjectTypeUtil.createObjectRefs(reference.getValues());
            } else {
                return List.of();
            }
        }
    }

    @Override
    public <T extends Containerable> T getExtensionContainerRealValueOrClone(ItemName name) {
        synchronized (prismAccess) {
            Item<?, ?> item = getExtensionItemUnsynchronized(name);
            if (item == null || item.getValues().isEmpty()) {
                return null;
            } else {
                //noinspection unchecked
                return cloneIfRunning(((PrismContainer<T>) item).getRealValue());
            }
        }
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition<?>> Item<IV, ID> getExtensionItemOrClone(ItemName name) {
        synchronized (prismAccess) {
            return cloneIfRunning(getExtensionItemUnsynchronized(name));
        }
    }

    private <IV extends PrismValue, ID extends ItemDefinition<?>> Item<IV, ID> getExtensionItemUnsynchronized(ItemName name) {
        PrismContainer<? extends ExtensionType> extension = getExtensionOrClone();
        return extension != null ? extension.findItem(name) : null;
    }

    @Override
    public PrismReference getExtensionReferenceOrClone(ItemName name) {
        return (PrismReference) (Item) getExtensionItemOrClone(name);
    }

    @Override
    public void setExtensionItem(Item<?, ?> item) throws SchemaException {
        if (item instanceof PrismProperty) {
            setExtensionProperty((PrismProperty) item);
        } else if (item instanceof PrismReference) {
            setExtensionReference((PrismReference) item);
        } else if (item instanceof PrismContainer) {
            //noinspection unchecked
            setExtensionContainer((PrismContainer) item);
        } else {
            throw new IllegalArgumentException("Unknown kind of item: " + (item == null ? "(null)" : item.getClass()));
        }
    }

    @Override
    public void setExtensionProperty(PrismProperty<?> property) throws SchemaException {
        addPendingModification(setExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
                PrismValueCollectionsUtil.cloneCollection(property.getValues())));
    }

    @Override
    public void setExtensionReference(PrismReference reference) throws SchemaException {
        addPendingModification(
                setExtensionReferenceAndPrepareDelta(
                        reference.getElementName(),
                        reference.getDefinition(),
                        PrismValueCollectionsUtil.cloneCollection(reference.getValues())));
    }

    @Override
    public void addExtensionReference(PrismReference reference) throws SchemaException {
        addPendingModification(addExtensionReferenceAndPrepareDelta(reference.getElementName(), reference.getDefinition(),
                PrismValueCollectionsUtil.cloneCollection(reference.getValues())));
    }

    @Override
    public <C extends Containerable> void setExtensionContainer(PrismContainer<C> container) throws SchemaException {
        addPendingModification(setExtensionContainerAndPrepareDelta(container.getElementName(), container.getDefinition(),
                PrismValueCollectionsUtil.cloneCollection(container.getValues())));
    }

    @Override
    public <T> void setPropertyRealValue(ItemPath path, T value) throws SchemaException {
        modify(
                taskManager.getPrismContext().deltaFor(TaskType.class)
                        .item(path).add(value)
                        .asItemDelta());
    }

    // use this method to avoid cloning the value
    @Override
    public <T extends Containerable> void setExtensionContainerValue(QName containerName, T value) throws SchemaException {
        PrismContainerDefinition<T> containerDef = beans.prismContext.getSchemaRegistry()
                .findContainerDefinitionByElementName(containerName);
        if (containerDef == null) {
            throw new SchemaException("Unknown container item " + containerName);
        }
        addPendingModification(
                setExtensionContainerAndPrepareDelta(containerName, containerDef,
                        (Collection) singletonList(value.asPrismContainerValue())));
    }

    @Override
    public void addExtensionProperty(PrismProperty<?> property) throws SchemaException {
        addPendingModification(addExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
                PrismValueCollectionsUtil.cloneCollection(property.getValues())));
    }

    @Override
    public void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException {
        addPendingModification(deleteExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
                PrismValueCollectionsUtil.cloneCollection(property.getValues())));
    }

    @Override
    public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        try {
            modifyRepository(setExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
                    PrismValueCollectionsUtil.cloneCollection(property.getValues())), result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
    }

    private ItemDelta<?, ?> setExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition<?> definition,
            Collection<? extends PrismPropertyValue<?>> values) throws SchemaException {
        ItemDelta<?, ?> delta = deltaFactory().property().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?, ?> setExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition,
            Collection<? extends PrismReferenceValue> values) throws SchemaException {
        ItemDelta<?, ?> delta = deltaFactory().reference().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?, ?> addExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition,
            Collection<? extends PrismReferenceValue> values) throws SchemaException {
        ItemDelta<?, ?> delta = deltaFactory().reference().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return addExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?, ?> setExtensionContainerAndPrepareDelta(QName itemName, PrismContainerDefinition<?> definition,
            Collection<? extends PrismContainerValue<?>> values) throws SchemaException {
        ItemDelta<?, ?> delta = deltaFactory().container().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private <V extends PrismValue> ItemDelta<?, ?> setExtensionItemAndPrepareDeltaCommon(ItemDelta<?, ?> delta, Collection<V> values)
            throws SchemaException {
        // these values should have no parent, otherwise the following will fail
        //noinspection unchecked
        ((ItemDelta) delta).setValuesToReplace(values);
        applyModificationsTransient(singletonList(delta)); // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }

    private <V extends PrismValue> ItemDelta<?, ?> addExtensionItemAndPrepareDeltaCommon(ItemDelta<?, ?> delta, Collection<V> values)
            throws SchemaException {
        // these values should have no parent, otherwise the following will fail
        //noinspection unchecked
        ((ItemDelta) delta).addValuesToAdd(values);
        applyModificationsTransient(singletonList(delta)); // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }

    private ItemDelta<?, ?> addExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
            Collection<? extends PrismPropertyValue> values) throws SchemaException {
        //noinspection unchecked
        ItemDelta<?, ?> delta = deltaFactory().property().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        //noinspection unchecked
        ((ItemDelta) delta).addValuesToAdd(values);
        applyModificationsTransient(singletonList(delta)); // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }

    private ItemDelta<?, ?> deleteExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
            Collection<? extends PrismPropertyValue> values) throws SchemaException {
        //noinspection unchecked
        ItemDelta<?, ?> delta = deltaFactory().property().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        //noinspection unchecked
        ((ItemDelta) delta).addValuesToDelete(values);
        applyModificationsTransient(singletonList(delta)); // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }
    //endregion

    //region Other getters and setters
    @Override
    public PrismObject<UserType> getRequestee() {
        return requestee;
    }

    @Override
    public void setRequesteeTransient(PrismObject<UserType> user) {
        requestee = user;
    }

    @Override
    public String getNode() {
        return getProperty(TaskType.F_NODE);
    }

    @Override
    public String getNodeAsObserved() {
        return getProperty(TaskType.F_NODE_AS_OBSERVED);
    }

    public void setNode(String value) {
        setProperty(TaskType.F_NODE, value);
    }

    public void setNodeImmediate(String value, OperationResult result) throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_NODE, value, result);
    }

    @Override
    public Long getLastRunStartTimestamp() {
        XMLGregorianCalendar gc = getProperty(TaskType.F_LAST_RUN_START_TIMESTAMP);
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    public void setLastRunStartTimestamp(Long value) {
        setProperty(TaskType.F_LAST_RUN_START_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    @Override
    public Long getLastRunFinishTimestamp() {
        XMLGregorianCalendar gc = getProperty(TaskType.F_LAST_RUN_FINISH_TIMESTAMP);
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    public void setLastRunFinishTimestamp(Long value) {
        setProperty(TaskType.F_LAST_RUN_FINISH_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    @Override
    public Long getCompletionTimestamp() {
        XMLGregorianCalendar gc = getProperty(TaskType.F_COMPLETION_TIMESTAMP);
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    @SuppressWarnings("unused")
    public void setCompletionTimestamp(Long value) {
        setProperty(TaskType.F_COMPLETION_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    @Override
    public Duration getCleanupAfterCompletion() {
        return getProperty(TaskType.F_CLEANUP_AFTER_COMPLETION);
    }

    @Override
    public void setCleanupAfterCompletion(Duration duration) {
        setProperty(TaskType.F_CLEANUP_AFTER_COMPLETION, duration);
    }

    @Override
    public Long getNextRunStartTime(OperationResult result) {
        return taskManager.getNextRunStartTime(getOid(), result);
    }

    public TaskHandler getHandler() {
        return beans.handlerRegistry.getHandler(
                getHandlerUri());
    }

    public String getChannelFromHandler() {
        TaskHandler h = getHandler();
        return h != null ? h.getDefaultChannel() : null;
    }

    @Override
    public void addArchetypeInformation(@NotNull String archetypeOid) {
        addArchetypeInformationInternal(archetypeOid, true);
    }

    private void addArchetypeInformationInternal(
            @NotNull String archetypeOid,
            boolean assertNoExistingArchetypes) {
        synchronized (prismAccess) {
            var existingArchetypeOids = ObjectTypeUtil.getAssignedArchetypeOids(taskPrism.asObjectable());
            if (assertNoExistingArchetypes) {
                if (!existingArchetypeOids.isEmpty()) {
                    throw new IllegalStateException(
                            "Couldn't add archetype %s because there is already one: %s; in %s"
                                    .formatted(archetypeOid, existingArchetypeOids, this));
                }
            } else {
                // We only check that this particular archetype is not already present.
                if (existingArchetypeOids.contains(archetypeOid)) {
                    LOGGER.trace("Archetype {} is already set in {}", archetypeOid, this);
                    return;
                }
            }
            addContainerable(TaskType.F_ASSIGNMENT,
                    ObjectTypeUtil.createAssignmentTo(archetypeOid, ObjectTypes.ARCHETYPE));
            addReferencable(TaskType.F_ROLE_MEMBERSHIP_REF,
                    ObjectTypeUtil.createObjectRef(archetypeOid, ObjectTypes.ARCHETYPE));
            addReferencable(TaskType.F_ARCHETYPE_REF,
                    ObjectTypeUtil.createObjectRef(archetypeOid, ObjectTypes.ARCHETYPE));
        }
    }

    @Override
    public void addArchetypeInformationIfMissing(@NotNull String archetypeOid) {
        synchronized (prismAccess) {
            List<ObjectReferenceType> existingArchetypes = taskPrism.asObjectable().getArchetypeRef();
            if (existingArchetypes.isEmpty()) {
                addArchetypeInformationInternal(archetypeOid, false);
            }
        }
    }

    // todo thread safety (creating a clone?)
    @Override
    public TaskActivityStateType getWorkState() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getActivityState();
        }
    }

    @Override
    public TaskActivityStateType getActivitiesStateOrClone() {
        return getContainerableOrClone(TaskType.F_ACTIVITY_STATE);
    }

    public TaskUnpauseActionType getUnpauseAction() {
        return getProperty(TaskType.F_UNPAUSE_ACTION);
    }

    public TaskExecutionStateType getStateBeforeSuspend() {
        return getProperty(TaskType.F_STATE_BEFORE_SUSPEND);
    }

    public TaskSchedulingStateType getSchedulingStateBeforeSuspend() {
        return getProperty(TaskType.F_SCHEDULING_STATE_BEFORE_SUSPEND);
    }

    @Override
    public OperationStatsType getAggregatedLiveOperationStats() {
        return statistics.getAggregatedOperationStats(emptyList());
    }

    @Override
    public String getVersion() {
        synchronized (prismAccess) {
            return taskPrism.getVersion();
        }
    }

    @Override
    public boolean hasAssignments() {
        synchronized (prismAccess) {
            return !taskPrism.asObjectable().getAssignment().isEmpty();
        }
    }

    @Override
    public ObjectReferenceType getOwnerRef() {
        synchronized (prismAccess) {
            return cloneIfRunning(taskPrism.asObjectable().getOwnerRef());
        }
    }

    @NotNull
    @Override
    public Collection<String> getCachingProfiles() {
        TaskExecutionEnvironmentType executionEnvironment = getExecutionEnvironment();
        return executionEnvironment != null ? Collections.unmodifiableCollection(executionEnvironment.getCachingProfile()) : emptySet();
    }

    public String getOperationResultHandlingStrategyName() {
        TaskExecutionEnvironmentType executionEnvironment = getExecutionEnvironment();
        return executionEnvironment != null ? executionEnvironment.getOperationResultHandlingStrategy() : null;
    }

    @Override
    public TaskExecutionEnvironmentType getExecutionEnvironment() {
        return getContainerableOrClone(TaskType.F_EXECUTION_ENVIRONMENT);
    }

    @Override
    public void setExecutionEnvironment(TaskExecutionEnvironmentType value) {
        setContainerable(TaskType.F_EXECUTION_ENVIRONMENT, value);
    }

    //endregion

    //region Tracing

    @Override
    public boolean isTracingRequestedFor(@NotNull TracingRootType point) {
        if (taskManager.isTracingOverridden()) {
            return taskManager.getGlobalTracingRequestedFor().contains(point);
        } else {
            return tracingRequestedFor.contains(point);
        }
    }

    @Override
    public @NotNull Collection<TracingRootType> getTracingRequestedFor() {
        return tracingRequestedFor;
    }

    @Override
    public void setTracingRequestedFor(@NotNull Collection<TracingRootType> points) {
        tracingRequestedFor.clear();
        tracingRequestedFor.addAll(points);
    }

    @Override
    public void addTracingRequest(TracingRootType point) {
        tracingRequestedFor.add(point);
    }

    @Override
    public void removeTracingRequest(TracingRootType point) {
        tracingRequestedFor.remove(point);
    }

    @Override
    public void removeTracingRequests() {
        tracingRequestedFor.clear();
    }

    @Override
    public TracingProfileType getTracingProfile() {
        if (taskManager.isTracingOverridden()) {
            return taskManager.getGlobalTracingProfile();
        } else {
            return tracingProfile;
        }
    }

    @Override
    public void setTracingProfile(TracingProfileType tracingProfile) {
        this.tracingProfile = tracingProfile;
    }
    //endregion

    //region ConnId listeners
    @Override
    public void onConnIdOperationStart(@NotNull ConnIdOperation operation) {
        connIdOperationsListeners.forEach(l -> l.onConnIdOperationStart(operation));
    }

    @Override
    public void onConnIdOperationEnd(@NotNull ConnIdOperation operation) {
        updateConnIdStatistics(operation);
        connIdOperationsListeners.forEach(l -> l.onConnIdOperationEnd(operation));
    }

    private void updateConnIdStatistics(@NotNull ConnIdOperation operation) {
        statistics.recordProvisioningOperation(operation);
    }

    @Override
    public void onConnIdOperationSuspend(@NotNull ConnIdOperation operation) {
        connIdOperationsListeners.forEach(l -> l.onConnIdOperationSuspend(operation));
    }

    @Override
    public void onConnIdOperationResume(@NotNull ConnIdOperation operation) {
        connIdOperationsListeners.forEach(l -> l.onConnIdOperationResume(operation));
    }

    @Override
    public void registerConnIdOperationsListener(@NotNull ConnIdOperationsListener listener) {
        connIdOperationsListeners.add(listener);
    }

    @Override
    public void unregisterConnIdOperationsListener(@NotNull ConnIdOperationsListener listener) {
        connIdOperationsListeners.remove(listener);
    }
    //endregion

    //region More complex processing: Refresh, subtasks, path to root, self reference
    @Override
    public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        if (!isPersistent()) {
            // Nothing to do for transient tasks
            return;
        }
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "refresh");
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        try {
            if (this instanceof RunningTask) {
                // For running tasks we do NOT update the operation result from repo. The running task itself keeps the most
                // current value of it. Instead, we update the result in prism from the current value. (Although it may not
                // be strictly necessary. We hope it won't eat too much memory and CPU cycles. We could get rid of it eventually.)
                this.taskPrism = beans.taskRetriever.getRepoObjectWithoutResult(getOid(), result);
                updateTaskPrismResult();
            } else {
                // Here we conservatively fetch the result. In the future we could optimize this a bit, avoiding result
                // fetching when not strictly necessary. But it seems that it needs to be fetched most of the time.
                this.taskPrism = beans.taskRetriever.getRepoObjectWithResult(getOid(), result);
                updateTaskResult();
            }
            setDefaults();
            checkOwnerRefPresent();
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    // TODO move to task instantiator?
    @Override
    public TaskQuartzImpl createSubtask() {
        TaskQuartzImpl sub = beans.taskInstantiator.createTaskInstance(null);
        sub.setParent(this.getTaskIdentifier());
        sub.setOwnerRef(this.getOwnerRef());
        sub.setChannel(this.getChannel());
        sub.setExecutionState(TaskExecutionStateType.RUNNABLE);
        sub.setSchedulingState(TaskSchedulingStateType.READY);
        LOGGER.trace("New subtask {} has been created.", sub.getTaskIdentifier());
        return sub;
    }

    @Override
    @NotNull
    public List<TaskQuartzImpl> listSubtasks(OperationResult parentResult) throws SchemaException {
        return listSubtasks(false, parentResult);
    }

    @NotNull
    @Override
    public List<TaskQuartzImpl> listSubtasks(boolean persistentOnly, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listSubtasks");
        result.addParam("persistentOnly", persistentOnly);
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        try {
            return beans.taskRetriever.listSubtasks(this, persistentOnly, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public List<TaskQuartzImpl> listSubtasksDeeply(OperationResult result) throws SchemaException {
        return listSubtasksDeeply(false, result);
    }

    @Override
    public @NotNull List<TaskQuartzImpl> listSubtasksDeeply(boolean persistentOnly, OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listSubtasksDeeply");
        result.addParam("persistentOnly", persistentOnly);
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        try {
            return beans.taskRetriever.listSubtasksDeeply(this, persistentOnly, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public List<TaskQuartzImpl> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listPrerequisiteTasks");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        try {
            return beans.taskRetriever.listPrerequisiteTasks(this, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public @NotNull ParentAndRoot getParentAndRoot(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        argCheck(isPersistent(), "Couldn't determine parent and root for non-persistent, non-running task: %s", this);
        return ParentAndRoot.fromPath(
                getPathToRootTask(result));
    }

    @Override
    public List<Task> getPathToRootTask(OperationResult result) throws SchemaException {
        List<Task> allTasksToRoot = new ArrayList<>();
        TaskQuartzImpl current = this;
        for (;;) {
            checkNoCycle(allTasksToRoot, current);
            allTasksToRoot.add(current);

            TaskQuartzImpl parent = current.getParentTaskSafe(result);
            if (parent == null) {
                return allTasksToRoot;
            } else {
                current = parent;
            }
        }
    }

    private static void checkNoCycle(List<Task> path, TaskQuartzImpl newTask) {
        for (Task task : path) {
            String taskIdentifier = task.getTaskIdentifier();
            if (taskIdentifier != null && taskIdentifier.equals(newTask.getTaskIdentifier())) {
                throw new IllegalStateException("Cycle in the task tree: " + path + " vs " + newTask);
            }
        }
    }

    private TaskQuartzImpl getParentTaskSafe(OperationResult parentResult) throws SchemaException {
        if (getParent() == null) {
            return null;
        }

        // TODO operation result handling here?
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "getParentTaskSafe");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        try {
            TaskQuartzImpl parent = getParentTask(result);
            result.recordSuccess();
            return parent;
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Cannot find parent identified by {}, {}", getParent(), e.getMessage(), e);
            result.recordFatalError("Cannot find parent identified by " + getParent() + ". Reason: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public @NotNull ObjectReferenceType getSelfReference() {
        if (getOid() != null) {
            return new ObjectReferenceType()
                    .type(TaskType.COMPLEX_TYPE)
                    .oid(getOid())
                    .relation(beans.prismContext.getDefaultRelation())
                    .targetName(getName());
        } else {
            throw new IllegalStateException("Reference cannot be created for a transient task: " + this);
        }
    }

    @Override
    public @NotNull ObjectReferenceType getSelfReferenceFull() {
        if (getOid() != null) {
            return ObjectTypeUtil.createObjectRefWithFullObject(
                    getRawTaskObjectClonedIfNecessary());
        } else {
            throw new IllegalStateException("Reference cannot be created for a transient task: " + this);
        }
    }

    public void addSubtask(TaskType subtaskBean) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().getSubtaskRef().add(ObjectTypeUtil.createObjectRefWithFullObject(subtaskBean));
        }
    }
    //endregion

    //region Trivia: dump, toString, equals, hashCode

    /**
     * FIXME: Not thread-safe because of taskResult access.
     */
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Task(").append(getClass().getSimpleName()).append(")\n");
        DebugUtil.debugDumpLabelLn(sb, "prism", indent + 1);
        synchronized (prismAccess) {
            sb.append(taskPrism.debugDump(indent + 2));
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToStringLn(sb, "persistenceStatus", getPersistenceStatus(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "taskResult", taskResult, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "pendingModifications", new ArrayList<>(pendingModifications), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "recreateQuartzTrigger", recreateQuartzTrigger, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Task(id:" + getTaskIdentifier() + ", name:" + getName() + ", oid:" + getOid() + ")";
    }

    @Override
    public int hashCode() {
        synchronized (prismAccess) {
            return taskPrism.hashCode();
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        synchronized (prismAccess) {
            if (this == obj) { return true; }
            if (obj == null) { return false; }
            if (getClass() != obj.getClass()) { return false; }
            TaskQuartzImpl other = (TaskQuartzImpl) obj;
            if (taskResult == null) {
                if (other.taskResult != null) { return false; }
            } else if (!taskResult.equals(other.taskResult)) { return false; }
            if (taskPrism == null) {
                if (other.taskPrism != null) { return false; }
            } else if (!taskPrism.equals(other.taskPrism)) { return false; }
            return true;
        }
    }
    //endregion

    //region Statistics collection

    @Override
    public void recordStateMessage(String message) {
        statistics.recordState(message);
    }

    @Override
    public void recordNotificationOperation(String transportName, boolean success, long duration) {
        statistics.recordNotificationOperation(transportName, success, duration);
    }

    @Override
    public void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName,
            long duration) {
        statistics.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, duration);
    }

    @Override
    public @NotNull Operation recordIterativeOperationStart(@NotNull IterativeOperationStartInfo info) {
        ExecutionSupport executionSupport = getExecutionSupport();
        if (executionSupport != null) {
            return executionSupport.recordIterativeOperationStart(info);
        } else {
            return new DummyOperationImpl(info);
        }
    }

    @Override
    public void onSynchronizationStart(@Nullable String processingIdentifier, @Nullable String shadowOid,
            @Nullable SynchronizationSituationType situation) {
        if (synchronizationStatisticsCollector != null) {
            synchronizationStatisticsCollector.onSynchronizationStart(processingIdentifier, shadowOid, situation);
        }
    }

    @Override
    public void onSynchronizationExclusion(@Nullable String processingIdentifier,
            @NotNull SynchronizationExclusionReasonType exclusionReason) {
        if (synchronizationStatisticsCollector != null) {
            synchronizationStatisticsCollector.onSynchronizationExclusion(processingIdentifier, exclusionReason);
        }
    }

    @Override
    public void onSynchronizationSituationChange(@Nullable String processingIdentifier,
            @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
        if (synchronizationStatisticsCollector != null) {
            synchronizationStatisticsCollector.onSynchronizationSituationChange(processingIdentifier, shadowOid, situation);
        }
    }

    @Override
    public void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
            ChangeType changeType, String channel, Throwable exception) {
        LOGGER.trace("recordObjectActionExecuted: {} {} in {}", changeType, objectDisplayName, this);
        if (actionsExecutedCollector != null) {
            actionsExecutedCollector.recordActionExecuted(
                    objectName, objectDisplayName, objectType, objectOid, changeType, channel, exception);
        }
    }

    @Override
    public void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception) {
        LOGGER.trace("recordObjectActionExecuted: {} {} in {}", changeType, object, this);
        if (actionsExecutedCollector != null) {
            actionsExecutedCollector.recordActionExecuted(object, null, null, changeType, getChannel(), exception);
        }
    }

    @Override
    public <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
            String defaultOid, ChangeType changeType, String channel, Throwable exception) {
        LOGGER.trace("recordObjectActionExecuted: {} {} in {}", changeType, object, this);
        if (actionsExecutedCollector != null) {
            actionsExecutedCollector.recordActionExecuted(object, objectTypeClass, defaultOid, changeType,
                    channel, exception);
        }
    }
    //endregion

    //region Misc
    @Override
    public void startCollectingSynchronizationStatistics(SynchronizationStatisticsCollector collector) {
        stateCheck(synchronizationStatisticsCollector == null, "Sync statistics collector already set in %s", this);
        synchronizationStatisticsCollector = collector;
    }

    @Override
    public void stopCollectingSynchronizationStatistics(@NotNull QualifiedItemProcessingOutcomeType outcome) {
        stateCheck(synchronizationStatisticsCollector != null, "Sync statistics collector not set in %s", this);
        synchronizationStatisticsCollector.stop(outcome);
        synchronizationStatisticsCollector = null;
    }

    @Override
    public void startCollectingActionsExecuted(ActionsExecutedCollector collector) {
        stateCheck(actionsExecutedCollector == null, "Actions executed collector already set in %s", this);
        actionsExecutedCollector = collector;
    }

    @Override
    public void stopCollectingActionsExecuted() {
        stateCheck(actionsExecutedCollector != null, "Actions executed collector not set in %s", this);
        actionsExecutedCollector.stop();
        actionsExecutedCollector = null;
    }

    @NotNull
    @Override
    public TaskExecutionMode getExecutionMode() {
        return executionMode;
    }

    public @NotNull TaskExecutionMode setExecutionMode(@NotNull TaskExecutionMode executionMode) {
        TaskExecutionMode oldMode = this.executionMode;
        this.executionMode = Objects.requireNonNull(executionMode);
        return oldMode;
    }

    @Override
    public SimulationTransaction setSimulationTransaction(SimulationTransaction newTransaction) {
        var oldTransaction = simulationTransaction;
        simulationTransaction = newTransaction;
        return oldTransaction;
    }

    @Override
    public @Nullable SimulationTransaction getSimulationTransaction() {
        return simulationTransaction;
    }

    //endregion
}
