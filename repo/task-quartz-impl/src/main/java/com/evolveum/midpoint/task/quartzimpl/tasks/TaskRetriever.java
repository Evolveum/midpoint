/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskTreeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.LightweightTaskManager;
import com.evolveum.midpoint.task.quartzimpl.LocalNodeState;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformationRetriever;
import com.evolveum.midpoint.task.quartzimpl.execution.StalledTasksWatcher;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.quartz.NextStartTimes;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class TaskRetriever {

    private static final Trace LOGGER = TraceManager.getTrace(TaskRetriever.class);
    private static final String CLASS_DOT = TaskRetriever.class.getName() + ".";
    private static final String OP_GET_TASK_SAFELY = CLASS_DOT + ".getTaskSafely";

    @Autowired private LocalScheduler localScheduler;
    @Autowired private ClusterManager clusterManager;
    @Autowired private TaskInstantiator taskInstantiator;
    @Autowired private LocalNodeState localNodeState;
    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;
    @Autowired private ClusterStatusInformationRetriever clusterStatusInformationRetriever;
    @Autowired private ClusterExecutionHelper clusterExecutionHelper;
    @Autowired private StalledTasksWatcher stalledTasksWatcher;
    @Autowired private LightweightTaskManager lightweightTaskManager;

    @NotNull
    public TaskQuartzImpl getTaskPlain(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        return getTaskPlain(taskOid, null, parentResult);
    }

    @NotNull
    public TaskQuartzImpl getTaskWithResult(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_RESULT).retrieve()
                .build();
        return getTaskPlain(taskOid, options, parentResult);
    }

    @NotNull
    public TaskQuartzImpl getTaskPlain(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<TaskType> taskPrism = repositoryService.getObject(TaskType.class, oid, options, result);
        return taskInstantiator.createTaskInstance(taskPrism, result);
    }

    @NotNull
    public <T extends ObjectType> PrismObject<T> getTaskPrismObject(String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (GetOperationOptions.isRaw(rootOptions)) {
            //noinspection unchecked
            return (PrismObject<T>) repositoryService.getObject(TaskType.class, oid, options, result);
        } else {
            Task task = getTask(oid, options, result);
            //noinspection unchecked
            return (PrismObject<T>) task.getUpdatedTaskObject();
        }
    }

    /**
     * Connects to the remote node if needed.
     */
    @NotNull
    public TaskQuartzImpl getTask(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        try {
            // returns null if noFetch is set
            ClusterStatusInformation csi = clusterStatusInformationRetriever
                    .getClusterStatusInformation(options, TaskType.class, true, result);

            PrismObject<TaskType> taskPrism = getTaskFromRemoteNode(oid, options, csi, result);
            if (taskPrism == null) {
                taskPrism = repositoryService.getObject(TaskType.class, oid, options, result);
            }

            TaskQuartzImpl task = taskInstantiator.createTaskInstance(taskPrism, result);

            addTransientTaskInformation(task,
                    csi,
                    SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NEXT_RUN_START_TIMESTAMP, options),
                    SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NEXT_RETRY_TIMESTAMP, options),
                    SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NODE_AS_OBSERVED, options),
                    result);

            if (SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_SUBTASK_REF, options)) {
                fillInSubtasks(task, csi, options, result);
            }
            updateFromTaskInMemory(task);
            return task;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private PrismObject<TaskType> getTaskFromRemoteNode(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            ClusterStatusInformation csi, OperationResult parentResult) throws SchemaException {

        if (csi == null) { // in case no fetch was used...
            return null;
        }

        NodeType runsAt = csi.findNodeInfoForTask(oid);
        if (runsAt == null || clusterManager.isCurrentNode(runsAt.asPrismObject())) {
            return null;
        }

        Holder<PrismObject<TaskType>> taskPrism = new Holder<>();
        clusterExecutionHelper.execute(runsAt, (client, node, opResult) -> {
            Response response = client.path(TaskConstants.GET_TASK_REST_PATH + oid)
                    .query("include", GetOperationOptions.toRestIncludeOption(options))
                    .get();
            Response.StatusType statusType = response.getStatusInfo();
            if (statusType.getFamily() == Response.Status.Family.SUCCESSFUL) {
                TaskType taskType = response.readEntity(TaskType.class);
                taskPrism.setValue(taskType.asPrismObject());
            } else {
                LOGGER.warn("Cannot get task from {}", node);
            }
        }, new ClusterExecutionOptions().tryAllNodes(), "load task (cluster)", parentResult);

        return taskPrism.getValue();
    }

    /**
     * Updates selected fields (operation statistics, progress) from in-memory running task.
     */
    private void updateFromTaskInMemory(Task task0) {
        TaskQuartzImpl task = (TaskQuartzImpl) task0;

        if (task.getTaskIdentifier() == null) {
            return; // shouldn't really occur
        }

        RunningTask taskInMemory = localNodeState.getLocallyRunningTaskByIdentifier(task.getTaskIdentifier());
        if (taskInMemory == null) {
            return;
        }

        OperationStatsType operationStats = taskInMemory.getAggregatedLiveOperationStats();
        if (operationStats != null) {
            operationStats.setLiveInformation(true);
        }
        task.setOperationStatsTransient(operationStats);
        task.setProgressTransient(taskInMemory.getLegacyProgress());

        // We intentionally do not try to get operation result from the task. OperationResult class is not thread-safe,
        // so it cannot be safely accessed from a different thread. It is not a big problem, because the result should be
        // periodically updated in the repository, so we get (at worst) an information that is a few seconds out-of-date.
    }

    // task is Task or TaskType
    // returns List<Task> or List<PrismObject<TaskType>>
    private List<?> getSubtasks(Object task, OperationResult result) throws SchemaException {
        if (task instanceof Task) {
            return ((Task) task).listSubtasks(result);
        } else if (task instanceof TaskType) {
            return listPersistentSubtasksForTask(((TaskType) task).getTaskIdentifier(), result);
        } else if (task instanceof PrismObject<?>) {
            //noinspection unchecked
            return listPersistentSubtasksForTask(((PrismObject<TaskType>) task).asObjectable().getTaskIdentifier(), result);
        } else {
            throw new IllegalArgumentException("task: " + task + " (of class " + (task != null ? task.getClass() : "null") + ")");
        }
    }

    // task is Task, TaskType or PrismObject<TaskType>
    // subtask is Task or PrismObject<TaskType>
    private void addSubtask(Object task, Object subtask) {
        TaskType subtaskBean;
        if (subtask instanceof TaskQuartzImpl) {
            subtaskBean = ((TaskQuartzImpl) subtask).getRawTaskObjectClonedIfNecessary().asObjectable();
        } else if (subtask instanceof PrismObject<?>) {
            //noinspection unchecked
            subtaskBean = ((PrismObject<TaskType>) subtask).asObjectable();
        } else {
            throw new IllegalArgumentException("subtask: " + task);
        }

        if (task instanceof Task) {
            ((TaskQuartzImpl) task).addSubtask(subtaskBean);
        } else if (task instanceof TaskType) {
            TaskTreeUtil.addSubtask((TaskType) task, subtaskBean, prismContext);
        } else if (task instanceof PrismObject<?>) {
            //noinspection unchecked
            TaskTreeUtil.addSubtask(((PrismObject<TaskType>) task).asObjectable(), subtaskBean, prismContext);
        } else {
            throw new IllegalArgumentException("task: " + task);
        }
    }

    private boolean isPersistent(Object task) {
        if (task instanceof Task) {
            return ((Task) task).isPersistent();
        } else if (task instanceof PrismObject<?>) {
            return ((PrismObject<?>) task).getOid() != null;
        } else {
            throw new IllegalArgumentException("task: " + task);
        }
    }

    // task is Task or TaskType
    private void fillInSubtasks(Object task, ClusterStatusInformation csi,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        boolean retrieveNextRunStartTime =
                SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NEXT_RUN_START_TIMESTAMP, options);
        boolean retrieveRetryTime =
                SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NEXT_RETRY_TIMESTAMP, options);
        boolean retrieveNodeAsObserved =
                SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NODE_AS_OBSERVED, options);

        for (Object subtask : getSubtasks(task, result)) {
            if (isPersistent(subtask)) {
                addTransientTaskInformation(subtask, csi, retrieveNextRunStartTime, retrieveRetryTime,
                        retrieveNodeAsObserved, result);
                fillInSubtasks(subtask, csi, options, result);
            }
            addSubtask(task, subtask);
        }
    }

    private List<PrismObject<TaskType>> listPersistentSubtasksForTask(String taskIdentifier, OperationResult result)
            throws SchemaException {
        if (StringUtils.isEmpty(taskIdentifier)) {
            return new ArrayList<>();
        }
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_PARENT).eq(taskIdentifier)
                .build();

        List<PrismObject<TaskType>> list;
        try {
            list = repositoryService.searchObjects(TaskType.class, query, null, result);
            result.recordSuccessIfUnknown();
        } catch (SchemaException | RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        }
        return list;
    }

    @NotNull
    public SearchResultList<PrismObject<TaskType>> searchTasks(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {

        // returns null if noFetch is set
        ClusterStatusInformation csi = clusterStatusInformationRetriever
                .getClusterStatusInformation(options, TaskType.class, true, result);

        List<PrismObject<TaskType>> tasksInRepository;
        try {
            tasksInRepository = repositoryService.searchObjects(TaskType.class, query, options, result);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't get tasks from repository: " + e.getMessage(), e);
            throw e;
        }

        boolean retrieveNextRunStartTime =
                SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NEXT_RUN_START_TIMESTAMP, options);
        boolean retrieveRetryTime =
                SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NEXT_RETRY_TIMESTAMP, options);
        boolean retrieveNodeAsObserved =
                SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NODE_AS_OBSERVED, options);
        boolean loadSubtasks =
                SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_SUBTASK_REF, options);

        List<PrismObject<TaskType>> tasks = new ArrayList<>();
        for (PrismObject<TaskType> taskInRepository : tasksInRepository) {
            addTransientTaskInformation(taskInRepository, csi,
                    retrieveNextRunStartTime, retrieveRetryTime, retrieveNodeAsObserved, result);
            if (loadSubtasks) {
                fillInSubtasks(taskInRepository, csi, options, result);
            }
            tasks.add(taskInRepository);
        }
        return new SearchResultList<>(tasks);
    }

    // task is Task or PrismObject<TaskType>
    private void addTransientTaskInformation(Object task, ClusterStatusInformation csi,
            boolean retrieveNextRunStartTime, boolean retrieveRetryTime, boolean retrieveNodeAsObserved, OperationResult result) {

        if (!isPersistent(task)) {
            throw new IllegalStateException("Task " + task + " is not persistent");
        }
        if (task instanceof RunningTask) {
            throw new UnsupportedOperationException("addTransientTaskInformation is not available for running tasks");
        }
        TaskType taskBean;
        if (task instanceof TaskQuartzImpl) {
            taskBean = ((TaskQuartzImpl) task).getRawTaskObject().asObjectable();
        } else if (task instanceof PrismObject<?>) {
            //noinspection unchecked
            taskBean = ((PrismObject<TaskType>) task).asObjectable();
        } else {
            throw new IllegalArgumentException("task: " + task);
        }

        if (csi != null && retrieveNodeAsObserved) {
            NodeType runsAt = csi.findNodeInfoForTask(taskBean.getOid());
            if (runsAt != null) {
                taskBean.setNodeAsObserved(runsAt.getNodeIdentifier());
            }
        }
        if (retrieveNextRunStartTime || retrieveRetryTime) {
            NextStartTimes times = localScheduler.getNextStartTimes(taskBean.getOid(), retrieveNextRunStartTime,
                    retrieveRetryTime, result);
            if (retrieveNextRunStartTime && times.getNextScheduledRun() != null) {
                taskBean.setNextRunStartTimestamp(XmlTypeConverter.createXMLGregorianCalendar(times.getNextScheduledRun()));
            }
            if (retrieveRetryTime && times.getNextRetry() != null) {
                taskBean.setNextRetryTimestamp(XmlTypeConverter.createXMLGregorianCalendar(times.getNextRetry()));
            }
        }
        Long stalledSince = stalledTasksWatcher.getStalledSinceForTask(taskBean);
        if (stalledSince != null) {
            taskBean.setStalledSince(XmlTypeConverter.createXMLGregorianCalendar(stalledSince));
        }
    }

    @NotNull
    public TaskQuartzImpl getTaskByIdentifier(String identifier, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        PrismObject<TaskType> taskObject = getTaskTypeByIdentifier(identifier, null, result);
        return taskInstantiator.createTaskInstance(taskObject, result);
    }

    @NotNull
    public PrismObject<TaskType> getTaskTypeByIdentifier(String identifier,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_TASK_IDENTIFIER).eq(identifier)
                .build();
        List<PrismObject<TaskType>> list = repositoryService.searchObjects(TaskType.class, query, options, result);
        if (list.isEmpty()) {
            throw new ObjectNotFoundException("Task with identifier " + identifier + " could not be found");
        } else if (list.size() > 1) {
            throw new IllegalStateException("Found more than one task with identifier " + identifier + " (" + list.size() + " of them)");
        }
        PrismObject<TaskType> taskPrism = list.get(0);
        if (SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_SUBTASK_REF, options)) {
            // returns null if noFetch is set
            ClusterStatusInformation csi = clusterStatusInformationRetriever
                    .getClusterStatusInformation(options, TaskType.class, true, result);
            fillInSubtasks(taskPrism.asObjectable(), csi, options, result);
        }
        return taskPrism;
    }

    public boolean isOrphaned(PrismObject<TaskType> task, OperationResult result) throws SchemaException {
        String parentIdentifier = task.asObjectable().getParent();
        if (parentIdentifier == null) {
            return false;
        }
        try {
            PrismObject<TaskType> parent = getTaskTypeByIdentifier(parentIdentifier, null, result);
            LOGGER.trace("Found a parent of {}: {}", task, parent);
            return false;
        } catch (ObjectNotFoundException e) {
            LOGGER.debug("Parent ({}) of {} does not exist. The task is orphaned.", parentIdentifier, task);
            result.muteLastSubresultError();
            result.recordSuccess(); // we want not only FATAL_ERROR to be removed but we don't want to see HANDLED_ERROR as well
            return true;
        }
    }

    private List<TaskQuartzImpl> resolveTasksFromTaskTypes(List<PrismObject<TaskType>> taskPrisms, OperationResult result)
            throws SchemaException {
        List<TaskQuartzImpl> tasks = new ArrayList<>(taskPrisms.size());
        for (PrismObject<TaskType> taskPrism : taskPrisms) {
            tasks.add(taskInstantiator.createTaskInstance(taskPrism, result));
        }
        result.recordSuccessIfUnknown();
        return tasks;
    }

    // if there are problems with retrieving a task, we just log exception and put into operation result
    List<TaskQuartzImpl> resolveTaskOids(Collection<String> oids, OperationResult result) {
        List<TaskQuartzImpl> tasks = new ArrayList<>();
        for (String oid : oids) {
            try {
                TaskQuartzImpl task = getTaskPlain(oid, result);
                tasks.add(task);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve task with OID {}", e, oid);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve task with OID {}", e, oid);
            }
        }
        return tasks;
    }

    @SuppressWarnings("SameParameterValue")
    public List<? extends Task> listWaitingTasks(TaskWaitingReasonType reason, OperationResult result) throws SchemaException {
        S_FilterEntry q = prismContext.queryFor(TaskType.class);
        q = q.item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.WAITING).and();
        if (reason != null) {
            q = q.item(TaskType.F_WAITING_REASON).eq(reason).and();
        }
        ObjectQuery query = q.all().build();
        List<PrismObject<TaskType>> prisms = repositoryService.searchObjects(TaskType.class, query, null, result);
        List<? extends Task> tasks = resolveTasksFromTaskTypes(prisms, result);

        result.recordSuccessIfUnknown();
        return tasks;
    }

    public List<Task> listDependents(Task task, OperationResult result) throws SchemaException {
        List<String> dependentsIdentifiers = task.getDependents();
        List<Task> dependents = new ArrayList<>(dependentsIdentifiers.size());
        for (String dependentId : dependentsIdentifiers) {
            try {
                dependents.add(getTaskByIdentifier(dependentId, result));
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("Dependent task {} was not found. Probably it was not yet stored to repo; we just ignore it.",
                        dependentId);
            }
        }
        return dependents;
    }

    public PrismObject<TaskType> getRepoObjectWithResult(String oid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_RESULT).retrieve().build();
        return repositoryService.getObject(TaskType.class, oid, options, result);
    }

    public PrismObject<TaskType> getRepoObjectWithoutResult(String oid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return repositoryService.getObject(TaskType.class, oid, null, result);
    }

    private List<PrismObject<TaskType>> listPrerequisiteTasksRaw(TaskQuartzImpl task, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_DEPENDENT).eq(task.getTaskIdentifier())
                .build();
        return repositoryService.searchObjects(TaskType.class, query, null, result);
    }

    public List<TaskQuartzImpl> listPrerequisiteTasks(TaskQuartzImpl task, OperationResult result) throws SchemaException {
        List<PrismObject<TaskType>> rawTasks = listPrerequisiteTasksRaw(task, result);
        return resolveTasksFromTaskTypes(rawTasks, result);
    }

    public List<TaskQuartzImpl> listSubtasks(TaskQuartzImpl task, boolean persistentOnly, OperationResult result)
            throws SchemaException {
        List<PrismObject<TaskType>> persistentSubtasksRaw = listPersistentSubtasksRaw(task, result);
        List<TaskQuartzImpl> persistentSubtasks = resolveTasksFromTaskTypes(persistentSubtasksRaw, result);
        Collection<TaskQuartzImpl> transientSubtasks;
        if (!persistentOnly) {
            transientSubtasks = lightweightTaskManager.getTransientSubtasks(task.getTaskIdentifier());
        } else {
            transientSubtasks = emptyList();
        }
        List<TaskQuartzImpl> subtasks = new ArrayList<>(persistentSubtasks.size() + transientSubtasks.size());
        subtasks.addAll(persistentSubtasks);
        subtasks.addAll(transientSubtasks);
        return subtasks;
    }

    private List<PrismObject<TaskType>> listPersistentSubtasksRaw(TaskQuartzImpl task, OperationResult result) throws SchemaException {
        if (task.isPersistent()) {
            return listPersistentSubtasksForTask(task.getTaskIdentifier(), result);
        } else {
            return new ArrayList<>(0);
        }
    }

    public @NotNull List<TaskQuartzImpl> listSubtasksDeeply(TaskQuartzImpl task, boolean persistentOnly, OperationResult result) throws SchemaException {
        List<TaskQuartzImpl> subtasks = new ArrayList<>();
        addSubtasks(subtasks, task, persistentOnly, result);
        return subtasks;
    }

    private void addSubtasks(List<TaskQuartzImpl> tasks, TaskQuartzImpl taskToProcess, boolean persistentOnly,
            OperationResult result) throws SchemaException {
        List<TaskQuartzImpl> subtasks = listSubtasks(taskToProcess, persistentOnly, result);
        for (TaskQuartzImpl task : subtasks) {
            tasks.add(task);
            addSubtasks(tasks, task, persistentOnly, result);
        }
    }

    /**
     * Resolves task OIDs. Skips those that do not exist or cannot be fetched for other reason.
     */
    public Collection<Task> resolveTaskOidsSafely(Collection<String> oids, OperationResult result) {
        try {
            List<Task> tasks = new ArrayList<>();
            for (String oid : oids) {
                TaskQuartzImpl task = getTaskSafely(oid, result);
                if (task != null) {
                    tasks.add(task);
                }
            }
            return tasks;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /** returns null if the task cannot be fetched */
    private TaskQuartzImpl getTaskSafely(String oid, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_GET_TASK_SAFELY);
        try {
            return getTaskPlain(oid, result);
        } catch (Exception e) {
            String m = "Couldn't get the task with OID " + oid + ": " + e.getMessage();
            LoggingUtils.logUnexpectedException(LOGGER, m, e);
            result.recordFatalError(e);
            return null;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

}
