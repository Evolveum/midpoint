/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Collections.*;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_MODEL_OPERATION_CONTEXT;

import java.util.*;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
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
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Implementation of a Task.
 * <p>
 * A few notes about concurrency:
 * <p>
 * This class is a frequent source of concurrency-related issues: see e.g. MID-3954, MID-4088, MID-5111, MID-5113,
 * MID-5131, MID-5135. Therefore we decided to provide more explicit synchronization to it starting in midPoint 4.0.
 * There are three synchronization objects:
 * - PRISM_ACCESS: synchronizes access to the prism object (that is not thread-safe by itself; and that caused all mentioned issues)
 * - QUARTZ_ACCESS: synchronizes execution of Quartz-related actions
 * - pendingModification: synchronizes modifications queue
 * - HANDLER_URI_STACK: manipulation of the URI stack (probably obsolete as URI stack is not used much)
 * <p>
 * Note that PRISM_ACCESS could be replaced by taskPrism object; but unfortunately taskPrism is changed in updateTaskInstance().
 * Quartz and Pending modification synchronization is perhaps not so useful, because we do not expect two threads to modify
 * a task at the same time. But let's play it safe.
 * <p>
 * PRISM_ACCESS by itself is NOT sufficient, though. TODO explain
 * <p>
 * TODO notes for developers (do not nest synchronization blocks)
 * <p>
 * Order of synchronization:
 * 1) HANDLER_URI_STACK
 * 2) QUARTZ_ACCESS
 * 3) pendingModification
 * 4) PRISM_ACCESS
 * <p>
 * TODO what about the situation where a task tries to close/suspend itself and (at the same time) task manager tries to do the same?
 * Maybe the task manager should act on a clone of the task
 */
public class TaskQuartzImpl implements InternalTaskInterface {

    private static final int TIGHT_BINDING_INTERVAL_LIMIT = 10;

    private final Object quartzAccess = new Object();
    private final Object prismAccess = new Object();
    private final Object handlerUriStack = new Object();

    @NotNull protected final Statistics statistics;

    private PrismObject<TaskType> taskPrism;

    private PrismObject<UserType> requestee; // temporary information

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
     */
    protected OperationResult taskResult;

    @NotNull protected final TaskManagerQuartzImpl taskManager;
    @NotNull protected final RepositoryService repositoryService;

    /**
     * Whether to recreate quartz trigger on next flushPendingModifications and/or synchronizeWithQuartz.
     */
    private boolean recreateQuartzTrigger = false;

    @NotNull // beware, we still have to synchronize on pendingModifications while iterating over it
    private final List<ItemDelta<?, ?>> pendingModifications = Collections.synchronizedList(new ArrayList<>());

    /**
     * Points where tracing is requested (for this task).
     */
    private final Set<TracingRootType> tracingRequestedFor = new HashSet<>();

    /**
     * The profile to be used for tracing - it is copied into operation result at specified tracing point(s).
     */
    private TracingProfileType tracingProfile;

    private static final Trace LOGGER = TraceManager.getTrace(TaskQuartzImpl.class);

    //region Constructors

    TaskQuartzImpl(@NotNull TaskManagerQuartzImpl taskManager, @NotNull PrismObject<TaskType> taskPrism) {
        this.taskManager = taskManager;
        this.repositoryService = taskManager.getRepositoryService();
        this.taskPrism = taskPrism;
        statistics = new Statistics(taskManager.getPrismContext());
        setDefaults();
        updateTaskResult();
    }

    /**
     * Creates a new task instance i.e. from scratch.
     *
     * @param operationName if null, default op. name will be used
     */
    static TaskQuartzImpl createNew(@NotNull TaskManagerQuartzImpl taskManager, String operationName) {
        TaskType taskBean = new TaskType(taskManager.getPrismContext())
                .taskIdentifier(taskManager.generateTaskIdentifier().toString())
                .executionStatus(TaskExecutionStatusType.RUNNABLE)
                .recurrence(TaskRecurrenceType.SINGLE)
                .progress(0L)
                .result(createTaskResult(operationName));
        return new TaskQuartzImpl(taskManager, taskBean.asPrismObject());
    }

    /**
     * Creates a new task instance from provided task prism object.
     *
     * NOTE: if the result in prism is null, task result will be kept null as well (meaning it was not fetched from the repository).
     */
    static TaskQuartzImpl createFromPrismObject(@NotNull TaskManagerQuartzImpl taskManager, PrismObject<TaskType> taskObject) {
        return new TaskQuartzImpl(taskManager, taskObject);
    }

    private void setDefaults() {
        if (getBinding() == null) {
            setBindingTransient(bindingFromSchedule(getSchedule()));
        }
    }

    //endregion

    //region Result handling
    private void updateTaskResult() {
        synchronized (prismAccess) {
            OperationResultType resultInPrism = taskPrism.asObjectable().getResult();
            if (resultInPrism != null) {
                taskResult = OperationResult.createOperationResult(resultInPrism);
            } else {
                taskResult = null;
            }
        }
    }

    private void updateTaskPrismResult(PrismObject<TaskType> target) {
        synchronized (prismAccess) {
            if (taskResult != null) {
                target.asObjectable().setResult(taskResult.createOperationResultType());
                target.asObjectable().setResultStatus(taskResult.getStatus().createStatusType());
            } else {
                target.asObjectable().setResult(null);
                target.asObjectable().setResultStatus(null);
            }
        }
    }
    //endregion

    //region Main getters and setters

    private boolean isLiveRunningInstance() {
        return this instanceof RunningTask;
    }

    /**
     * TODO TODO TODO (think out better name)
     * Use with care. Never provide to outside world (beyond task manager).
     */
    PrismObject<TaskType> getLiveTaskObjectForNotRunningTasks() {
        if (isLiveRunningInstance()) {
            throw new UnsupportedOperationException("It is not possible to get live task prism object from the running task instance: " + this);
        } else {
            return taskPrism;
        }
    }

    // Use with utmost care! Never provide to outside world (beyond task manager)
    PrismObject<TaskType> getLiveTaskObject() {
        return taskPrism;
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getUpdatedOrClonedTaskObject() {
        if (isLiveRunningInstance()) {
            return getClonedTaskObject();
        } else {
            updateTaskPrismResult(taskPrism);
            return taskPrism;
        }
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getUpdatedTaskObject() {
        if (isLiveRunningInstance()) {
            throw new IllegalStateException("Cannot get task object from live running task instance");
        } else {
            updateTaskPrismResult(taskPrism);
            return taskPrism;
        }
    }

    Task cloneAsStaticTask() {
        return TaskQuartzImpl.createFromPrismObject(taskManager, getClonedTaskObject());
    }

    @NotNull
    @Override
    public PrismObject<TaskType> getClonedTaskObject() {
        synchronized (prismAccess) {
            PrismObject<TaskType> rv = taskPrism.clone();
            updateTaskPrismResult(rv);
            return rv;
        }
    }

    @NotNull RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public boolean isRecreateQuartzTrigger() {
        return recreateQuartzTrigger;
    }

    @Override
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
    public void modify(ItemDelta<?, ?> delta) throws SchemaException {
        addPendingModification(delta);
        synchronized (prismAccess) {
            delta.applyTo(taskPrism);
        }
    }

    @Override
    public void modify(Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
        for (ItemDelta<?, ?> delta : deltas) {
            modify(delta);
        }
    }

    @Override
    public void modifyAndFlush(ItemDelta<?, ?> delta, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        synchronized (pendingModifications) {
            modify(delta);
            flushPendingModifications(parentResult);
        }
    }

    @Override
    public void flushPendingModifications(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        if (isTransient()) {
            synchronized (pendingModifications) {
                pendingModifications.clear();
            }
            return;
        }
        synchronized (pendingModifications) {        // todo perhaps we should put something like this at more places here...
            if (!pendingModifications.isEmpty()) {
                try {
                    repositoryService.modifyObject(TaskType.class, getOid(), pendingModifications, parentResult);
                } finally {     // todo reconsider this (it's not ideal but we need at least to reset pendingModifications to stop repeating applying this change)
                    synchronizeWithQuartzIfNeeded(pendingModifications, parentResult);
                    pendingModifications.clear();
                }
            }
        }
        if (isRecreateQuartzTrigger()) {
            synchronizeWithQuartz(parentResult);
        }
    }

    @Override
    @NotNull
    public Collection<ItemDelta<?, ?>> getPendingModifications() {
        return pendingModifications;
    }

    private void modifyRepository(ItemDelta<?, ?> delta, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        if (delta != null) {
            modifyRepository(singleton(delta), parentResult);
        }
    }

    private void modifyRepository(Collection<ItemDelta<?, ?>> deltas, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        if (isPersistent()) {
            repositoryService.modifyObject(TaskType.class, getOid(), deltas, parentResult);
            synchronizeWithQuartzIfNeeded(deltas, parentResult);
        }
    }

    private void modifyRepository(Collection<ItemDelta<?, ?>> deltas, ModificationPrecondition<TaskType> precondition, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {
        if (isPersistent()) {
            repositoryService.modifyObject(TaskType.class, getOid(), deltas, precondition, null, parentResult);
            synchronizeWithQuartzIfNeeded(deltas, parentResult);
        }
    }
    //endregion

    //region Quartz integration
    void synchronizeWithQuartz(OperationResult parentResult) {
        synchronized (quartzAccess) {
            taskManager.synchronizeTaskWithQuartz(this, parentResult);
            setRecreateQuartzTrigger(false);
        }
    }

    private static final Set<QName> QUARTZ_RELATED_PROPERTIES = new HashSet<>();

    static {
        QUARTZ_RELATED_PROPERTIES.add(TaskType.F_BINDING);
        QUARTZ_RELATED_PROPERTIES.add(TaskType.F_RECURRENCE);
        QUARTZ_RELATED_PROPERTIES.add(TaskType.F_SCHEDULE);
        QUARTZ_RELATED_PROPERTIES.add(TaskType.F_HANDLER_URI);
    }

    private void synchronizeWithQuartzIfNeeded(Collection<ItemDelta<?, ?>> deltas, OperationResult parentResult) {
        synchronized (quartzAccess) {
            if (isRecreateQuartzTrigger()) {
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

    @Nullable <X> PropertyDelta<X> createPropertyDeltaIfPersistent(ItemName name, X value) {
        return isPersistent() ? deltaFactory().property().createReplaceDeltaOrEmptyDelta(
                taskManager.getTaskObjectDefinition(), name, value) : null;
    }

    @Nullable
    private <X extends Containerable> ContainerDelta<X> createContainerDeltaIfPersistent(ItemName name, X value)
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

    //region Getting and setting task properties, references and containers

    private <T> T cloneIfRunning(T value) {
        return isLiveRunningInstance() ? CloneUtil.clone(value) : value;
    }

    private <X> X getProperty(ItemName name) {
        synchronized (prismAccess) {
            PrismProperty<X> property = taskPrism.findProperty(name);
            return property != null ? property.getRealValue() : null;
        }
    }

    private <X> void setProperty(ItemName name, X value) {
        addPendingModification(setPropertyAndCreateDeltaIfPersistent(name, value));
    }

    private <C extends Containerable> C getContainerableOrClone(ItemName name) {
        synchronized (prismAccess) {
            PrismContainer<C> container = taskPrism.findContainer(name);
            return container != null && !container.hasNoValues() ? cloneIfRunning(container.getRealValue()) : null;
        }
    }

    private <X extends Containerable> void setContainerable(ItemName name, X value) {
        try {
            addPendingModification(setContainerableAndCreateDeltaIfPersistent(name, value));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't set the task container '" + name + "': " + e.getMessage(), e);
        }
    }

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

    @SuppressWarnings("SameParameterValue")
    private void setReferenceImmediate(ItemName name, ObjectReferenceType value, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        try {
            modifyRepository(setReferenceAndCreateDeltaIfPersistent(name, value), result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException("Unexpected ObjectAlreadyExistsException while modifying '" + name + "' property: " +
                    ex.getMessage(), ex);
        }
    }

    private ReferenceDelta setReferenceAndCreateDeltaIfPersistent(ItemName name, ObjectReferenceType value) {
        setReferenceTransient(name, value);
        return createReferenceDeltaIfPersistent(name, value);
    }

    /*
     * progress
     */

    @Override
    public long getProgress() {
        return defaultIfNull(getProperty(TaskType.F_PROGRESS), 0L);
    }

    @Override
    public void setProgress(Long value) {
        setProperty(TaskType.F_PROGRESS, value);
    }

    @Override
    public void setProgressImmediate(Long value, OperationResult result) throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_PROGRESS, value, result);
    }

    @Override
    public void setProgressTransient(Long value) {
        setPropertyTransient(TaskType.F_PROGRESS, value);
    }

    /*
     * storedOperationStats
     */

    @Override
    public OperationStatsType getStoredOperationStats() {
        return getContainerableOrClone(TaskType.F_OPERATION_STATS);
    }

    public void setOperationStats(OperationStatsType value) {
        setContainerable(TaskType.F_OPERATION_STATS, value);
    }

    void setOperationStatsTransient(OperationStatsType value) {
        setContainerableTransient(TaskType.F_OPERATION_STATS, value != null ? value.clone() : null);
    }

    /*
     * expectedTotal
     */
    @Override
    @Nullable
    public Long getExpectedTotal() {
        return getProperty(TaskType.F_EXPECTED_TOTAL);
    }

    @Override
    public void setExpectedTotal(Long value) {
        setProperty(TaskType.F_EXPECTED_TOTAL, value);
    }

    @SuppressWarnings("unused")
    public void setExpectedTotalTransient(Long value) {
        setPropertyTransient(TaskType.F_EXPECTED_TOTAL, value);
    }

    @Override
    public void setExpectedTotalImmediate(Long value, OperationResult result) throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_EXPECTED_TOTAL, value, result);
    }

    /*
     * result
     *
     * setters set also result status type!
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

    @Override
    public void setResultImmediate(OperationResult result, OperationResult opResult)
            throws ObjectNotFoundException, SchemaException {
        try {
            modifyRepository(setResultAndPrepareDelta(result), opResult);
            setPropertyImmediate(TaskType.F_RESULT_STATUS, result != null ? result.getStatus().createStatusType() : null, opResult);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
    }

    @Override
    public void setResultTransient(OperationResult result) {
        synchronized (prismAccess) {
            taskResult = result;
            taskPrism.asObjectable().setResult(result != null ? result.createOperationResultType() : null);
            taskPrism.asObjectable().setResultStatus(result != null ? result.getStatus().createStatusType() : null);
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

    /*
     * Handler URI
     */

    @Override
    public String getHandlerUri() {
        return getProperty(TaskType.F_HANDLER_URI);
    }

    @Override
    public void setHandlerUri(String value) {
        setProperty(TaskType.F_HANDLER_URI, value);
    }

    @SuppressWarnings("unused")
    public void setHandlerUriTransient(String value) {
        setPropertyTransient(TaskType.F_HANDLER_URI, value);
    }

    @Override
    public void setHandlerUriImmediate(String value, OperationResult result) throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_HANDLER_URI, value, result);
    }

    /*
     * Other handlers URI stack
     */

    @Override
    public UriStack getOtherHandlersUriStack() {
        synchronized (handlerUriStack) {
            return getProperty(TaskType.F_OTHER_HANDLERS_URI_STACK);
        }
    }

    private void setOtherHandlersUriStack(UriStack value) {
        synchronized (handlerUriStack) {
            setProperty(TaskType.F_OTHER_HANDLERS_URI_STACK, value);
            checkHandlerUriConsistency();
        }
    }

    private UriStackEntry popFromOtherHandlersUriStack() {
        synchronized (handlerUriStack) {
            checkHandlerUriConsistency();
            UriStack stack = getOtherHandlersUriStack();
            if (stack == null || stack.getUriStackEntry().isEmpty()) {
                throw new IllegalStateException("Couldn't pop from OtherHandlersUriStack, because it is null or empty");
            }
            int last = stack.getUriStackEntry().size() - 1;
            UriStackEntry retval = stack.getUriStackEntry().get(last);
            stack.getUriStackEntry().remove(last);
            setOtherHandlersUriStack(stack);
            return retval;
        }
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding) {
        pushHandlerUri(uri, schedule, binding, (Collection<ItemDelta<?, ?>>) null);
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?, ?> delta) {
        pushHandlerUri(uri, schedule, binding, delta != null ? singletonList(delta) : null);
    }

    /**
     * Makes (uri, schedule, binding) the current task properties, and pushes current (uri, schedule, binding, extensionChange)
     * onto the stack.
     *
     * @param uri New Handler URI
     * @param schedule New schedule
     * @param binding New binding
     */
    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, Collection<ItemDelta<?, ?>> extensionDeltas) {
        Validate.notNull(uri);

        synchronized (handlerUriStack) {
            if (binding == null) {
                binding = bindingFromSchedule(schedule);
            }

            checkHandlerUriConsistency();

            if (this.getHandlerUri() != null) {
                UriStack stack = getOtherHandlersUriStack();
                if (stack == null) {
                    stack = new UriStack();
                }

                UriStackEntry entry = new UriStackEntry();
                entry.setHandlerUri(getHandlerUri());
                entry.setRecurrence(getRecurrenceStatus().toTaskType());
                entry.setSchedule(getSchedule());
                entry.setBinding(getBinding().toTaskType());
                if (extensionDeltas != null) {
                    storeExtensionDeltas(entry.getExtensionDelta(), extensionDeltas);
                }
                stack.getUriStackEntry().add(entry);
                setOtherHandlersUriStack(stack);
            }

            setHandlerUri(uri);
            setSchedule(schedule);
            setRecurrenceStatus(recurrenceFromSchedule(schedule));
            setBinding(binding);

            setRecreateQuartzTrigger(true);            // will be applied on modifications save
        }
    }

    // the following methods are static to be sure there are no concurrency issues

    private static void storeExtensionDeltas(List<ItemDeltaType> result, Collection<ItemDelta<?, ?>> extensionDeltas) {
        for (ItemDelta itemDelta : extensionDeltas) {
            Collection<ItemDeltaType> deltaTypes;
            try {
                deltaTypes = DeltaConvertor.toItemDeltaTypes(itemDelta);
            } catch (SchemaException e) {
                throw new SystemException("Unexpected SchemaException when converting extension ItemDelta to ItemDeltaType", e);
            }
            result.addAll(deltaTypes);
        }
    }

    // derives default binding form schedule
    private static TaskBinding bindingFromSchedule(ScheduleType schedule) {
        if (schedule != null && schedule.getInterval() != null && schedule.getInterval() > 0 && schedule.getInterval() <= TIGHT_BINDING_INTERVAL_LIMIT) {
            return TaskBinding.TIGHT;
        } else {
            return TaskBinding.LOOSE;
        }
    }

    private static TaskRecurrence recurrenceFromSchedule(ScheduleType schedule) {
        if (schedule == null) {
            return TaskRecurrence.SINGLE;
        } else if (schedule.getInterval() != null && schedule.getInterval() != 0) {
            return TaskRecurrence.RECURRING;
        } else if (StringUtils.isNotEmpty(schedule.getCronLikePattern())) {
            return TaskRecurrence.RECURRING;
        } else {
            return TaskRecurrence.SINGLE;
        }
    }

    @Override
    public void finishHandler(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        synchronized (handlerUriStack) {
            // let us drop the current handler URI and nominate the top of the other
            // handlers stack as the current one

            LOGGER.trace("finishHandler called for handler URI {}, task {}", this.getHandlerUri(), this);

            checkHandlerUriConsistency();
            UriStack otherHandlersUriStack = getOtherHandlersUriStack();
            if (otherHandlersUriStack != null && !otherHandlersUriStack.getUriStackEntry().isEmpty()) {
                UriStackEntry entry = popFromOtherHandlersUriStack();
                setHandlerUri(entry.getHandlerUri());
                setRecurrenceStatus(entry.getRecurrence() != null ?
                        TaskRecurrence.fromTaskType(entry.getRecurrence()) :
                        recurrenceFromSchedule(entry.getSchedule()));
                setSchedule(entry.getSchedule());
                if (entry.getBinding() != null) {
                    setBinding(TaskBinding.fromTaskType(entry.getBinding()));
                } else {
                    setBinding(bindingFromSchedule(entry.getSchedule()));
                }
                for (ItemDeltaType itemDeltaType : entry.getExtensionDelta()) {
                    ItemDelta itemDelta = DeltaConvertor
                            .createItemDelta(itemDeltaType, TaskType.class, taskManager.getPrismContext());
                    LOGGER.trace("Applying ItemDelta to task extension; task = {}; itemDelta = {}", this, itemDelta.debugDump());
                    modifyExtension(itemDelta);
                }
                setRecreateQuartzTrigger(true);
            } else {
                //setHandlerUri(null);                                                  // we want the last handler to remain set so the task can be revived
                taskManager.closeTaskWithoutSavingState(this,
                        parentResult);            // as there are no more handlers, let us close this task
            }
            try {
                flushPendingModifications(parentResult);
                checkDependentTasksOnClose(parentResult);
            } catch (ObjectAlreadyExistsException ex) {
                throw new SystemException(ex);
            }
            LOGGER.trace("finishHandler: new current handler uri = {}, new number of handlers = {}", getHandlerUri(),
                    getHandlersCount());
        }
    }

    @Override
    public void checkDependentTasksOnClose(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (getExecutionStatus() != TaskExecutionStatus.CLOSED) {
            return;
        }
        for (Task dependent : listDependents(result)) {
            ((InternalTaskInterface) dependent).checkDependencies(result);
        }
        Task parentTask = getParentTask(result);
        if (parentTask != null) {
            ((InternalTaskInterface) parentTask).checkDependencies(result);
        }
    }

    @Override
    public void checkDependencies(OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (getExecutionStatus() != TaskExecutionStatus.WAITING || getWaitingReason() != TaskWaitingReason.OTHER_TASKS) {
            return;
        }

        List<Task> dependencies = listSubtasks(result);
        dependencies.addAll(listPrerequisiteTasks(result));

        LOGGER.trace("Checking {} dependencies for waiting task {}", dependencies.size(), this);

        for (Task dependency : dependencies) {
            if (!dependency.isClosed()) {
                LOGGER.trace("Dependency {} of {} is not closed (status = {})", dependency, this, dependency.getExecutionStatus());
                return;
            }
        }
        LOGGER.trace("All dependencies of {} are closed, unpausing the task", this);
        try {
            taskManager.unpauseTask(this, result);
        } catch (PreconditionViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Task cannot be unpaused because it is no longer in WAITING state -- ignoring", e, this);
        }
    }

    int getHandlersCount() {
        synchronized (handlerUriStack) {
            checkHandlerUriConsistency();
            int main = getHandlerUri() != null ? 1 : 0;
            UriStack otherHandlersUriStack = getOtherHandlersUriStack();
            int others = otherHandlersUriStack != null ? otherHandlersUriStack.getUriStackEntry().size() : 0;
            return main + others;
        }
    }

    private boolean isOtherHandlersUriStackEmpty() {
        UriStack stack = getOtherHandlersUriStack();
        return stack == null || stack.getUriStackEntry().isEmpty();
    }

    private void checkHandlerUriConsistency() {
        if (getHandlerUri() == null && !isOtherHandlersUriStackEmpty()) {
            throw new IllegalStateException(
                    "Handler URI is null but there is at least one 'other' handler (otherHandlerUriStack size = "
                            + getOtherHandlersUriStack().getUriStackEntry().size() + ")");
        }
    }


    /*
     * Persistence status
     */

    @Override
    public TaskPersistenceStatus getPersistenceStatus() {
        return StringUtils.isEmpty(getOid()) ? TaskPersistenceStatus.TRANSIENT : TaskPersistenceStatus.PERSISTENT;
    }

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

    /*
     * Oid
     */

    @Override
    public String getOid() {
        synchronized (prismAccess) {
            return taskPrism.getOid();
        }
    }

    @Override
    public synchronized void setOid(String oid) {
        synchronized (prismAccess) {
            taskPrism.setOid(oid);
        }
    }

    // obviously, there are no "persistent" versions of setOid

    /*
     * Task identifier (again, without "persistent" versions)
     */

    @Override
    public String getTaskIdentifier() {
        return getProperty(TaskType.F_TASK_IDENTIFIER);
    }

    public void setTaskIdentifier(String value) {
        setProperty(TaskType.F_TASK_IDENTIFIER, value);
    }

    /*
     * Execution status
     *
     * IMPORTANT: do not set this attribute explicitly (due to the need of synchronization with Quartz scheduler).
     * Use task life-cycle methods, like close(), suspendTask(), resumeTask(), and so on.
     */

    @Override
    public TaskExecutionStatus getExecutionStatus() {
        return TaskExecutionStatus.fromTaskType(getProperty(TaskType.F_EXECUTION_STATUS));
    }

    public void setExecutionStatus(@NotNull TaskExecutionStatus value) {
        setProperty(TaskType.F_EXECUTION_STATUS, value.toTaskType());
    }

    @Override
    public void setInitialExecutionStatus(@NotNull TaskExecutionStatus value) {
        if (isPersistent()) {
            throw new IllegalStateException("Initial execution state can be set only on transient tasks.");
        }
        setProperty(TaskType.F_EXECUTION_STATUS, value.toTaskType());
    }

    private void setExecutionStatusTransient(@NotNull TaskExecutionStatus executionStatus) {
        setPropertyTransient(TaskType.F_EXECUTION_STATUS, executionStatus.toTaskType());
    }

    @Override
    public void setExecutionStatusImmediate(TaskExecutionStatus value, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_EXECUTION_STATUS, value.toTaskType(), result);
    }

    @Override
    public void setExecutionStatusImmediate(TaskExecutionStatus value, TaskExecutionStatusType previousValue,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {
        try {
            modifyRepository(singleton(setExecutionStatusAndPrepareDelta(value)),
                    t -> previousValue == null || previousValue == t.asObjectable().getExecutionStatus(), parentResult);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
    }

    private PropertyDelta<?> setExecutionStatusAndPrepareDelta(TaskExecutionStatus value) {
        setExecutionStatusTransient(value);
        return createPropertyDeltaIfPersistent(TaskType.F_EXECUTION_STATUS, value.toTaskType());
    }

    @Override
    public void makeRunnable() {
        if (!isTransient()) {
            throw new IllegalStateException("makeRunnable can be invoked only on transient tasks; task = " + this);
        }
        setExecutionStatus(TaskExecutionStatus.RUNNABLE);
    }

    @Override
    public void makeWaiting() {
        setExecutionStatus(TaskExecutionStatus.WAITING);
    }

    @Override
    public void makeWaiting(TaskWaitingReason reason) {
        makeWaiting();
        setWaitingReason(reason);
    }

    @Override
    public void makeWaiting(TaskWaitingReason reason, TaskUnpauseActionType unpauseAction) {
        makeWaiting(reason);
        setUnpauseAction(unpauseAction);
    }

    public boolean isClosed() {
        return getExecutionStatus() == TaskExecutionStatus.CLOSED;
    }

    /*
     * Waiting reason
     */

    @Override
    public TaskWaitingReason getWaitingReason() {
        return TaskWaitingReason.fromTaskType(getProperty(TaskType.F_WAITING_REASON));
    }

    public void setWaitingReason(TaskWaitingReason value) {
        setProperty(TaskType.F_WAITING_REASON, value != null ? value.toTaskType() : null);
    }

    @Override
    public void setWaitingReasonImmediate(TaskWaitingReason value, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_WAITING_REASON, value != null ? value.toTaskType() : null, result);
    }

    /*
     * Unpause action
     */

    private void setUnpauseAction(TaskUnpauseActionType value) {
        setProperty(TaskType.F_UNPAUSE_ACTION, value);
    }

    // "safe" method
    @Override
    public void startWaitingForTasksImmediate(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (getExecutionStatus() != TaskExecutionStatus.WAITING) {
            throw new IllegalStateException(
                    "Task that has to start waiting for tasks should be in WAITING state (it is in " + getExecutionStatus()
                            + " now)");
        }
        setWaitingReasonImmediate(TaskWaitingReason.OTHER_TASKS, result);
        checkDependencies(result);
    }

    /*
     * Recurrence status
     */

    public TaskRecurrence getRecurrenceStatus() {
        return TaskRecurrence.fromTaskType(getProperty(TaskType.F_RECURRENCE));
    }

    @Override
    public boolean isSingle() {
        return getRecurrenceStatus() == TaskRecurrence.SINGLE;
    }

    @Override
    public boolean isRecurring() {
        return getRecurrenceStatus() == TaskRecurrence.RECURRING;
    }

    void setRecurrenceStatus(@NotNull TaskRecurrence value) {
        setProperty(TaskType.F_RECURRENCE, value.toTaskType());
    }

    @SuppressWarnings("SameParameterValue")
    private void setRecurrenceStatusTransient(TaskRecurrence value) {
        setPropertyTransient(TaskType.F_RECURRENCE, value.toTaskType());
    }

    @Override
    public void makeSingle() {
        setRecurrenceStatus(TaskRecurrence.SINGLE);
        setSchedule(new ScheduleType());
    }

    @Override
    public void makeSingle(ScheduleType schedule) {
        setRecurrenceStatus(TaskRecurrence.SINGLE);
        setSchedule(schedule);
    }

    @Override
    public void makeRecurring(ScheduleType schedule) {
        setRecurrenceStatus(TaskRecurrence.RECURRING);
        setSchedule(schedule);
    }

    @Override
    public void makeRecurringSimple(int interval) {
        setRecurrenceStatus(TaskRecurrence.RECURRING);
        setSchedule(new ScheduleType().interval(interval));
    }

    @Override
    public void makeRecurringCron(String cronLikeSpecification) {
        setRecurrenceStatus(TaskRecurrence.RECURRING);
        setSchedule(new ScheduleType().cronLikePattern(cronLikeSpecification));
    }

    /*
     * executionConstraints
     */

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
                if (sg.getGroup()
                        != null) {    // shouldn't occur but it's a user configurable field, so be prepared for the worst
                    rv.put(sg.getGroup(), sg.getGroupTaskLimit());
                }
            }
            return rv;
        }
    }

    /*
     * Schedule
     */

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

    public void setSchedule(ScheduleType value) {
        setContainerable(TaskType.F_SCHEDULE, value);
    }

    @SuppressWarnings("unused")
    private void setScheduleTransient(ScheduleType value) {
        setContainerableTransient(TaskType.F_SCHEDULE, value);
    }

    /*
     * ThreadStopAction
     */

    @Override
    public ThreadStopActionType getThreadStopAction() {
        return getProperty(TaskType.F_THREAD_STOP_ACTION);
    }

    @Override
    public void setThreadStopAction(ThreadStopActionType value) {
        setProperty(TaskType.F_THREAD_STOP_ACTION, value);
    }

    @Override
    public boolean isResilient() {
        ThreadStopActionType action = getThreadStopAction();
        return action == null || action == ThreadStopActionType.RESCHEDULE || action == ThreadStopActionType.RESTART;
    }

    /*
     * Binding
     */

    @Override
    public TaskBinding getBinding() {
        return TaskBinding.fromTaskType(getProperty(TaskType.F_BINDING));
    }

    @Override
    public boolean isTightlyBound() {
        return getBinding() == TaskBinding.TIGHT;
    }

    @Override
    public boolean isLooselyBound() {
        return getBinding() == TaskBinding.LOOSE;
    }

    @Override
    public void setBinding(@NotNull TaskBinding value) {
        setProperty(TaskType.F_BINDING, value.toTaskType());
    }

    @Override
    public void setBindingImmediate(@NotNull TaskBinding value, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_BINDING, value.toTaskType(), result);
    }

    @SuppressWarnings("SameParameterValue")
    private void setBindingTransient(TaskBinding value) {
        setPropertyTransient(TaskType.F_BINDING, value.toTaskType());
    }

    /*
     * Owner
     */

    @Override
    public PrismObject<? extends FocusType> getOwner() {
        PrismReferenceValue ownerRef = getReferenceValue(TaskType.F_OWNER_REF);
        //noinspection unchecked
        return ownerRef != null ? ownerRef.getObject() : null;
    }

    @Override
    public void setOwner(PrismObject<? extends FocusType> owner) {
        if (isPersistent()) {
            throw new IllegalStateException("setOwner method can be called only on transient tasks!");
        }

        synchronized (prismAccess) {
            PrismReference ownerRef;
            try {
                ownerRef = taskPrism.findOrCreateReference(TaskType.F_OWNER_REF);
            } catch (SchemaException e) {
                throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
            }
            ownerRef.getValue().setObject(owner);
        }
    }

    void resolveOwnerRef(OperationResult result) throws SchemaException {
        PrismReferenceValue ownerRef = getReferenceValue(TaskType.F_OWNER_REF);
        if (ownerRef == null) {
            throw new SchemaException("Task " + getOid() + " does not have an owner (missing ownerRef)");
        }
        try {
            PrismObject<UserType> owner = repositoryService.getObject(UserType.class, ownerRef.getOid(), null, result);
            synchronized (prismAccess) {
                ownerRef.setObject(owner);
            }
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logExceptionAsWarning(LOGGER, "The owner of task {} cannot be found (owner OID: {})", e, getOid(),
                    ownerRef.getOid());
        }
    }

    /*
     * channel
     */

    @Override
    public String getChannel() {
        return getProperty(TaskType.F_CHANNEL);
    }

    @Override
    public void setChannel(String value) {
        setProperty(TaskType.F_CHANNEL, value);
    }

    @Override
    public void setChannelImmediate(String value, OperationResult result) throws ObjectNotFoundException, SchemaException {
        setPropertyImmediate(TaskType.F_CHANNEL, value, result);
    }

    /*
     * Object
     */

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
    public void setObjectRefImmediate(ObjectReferenceType value, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        setReferenceImmediate(TaskType.F_OBJECT_REF, value, result);
    }

    @SuppressWarnings("unused")
    private void setObjectRefTransient(ObjectReferenceType value) {
        setReferenceTransient(TaskType.F_OBJECT_REF, value);
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
                //noinspection CastCanBeRemovedNarrowingVariableType,unchecked
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
            PrismObject<T> object = repositoryService.getObject(type, objectRef.getOid(), null, result);
            synchronized (prismAccess) {
                objectRef.asReferenceValue().setObject(object);
                result.recordSuccess();
                return object.clone();
            }
        } catch (ObjectNotFoundException ex) {
            result.recordFatalError("Object not found", ex);
            throw ex;
        } catch (SchemaException ex) {
            result.recordFatalError("Schema error", ex);
            throw ex;
        }
    }

    @Override
    public void setObjectTransient(PrismObject object) {
        synchronized (prismAccess) {
            if (object == null) {
                PrismReference objectRef = taskPrism.findReference(TaskType.F_OBJECT_REF);
                if (objectRef != null) {
                    taskPrism.getValue().remove(objectRef);
                }
            } else {
                PrismReference objectRef;
                try {
                    objectRef = taskPrism.findOrCreateReference(TaskType.F_OBJECT_REF);
                } catch (SchemaException e) {
                    // This should not happen
                    throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
                }
                objectRef.getValue().setObject(object.clone());
            }
        }
    }

    /*
     * Name
     */

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
    public void setNameImmediate(PolyStringType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        modifyRepository(setNameAndPrepareDelta(value), parentResult);
    }

    void setNameTransient(PolyStringType name) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().setName(name);
        }
    }

    private PropertyDelta<?> setNameAndPrepareDelta(PolyStringType value) {
        setNameTransient(value);
        return createPropertyDeltaIfPersistent(TaskType.F_NAME, value.toPolyString());
    }

    /*
     * Description
     */

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

    @SuppressWarnings("unused")
    public void setDescriptionTransient(String value) {
        setPropertyTransient(TaskType.F_DESCRIPTION, value);
    }

    /*
     *  policyRule
     */

    /**
     * BEWARE: this returns a clone
     */
    @Override
    public PolicyRuleType getPolicyRule() {
        synchronized (prismAccess) {
            return cloneIfRunning(taskPrism.asObjectable().getPolicyRule());
        }
    }

    /*
     * Parent
     */

    @Override
    public String getParent() {
        return getProperty(TaskType.F_PARENT);
    }

    @Override
    public Task getParentTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (getParent() == null) {
            return null;
        } else {
            return taskManager.getTaskByIdentifier(getParent(), result);
        }
    }

    public void setParent(String value) {
        setProperty(TaskType.F_PARENT, value);
    }

    /*
     * Dependents
     */

    @Override
    public List<String> getDependents() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getDependent();
        }
    }

    @Override
    public List<Task> listDependents(OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listDependents");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        List<String> dependentsIdentifiers = getDependents();
        List<Task> dependents = new ArrayList<>(dependentsIdentifiers.size());
        for (String dependentId : dependentsIdentifiers) {
            try {
                dependents.add(taskManager.getTaskByIdentifier(dependentId, result));
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("Dependent task {} was not found. Probably it was not yet stored to repo; we just ignore it.",
                        dependentId);
            }
        }

        result.recordSuccessIfUnknown();
        return dependents;
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

    @Override
    public void deleteDependent(String value) {
        addPendingModification(deleteDependentAndPrepareDelta(value));
    }

    private void deleteDependentTransient(String name) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().getDependent().remove(name);
        }
    }

    private PropertyDelta<?> deleteDependentAndPrepareDelta(String value) {
        deleteDependentTransient(value);
        return isPersistent() ? deltaFactory().property().createDeleteDelta(
                taskManager.getTaskObjectDefinition(), TaskType.F_DEPENDENT, value) : null;
    }

    /*
     *  Trigger
     */

    private void addTriggerTransient(TriggerType trigger) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().getTrigger().add(trigger);
        }
    }

    private ItemDelta<?, ?> addTriggerAndPrepareDelta(TriggerType trigger) throws SchemaException {
        addTriggerTransient(trigger.clone());
        return isPersistent() ?
                getPrismContext().deltaFor(TaskType.class)
                        .item(TaskType.F_TRIGGER).add(trigger.clone())
                        .asItemDelta()
                : null;
    }

    //endregion

    //region Dealing with extension

    /*
     *  Extension
     */

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
    public boolean hasExtension() {
        synchronized (prismAccess) {
            return taskPrism.getExtension() != null && !taskPrism.getExtension().isEmpty();
        }
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
    public <T> T getExtensionPropertyRealValue(ItemName propertyName) {
        synchronized (prismAccess) {
            PrismProperty<T> property = getExtensionPropertyUnsynchronized(propertyName);
            return property != null && !property.isEmpty() ? property.getRealValue() : null;
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
    public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> getExtensionItemOrClone(ItemName name) {
        synchronized (prismAccess) {
            return cloneIfRunning(getExtensionItemUnsynchronized(name));
        }
    }

    private <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> getExtensionItemUnsynchronized(ItemName name) {
        PrismContainer<? extends ExtensionType> extension = getExtensionOrClone();
        return extension != null ? extension.findItem(name) : null;
    }

    @Override
    public PrismReference getExtensionReferenceOrClone(ItemName name) {
        return (PrismReference) (Item) getExtensionItemOrClone(name);
    }

    @Override
    public void setExtensionItem(Item item) throws SchemaException {
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
        addPendingModification(setExtensionReferenceAndPrepareDelta(reference.getElementName(), reference.getDefinition(),
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

    // use this method to avoid cloning the value
    @Override
    public <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException {
        PrismPropertyDefinition propertyDef = getPrismContext().getSchemaRegistry()
                .findPropertyDefinitionByElementName(propertyName);
        if (propertyDef == null) {
            throw new SchemaException("Unknown property " + propertyName);
        }
        addPendingModification(
                setExtensionPropertyAndPrepareDelta(propertyName, propertyDef,
                        singletonList(getPrismContext().itemFactory().createPropertyValue(value))));
    }

    @Override
    public <T> void setExtensionPropertyValueTransient(QName propertyName, T value) throws SchemaException {
        PrismContext prismContext = getPrismContext();
        //noinspection unchecked
        PrismPropertyDefinition<T> propertyDef = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(propertyName);
        if (propertyDef == null) {
            throw new SchemaException("Unknown property " + propertyName);
        }
        PropertyDelta<T> delta = prismContext.deltaFactory().property().create(ItemPath.create(TaskType.F_EXTENSION, propertyName), propertyDef);
        //noinspection unchecked
        delta.setRealValuesToReplace(value);

        applyModificationsTransient(singletonList(delta));
    }

    // use this method to avoid cloning the value
    @Override
    public <T extends Containerable> void setExtensionContainerValue(QName containerName, T value) throws SchemaException {
        PrismContainerDefinition containerDef = getPrismContext().getSchemaRegistry()
                .findContainerDefinitionByElementName(containerName);
        if (containerDef == null) {
            throw new SchemaException("Unknown container item " + containerName);
        }
        addPendingModification(setExtensionContainerAndPrepareDelta(containerName, containerDef,
                singletonList(value.asPrismContainerValue())));
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
    public void modifyExtension(ItemDelta itemDelta) throws SchemaException {
        if (ItemPath.isEmpty(itemDelta.getPath()) || !itemDelta.getPath().startsWithName(TaskType.F_EXTENSION)) {
            throw new IllegalArgumentException(
                    "modifyExtension must modify the Task extension element; however, the path is " + itemDelta.getPath());
        }
        addPendingModification(modifyExtensionAndPrepareDelta(itemDelta));
    }

    @Override
    public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        try {
            modifyRepository(setExtensionPropertyAndPrepareDelta(property.getElementName(), property.getDefinition(),
                    PrismValueCollectionsUtil.cloneCollection(property.getValues())), parentResult);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
    }

    private ItemDelta<?, ?> setExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
            Collection<? extends PrismPropertyValue> values) throws SchemaException {
        //noinspection unchecked
        ItemDelta delta = deltaFactory().property().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?, ?> setExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition,
            Collection<? extends PrismReferenceValue> values) throws SchemaException {
        ItemDelta delta = deltaFactory().reference().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?, ?> addExtensionReferenceAndPrepareDelta(QName itemName, PrismReferenceDefinition definition,
            Collection<? extends PrismReferenceValue> values) throws SchemaException {
        ItemDelta delta = deltaFactory().reference().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return addExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private ItemDelta<?, ?> setExtensionContainerAndPrepareDelta(QName itemName, PrismContainerDefinition definition,
            Collection<? extends PrismContainerValue> values) throws SchemaException {
        //noinspection unchecked
        ItemDelta delta = deltaFactory().container().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        return setExtensionItemAndPrepareDeltaCommon(delta, values);
    }

    private <V extends PrismValue> ItemDelta<?, ?> setExtensionItemAndPrepareDeltaCommon(ItemDelta<?, ?> delta, Collection<V> values)
            throws SchemaException {
        // these values should have no parent, otherwise the following will fail
        //noinspection unchecked
        ((ItemDelta) delta).setValuesToReplace(values);
        applyModificationsTransient(singletonList(delta));         // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }

    private <V extends PrismValue> ItemDelta<?, ?> addExtensionItemAndPrepareDeltaCommon(ItemDelta<?, ?> delta, Collection<V> values)
            throws SchemaException {
        // these values should have no parent, otherwise the following will fail
        //noinspection unchecked
        ((ItemDelta) delta).addValuesToAdd(values);
        applyModificationsTransient(singletonList(delta));         // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }

    private ItemDelta<?, ?> modifyExtensionAndPrepareDelta(ItemDelta<?, ?> delta) throws SchemaException {
        applyModificationsTransient(singletonList(delta));         // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }

    private ItemDelta<?, ?> addExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
            Collection<? extends PrismPropertyValue> values) throws SchemaException {
        //noinspection unchecked
        ItemDelta<?, ?> delta = deltaFactory().property().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        //noinspection unchecked
        ((ItemDelta) delta).addValuesToAdd(values);
        applyModificationsTransient(singletonList(delta));         // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }

    private ItemDelta<?, ?> deleteExtensionPropertyAndPrepareDelta(QName itemName, PrismPropertyDefinition definition,
            Collection<? extends PrismPropertyValue> values) throws SchemaException {
        //noinspection unchecked
        ItemDelta<?, ?> delta = deltaFactory().property().create(ItemPath.create(TaskType.F_EXTENSION, itemName), definition);
        //noinspection unchecked
        ((ItemDelta) delta).addValuesToDelete(values);
        applyModificationsTransient(singletonList(delta));         // i.e. here we apply changes only locally (in memory)
        return isPersistent() ? delta : null;
    }
    //endregion

    //region Other getters and setters

    /*
     * Requestee
     */

    @Override
    public PrismObject<UserType> getRequestee() {
        return requestee;
    }

    @Override
    public void setRequesteeTransient(PrismObject<UserType> user) {
        requestee = user;
    }

    /*
     * Model operation context
     */

    // todo thread safety
    @Override
    public LensContextType getModelOperationContext() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getModelOperationContext();
        }
    }

    @Override
    public void setModelOperationContext(LensContextType value) throws SchemaException {
        synchronized (prismAccess) {
            addPendingModification(setModelOperationContextAndPrepareDelta(value));
        }
    }

    private void setModelOperationContextTransient(LensContextType value) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().setModelOperationContext(value);
        }
    }

    private ItemDelta<?, ?> setModelOperationContextAndPrepareDelta(LensContextType value)
            throws SchemaException {
        setModelOperationContextTransient(value);
        if (!isPersistent()) {
            return null;
        }
        if (value != null) {
            return getPrismContext().deltaFor(TaskType.class)
                    .item(F_MODEL_OPERATION_CONTEXT).replace(value.asPrismContainerValue().clone())
                    .asItemDelta();
        } else {
            return getPrismContext().deltaFor(TaskType.class)
                    .item(F_MODEL_OPERATION_CONTEXT).replace()
                    .asItemDelta();
        }
    }

    /*
     * Node
     */

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

    @SuppressWarnings("unused")
    public void setNodeTransient(String value) {
        setPropertyTransient(TaskType.F_NODE, value);
    }

    /*
     * Last run start timestamp
     */
    @Override
    public Long getLastRunStartTimestamp() {
        XMLGregorianCalendar gc = getProperty(TaskType.F_LAST_RUN_START_TIMESTAMP);
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    public void setLastRunStartTimestamp(Long value) {
        setProperty(TaskType.F_LAST_RUN_START_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    /*
     * Last run finish timestamp
     */

    @Override
    public Long getLastRunFinishTimestamp() {
        XMLGregorianCalendar gc = getProperty(TaskType.F_LAST_RUN_FINISH_TIMESTAMP);
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    public void setLastRunFinishTimestamp(Long value) {
        setProperty(TaskType.F_LAST_RUN_FINISH_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    /*
     * Completion timestamp
     */

    @Override
    public Long getCompletionTimestamp() {
        XMLGregorianCalendar gc = getProperty(TaskType.F_COMPLETION_TIMESTAMP);
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    @SuppressWarnings("unused")
    public void setCompletionTimestamp(Long value) {
        setProperty(TaskType.F_COMPLETION_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    private void setCompletionTimestampTransient(Long value) {
        setPropertyTransient(TaskType.F_COMPLETION_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    private PropertyDelta<?> setCompletionTimestampAndPrepareDelta(Long value) {
        setCompletionTimestampTransient(value);
        return createPropertyDeltaIfPersistent(TaskType.F_COMPLETION_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    /*
     * Next run start time
     */

    @Override
    public Long getNextRunStartTime(OperationResult result) {
        return taskManager.getNextRunStartTime(getOid(), result);
    }

    /*
     *  Handler and category
     */

    public TaskHandler getHandler() {
        String handlerUri = getHandlerUri();
        return handlerUri != null ? taskManager.getHandler(handlerUri) : null;
    }

    @Override
    public String getCategory() {
        return getProperty(TaskType.F_CATEGORY);
    }

    @Override
    public void setCategory(String value) {
        setProperty(TaskType.F_CATEGORY, value);
    }

    void setCategoryTransient(String value) {
        setPropertyTransient(TaskType.F_CATEGORY, value);
    }

    public String getCategoryFromHandler() {
        TaskHandler h = getHandler();
        return h != null ? h.getCategoryName(this) : null;
    }

    public String getChannelFromHandler() {
        TaskHandler h = getHandler();
        return h != null ? h.getDefaultChannel() : null;
    }

    @Override
    public void addArchetypeInformation(String archetypeOid) {
        synchronized (prismAccess) {
            List<ObjectReferenceType> existingArchetypes = taskPrism.asObjectable().getArchetypeRef();
            if (!existingArchetypes.isEmpty()) {
                throw new IllegalStateException("Couldn't add archetype " + archetypeOid + " because there is already one: "
                        + existingArchetypes + "; in " + this);
            }
            addContainerable(TaskType.F_ASSIGNMENT,
                    ObjectTypeUtil.createAssignmentTo(archetypeOid, ObjectTypes.ARCHETYPE, getPrismContext()));
            addReferencable(TaskType.F_ROLE_MEMBERSHIP_REF,
                    ObjectTypeUtil.createObjectRef(archetypeOid, ObjectTypes.ARCHETYPE));
            addReferencable(TaskType.F_ARCHETYPE_REF,
                    ObjectTypeUtil.createObjectRef(archetypeOid, ObjectTypes.ARCHETYPE));
        }
    }

    @Override
    public void addArchetypeInformationIfMissing(String archetypeOid) {
        synchronized (prismAccess) {
            List<ObjectReferenceType> existingArchetypes = taskPrism.asObjectable().getArchetypeRef();
            if (existingArchetypes.isEmpty()) {
                addArchetypeInformation(archetypeOid);
            }
        }
    }

    /*
     *  Other methods
     */

    @Override
    public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "refresh");
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        if (!isPersistent()) {
            // Nothing to do for transient tasks
            result.recordSuccess();
            return;
        }

        PrismObject<TaskType> repoObj;
        try {
            // Here we conservatively fetch the result. In the future we could optimize this a bit, avoiding result
            // fetching when not strictly necessary. But it seems that it needs to be fetched most of the time.
            Collection<SelectorOptions<GetOperationOptions>> options = getSchemaHelper().getOperationOptionsBuilder()
                    .item(TaskType.F_RESULT).retrieve().build();
            repoObj = repositoryService.getObject(TaskType.class, getOid(), options, result);
        } catch (ObjectNotFoundException ex) {
            result.recordFatalError("Object not found", ex);
            throw ex;
        } catch (SchemaException ex) {
            result.recordFatalError("Schema error", ex);
            throw ex;
        }
        this.taskPrism = repoObj;
        updateTaskResult();
        setDefaults();
        resolveOwnerRef(result);
        result.recordSuccess();
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
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Task(");
        sb.append(TaskQuartzImpl.class.getName());
        sb.append(")\n");
        DebugUtil.debugDumpLabelLn(sb, "prism", indent + 1);
        synchronized (prismAccess) {
            sb.append(taskPrism.debugDump(indent + 2));
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToStringLn(sb, "persistenceStatus", getPersistenceStatus(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "taskResult", taskResult, indent);
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

    protected PrismContext getPrismContext() {
        return taskManager.getPrismContext();
    }

    private SchemaHelper getSchemaHelper() {
        return taskManager.getSchemaHelper();
    }

    @Override
    public TaskQuartzImpl createSubtask() {
        TaskQuartzImpl sub = taskManager.createTaskInstance();
        sub.setParent(this.getTaskIdentifier());
        sub.setOwner(this.getOwner());
        sub.setChannel(this.getChannel());
        LOGGER.trace("New subtask {} has been created.", sub.getTaskIdentifier());
        return sub;
    }

    @Override
    public List<PrismObject<TaskType>> listPersistentSubtasksRaw(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listPersistentSubtasksRaw");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        if (isPersistent()) {
            return taskManager.listPersistentSubtasksForTask(getTaskIdentifier(), result);
        } else {
            result.recordSuccessIfUnknown();
            return new ArrayList<>(0);
        }
    }

    private List<PrismObject<TaskType>> listPrerequisiteTasksRaw(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listPrerequisiteTasksRaw");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        ObjectQuery query = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_DEPENDENT).eq(getTaskIdentifier())
                .build();

        List<PrismObject<TaskType>> list = taskManager.getRepositoryService().searchObjects(TaskType.class, query, null, result);
        result.recordSuccessIfUnknown();
        return list;
    }

    @NotNull
    @Override
    public List<Task> listSubtasks(boolean persistentOnly, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listSubtasks");
        result.addParam("persistentOnly", persistentOnly);
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        return listSubtasksInternal(persistentOnly, result);
    }

    @Override
    @NotNull
    public List<Task> listSubtasksInternal(boolean persistentOnly, OperationResult result) throws SchemaException {
        // persistent subtasks
        List<Task> retval = new ArrayList<>(taskManager.resolveTasksFromTaskTypes(listPersistentSubtasksRaw(result), result));
        // transient asynchronous subtasks - must be taken from the running task instance!
        if (!persistentOnly) {
            retval.addAll(taskManager.getTransientSubtasks(getTaskIdentifier()));
        }
        return retval;
    }

    @Override
    public List<Task> listSubtasksDeeply(boolean persistentOnly, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listSubtasksDeeply");
        result.addParam("persistentOnly", persistentOnly);
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        ArrayList<Task> retval = new ArrayList<>();
        addSubtasks(retval, this, persistentOnly, result);
        return retval;
    }

    private void addSubtasks(ArrayList<Task> tasks, InternalTaskInterface taskToProcess, boolean persistentOnly, OperationResult result) throws SchemaException {
        for (Task task : taskToProcess.listSubtasksInternal(persistentOnly, result)) {
            tasks.add(task);
            addSubtasks(tasks, (InternalTaskInterface) task, persistentOnly, result);
        }
    }

    @Override
    public List<Task> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "listPrerequisiteTasks");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);

        return taskManager.resolveTasksFromTaskTypes(listPrerequisiteTasksRaw(result), result);
    }

    @Override
    public void close(OperationResult taskResult, boolean saveState, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        if (taskResult != null) {
            addIgnoreNull(deltas, setResultAndPrepareDelta(taskResult));
            // result status was set in task during the previous call
            addIgnoreNull(deltas, createPropertyDeltaIfPersistent(TaskType.F_RESULT_STATUS, taskResult.getStatus() != null ? taskResult.getStatus().createStatusType() : null));
        }
        addIgnoreNull(deltas, setExecutionStatusAndPrepareDelta(TaskExecutionStatus.CLOSED));
        addIgnoreNull(deltas, setCompletionTimestampAndPrepareDelta(System.currentTimeMillis()));
        Duration cleanupAfterCompletion = taskPrism.asObjectable().getCleanupAfterCompletion();
        if (cleanupAfterCompletion != null) {
            TriggerType trigger = new TriggerType(getPrismContext())
                    .timestamp(XmlTypeConverter.fromNow(cleanupAfterCompletion))
                    .handlerUri(SchemaConstants.COMPLETED_TASK_CLEANUP_TRIGGER_HANDLER_URI);
            addIgnoreNull(deltas, addTriggerAndPrepareDelta(
                    trigger));      // we just ignore any other triggers (they will do nothing if launched too early)
        }
        if (saveState) {
            try {
                modifyRepository(deltas, parentResult);
            } catch (ObjectAlreadyExistsException e) {
                throw new SystemException(e);
            }
        } else {
            pendingModifications.addAll(deltas);
        }
    }

    // todo thread safety (creating a clone?)
    @Override
    public TaskWorkManagementType getWorkManagement() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getWorkManagement();
        }
    }

    // todo thread safety (creating a clone?)
    @Override
    public TaskWorkStateType getWorkState() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getWorkState();
        }
    }

    @Override
    public TaskKindType getKind() {
        synchronized (prismAccess) {
            TaskWorkManagementType workManagement = getWorkManagement();
            return workManagement != null ? workManagement.getTaskKind() : null;
        }
    }

    @Override
    public TaskUnpauseActionType getUnpauseAction() {
        return getProperty(TaskType.F_UNPAUSE_ACTION);
    }

    @Override
    public TaskExecutionStatusType getStateBeforeSuspend() {
        return getProperty(TaskType.F_STATE_BEFORE_SUSPEND);
    }

    @Override
    public void applyDeltasImmediate(Collection<ItemDelta<?, ?>> itemDeltas, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        if (isPersistent()) {
            repositoryService.modifyObject(TaskType.class, getOid(), CloneUtil.cloneCollectionMembers(itemDeltas), result);
        }
        applyModificationsTransient(itemDeltas);
        synchronizeWithQuartzIfNeeded(pendingModifications, result);
    }

    @Override
    public boolean isPartitionedMaster() {
        synchronized (prismAccess) {
            TaskWorkManagementType workManagement = getWorkManagement();
            return workManagement != null && workManagement.getTaskKind() == TaskKindType.PARTITIONED_MASTER;
        }
    }

    @Override
    public String getExecutionGroup() {
        synchronized (prismAccess) {
            TaskExecutionConstraintsType executionConstraints = getExecutionConstraints();
            return executionConstraints != null ? executionConstraints.getGroup() : null;
        }
    }

    @Override
    public OperationStatsType getAggregatedLiveOperationStats() {
        return statistics.getAggregatedLiveOperationStats(emptyList());
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

    private DeltaFactory deltaFactory() {
        return getPrismContext().deltaFactory();
    }

    //region Statistics collection

    /*
     * Here we simply delegate statistics collection and retrieval methods calls to the collector.
     */
    @Override
    public void recordState(String message) {
        statistics.recordState(message);
    }

    @Override
    public void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
            ProvisioningOperation operation, boolean success, int count, long duration) {
        statistics.recordProvisioningOperation(resourceOid, resourceName, objectClassName, operation, success, count, duration);
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
    public void recordIterativeOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid) {
        LOGGER.trace("recordIterativeOperationStart: {} in {}", objectDisplayName, this);
        statistics.recordIterativeOperationStart(objectName, objectDisplayName, objectType, objectOid);
    }

    @Override
    public void recordIterativeOperationStart(ShadowType shadow) {
        statistics.recordIterativeOperationStart(shadow);
    }

    @Override
    public void recordIterativeOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid,
            long started, Throwable exception) {
        statistics.recordIterativeOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception);
    }

    @Override
    public void recordIterativeOperationEnd(ShadowType shadow, long started, Throwable exception) {
        statistics.recordIterativeOperationEnd(shadow, started, exception);
    }

    @Override
    public void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid,
            long started, Throwable exception, SynchronizationInformation.Record originalStateIncrement,
            SynchronizationInformation.Record newStateIncrement) {
        LOGGER.trace("recordSynchronizationOperationEnd: {} in {}", objectDisplayName, this);
        statistics.recordSynchronizationOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception, originalStateIncrement, newStateIncrement);
    }

    @Override
    public void recordSynchronizationOperationEnd(ShadowType shadow, long started, Throwable exception,
            SynchronizationInformation.Record originalStateIncrement,
            SynchronizationInformation.Record newStateIncrement) {
        LOGGER.trace("recordSynchronizationOperationEnd: {} in {}", shadow, this);
        statistics.recordSynchronizationOperationEnd(shadow, started, exception, originalStateIncrement, newStateIncrement);
    }

    @Override
    public void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
            ChangeType changeType, String channel, Throwable exception) {
        LOGGER.trace("recordObjectActionExecuted: {} {} in {}", changeType, objectDisplayName, this);
        statistics.recordObjectActionExecuted(objectName, objectDisplayName, objectType, objectOid, changeType, channel, exception);
    }

    @Override
    public void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception) {
        LOGGER.trace("recordObjectActionExecuted: {} {} in {}", changeType, object, this);
        statistics.recordObjectActionExecuted(object, changeType, getChannel(), exception);
    }

    @Override
    public <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
            String defaultOid, ChangeType changeType, String channel, Throwable exception) {
        LOGGER.trace("recordObjectActionExecuted: {} {} in {}", changeType, object, this);
        statistics.recordObjectActionExecuted(object, objectTypeClass, defaultOid, changeType, channel, exception);
    }

    @Override
    public void markObjectActionExecutedBoundary() {
        LOGGER.trace("markObjectActionExecutedBoundary: {}", this);
        statistics.markObjectActionExecutedBoundary();
    }

    @Override
    public void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
        statistics.resetEnvironmentalPerformanceInformation(value);
    }

    @Override
    public void resetSynchronizationInformation(SynchronizationInformationType value) {
        statistics.resetSynchronizationInformation(value);
    }

    @Override
    public void resetIterativeTaskInformation(IterativeTaskInformationType value) {
        statistics.resetIterativeTaskInformation(value);
    }

    @Override
    public void resetActionsExecutedInformation(ActionsExecutedInformationType value) {
        statistics.resetActionsExecutedInformation(value);
    }

    @NotNull
    @Override
    public List<String> getLastFailures() {
        return statistics.getLastFailures();
    }

    //endregion

    @Override
    public ObjectReferenceType getSelfReference() {
        if (getOid() != null) {
            return new ObjectReferenceType()
                    .type(TaskType.COMPLEX_TYPE)
                    .oid(getOid())
                    .relation(getDefaultRelation())
                    .targetName(getName());
        } else {
            throw new IllegalStateException("Reference cannot be created for a transient task: " + this);
        }
    }

    private QName getDefaultRelation() {
        return getPrismContext().getDefaultRelation();
    }

    @Override
    public String getVersion() {
        synchronized (prismAccess) {
            return taskPrism.getVersion();
        }
    }

    // todo thread-safety
    @Override
    public Collection<? extends TriggerType> getTriggers() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getTrigger();
        }
    }

    // todo thread-safety
    @Override
    public Collection<? extends AssignmentType> getAssignments() {
        synchronized (prismAccess) {
            return taskPrism.asObjectable().getAssignment();
        }
    }

    @Override
    public Collection<Task> getPathToRootTask(OperationResult parentResult) throws SchemaException {
        List<Task> allTasksToRoot = new ArrayList<>();
        allTasksToRoot.add(this);

        if (getParent() == null) {
            return allTasksToRoot;
        }

        Task parent = getParentTaskSafe(parentResult);
        if (parent == null) {
            return allTasksToRoot;
        }

        allTasksToRoot.addAll(parent.getPathToRootTask(parentResult));
        return allTasksToRoot;
    }

    public String getTaskTreeId(OperationResult parentResult) throws SchemaException {
        if (getParent() == null) {
            return getOid();
        }

        Task parent = getParentTaskSafe(parentResult);
        if (parent == null) {
            return getOid();
        }

        return parent.getTaskTreeId(parentResult);
    }

    private Task getParentTaskSafe(OperationResult parentResult) throws SchemaException {
        if (getParent() == null) {
            return null;
        }

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "getPathToRootTask");
        result.addContext(OperationResult.CONTEXT_OID, getOid());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskQuartzImpl.class);
        try {
            Task parent = getParentTask(result);
            result.recordSuccess();
            return parent;
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Cannot find parent identified by {}, {}", getParent(), e.getMessage(), e);
            result.recordFatalError("Cannot find parent identified by " + getParent() + ". Reason: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ObjectReferenceType getOwnerRef() {
        synchronized (prismAccess) {
            return cloneIfRunning(taskPrism.asObjectable().getOwnerRef());
        }
    }

    @Override
    public void applyModificationsTransient(Collection<ItemDelta<?, ?>> modifications) throws SchemaException {
        synchronized (prismAccess) {
            ItemDeltaCollectionsUtil.applyTo(modifications, taskPrism);
        }
    }

    @Override
    public void addSubtask(TaskType subtaskBean) {
        synchronized (prismAccess) {
            taskPrism.asObjectable().getSubtaskRef().add(ObjectTypeUtil.createObjectRefWithFullObject(subtaskBean, getPrismContext()));
        }
    }

    @NotNull
    @Override
    public Collection<String> getCachingProfiles() {
        TaskExecutionEnvironmentType executionEnvironment = getExecutionEnvironment();
        return executionEnvironment != null ? Collections.unmodifiableCollection(executionEnvironment.getCachingProfile()) : emptySet();
    }

    @Override
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

    @Override
    public void setExecutionEnvironmentTransient(TaskExecutionEnvironmentType value) {
        setContainerableTransient(TaskType.F_EXECUTION_ENVIRONMENT, value);
    }

    @Override
    public boolean isScavenger() {
        TaskWorkManagementType workManagement = getWorkManagement();
        return workManagement != null && Boolean.TRUE.equals(workManagement.isScavenger());
    }

    @NotNull
    @Override
    public Collection<TracingRootType> getTracingRequestedFor() {
        if (taskManager.isTracingOverridden()) {
            return taskManager.getGlobalTracingRequestedFor();
        } else {
            return tracingRequestedFor;
        }
    }

    @Override
    public void addTracingRequest(TracingRootType point) {
        tracingRequestedFor.add(point);
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
}
