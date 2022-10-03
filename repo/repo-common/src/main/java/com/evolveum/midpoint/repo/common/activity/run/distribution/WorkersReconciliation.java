/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.distribution;

import static com.evolveum.midpoint.repo.common.activity.run.distribution.WorkersReconciliationOptions.shouldCloseWorkersOnWorkDone;
import static com.evolveum.midpoint.repo.common.activity.run.distribution.WorkersReconciliationOptions.shouldCreateSuspended;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Executes the workers reconciliation. This includes
 *
 * 1. auto-reconciliation when distributed activity starts,
 * 2. explicitly requested reconciliation (e.g. via GUI),
 * 3. reconciliation during auto-scaling.
 *
 * The reconciliation process tries to match worker tasks with the "to be" state (driven by configured distribution
 * plus current cluster state). For details please see {@link #execute(OperationResult)} method.
 */
public class WorkersReconciliation {

    private static final String OP_EXECUTE = WorkersReconciliation.class.getName() + ".execute";

    /**
     * How long should we wait when we request workers suspension?
     *
     * It is imaginable to wait no time. However, let's keep this approach for now.
     */
    private static final long SUSPENSION_WAIT_TIME = 10000L;

    private static final ItemPath SCAVENGER_PATH = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY,
            ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_SCAVENGER);

    private static final Trace LOGGER = TraceManager.getTrace(WorkersReconciliation.class);

    @NotNull private final Task rootTask;

    @NotNull private final Task coordinatorTask;

    @NotNull private final ActivityPath activityPath;

    private final WorkersReconciliationOptions options;

    @NotNull private final CommonTaskBeans beans;

    private Activity<?, ?> activity;

    private ActivityStateType coordinatorActivityState;

    private WorkersDefinitionType workersDefinitionBean;

    private ExpectedSetup expectedSetup;

    private List<Task> currentWorkers;

    private final Set<String> workersToResume = new HashSet<>();

    /**
     * Workers that should be present in a system. Described by their characterization.
     */
    private Set<WorkerCharacterization> shouldBeWorkers;

    @NotNull private final WorkersReconciliationResultType reconciliationResult;

    public WorkersReconciliation(@NotNull Task rootTask, @NotNull Task coordinatorTask,
            @NotNull ActivityPath activityPath, WorkersReconciliationOptions options,
            @NotNull CommonTaskBeans beans) {
        this.rootTask = rootTask;
        this.coordinatorTask = coordinatorTask;
        this.activityPath = activityPath;
        this.options = options;
        this.beans = beans;
        this.reconciliationResult = new WorkersReconciliationResultType();
    }

    /**
     * Executes the workers reconciliation.
     *
     * The simple part where tasks are on the correct nodes:
     *
     * 1. workers that match "to be" state (i.e. they have appropriate group + name + scavenger flag)
     * are *accepted* - see {@link #skipMatchingWorkers()};
     * 2. workers that are compatible (matching group + scavenger flag) but with not matching name are simply *renamed*,
     * see {@link #renameCompatibleWorkers(OperationResult)};
     * 3. workers that are in correct group (but otherwise wrong) are *adapted* by renaming and setting scavenger flag,
     * see {@link #adaptGroupCompatibleWorkers(OperationResult)};
     *
     * At this moment, we have to start reconciling workers among nodes.
     *
     * TODO finish the description
     */
    public @NotNull WorkersReconciliationResultType execute(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(OP_EXECUTE);
        try {

            initialize();

            if (coordinatorActivityState == null) {
                result.recordNotApplicable("Activity has not run yet.");
                return reconciliationResult;
            }

            expectedSetup = ExpectedSetup.create(activity, workersDefinitionBean, beans, coordinatorTask, rootTask, result);
            shouldBeWorkers = expectedSetup.getWorkers();
            int startingShouldBeWorkersCount = shouldBeWorkers.size();

            currentWorkers = getCurrentWorkersSorted(result);
            int startingWorkersCount = currentWorkers.size();

            LOGGER.trace("Before reconciliation:\nCurrent workers: {}\nShould be workers: {}\n"
                            + "Nodes up: {}\nNodes up and alive: {}",
                    currentWorkers, shouldBeWorkers, expectedSetup.getNodesUp(), expectedSetup.getNodesUpAndAlive());

            // The simple part (correct nodes).

            skipMatchingWorkers();
            renameCompatibleWorkers(result);
            adaptGroupCompatibleWorkers(result);

            suspendRunningWorkersOnLiveNodes(result);

            // Finally, let's create workers for the nodes where they are missing.
            createWorkers(result);

            if (!shouldCreateSuspended(options)) {
                resumeSelectedWorkers(result);
            }

            if (shouldCloseWorkersOnWorkDone(options) && BucketingUtil.isWorkComplete(coordinatorActivityState)) {
                closeAllWorkers(result);
            }

            int closedBecauseDone = or0(reconciliationResult.getClosedDone());
            result.recordStatus(OperationResultStatus.SUCCESS, String.format("Worker reconciliation finished. Original workers: %d,"
                            + " should be: %d, matched: %d, renamed: %d, adapted: %d, suspended: %d, created: %d worker task(s).%s",
                    startingWorkersCount, startingShouldBeWorkersCount,
                    reconciliationResult.getMatched(), reconciliationResult.getRenamed(), reconciliationResult.getAdapted(),
                    reconciliationResult.getSuspended(), reconciliationResult.getCreated(),
                    (closedBecauseDone > 0 ? " Closed " + closedBecauseDone + " workers because the work is done." : "")));

            return reconciliationResult;
        } catch (TaskModificationConflictException e) {
            result.recordWarning("Conflicting worker task modification detected. Reconciliation aborted.");
            return reconciliationResult;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            reconciliationResult.status(OperationResultStatus.createStatusType(result.getStatus()));
        }
    }

    private void resumeSelectedWorkers(OperationResult result) {
        if (!workersToResume.isEmpty()) {
            LOGGER.info("Resuming suspended workers: {}", workersToResume);
            beans.taskManager.resumeTasks(workersToResume, result);
        }
        reconciliationResult.setResumed(workersToResume.size());
    }

    private void initialize() throws SchemaException, ConfigurationException {
        activity = beans.activityManager.getActivity(rootTask, activityPath);
        ActivityDistributionDefinition distributionDefinition = activity.getDistributionDefinition();

        // We will eventually remove this constraint, to be able to convert distributed to non-distributed tasks.
        workersDefinitionBean = distributionDefinition.getWorkers();
        argCheck(workersDefinitionBean != null, "Activity %s in %s (%s) has no workers defined",
                activityPath, rootTask, coordinatorTask);

        coordinatorActivityState = ActivityStateUtil.getActivityState(coordinatorTask.getActivitiesStateOrClone(), activityPath);

        if (coordinatorActivityState != null) {
            argCheck(BucketingUtil.isCoordinator(coordinatorActivityState), "Activity %s in %s (%s) is not a coordinator",
                    activityPath, rootTask, coordinatorTask);
        }
    }

    private @NotNull List<Task> getCurrentWorkersSorted(OperationResult result) throws SchemaException {
        List<Task> currentWorkers = getCurrentWorkers(result);
        sortCurrentWorkers(currentWorkers);
        return currentWorkers;
    }

    private void sortCurrentWorkers(List<Task> currentWorkers) {
        currentWorkers.sort(Comparator.comparing(t -> {
            if (t.getSchedulingState() == TaskSchedulingStateType.READY) {
                if (t.getExecutionState() == TaskExecutionStateType.RUNNING) {
                    return 0;
                } else if (t.getNode() != null) {
                    return 1;
                } else {
                    return 2;
                }
            } else if (t.getSchedulingState() == TaskSchedulingStateType.SUSPENDED) {
                return 3;
            } else if (t.getSchedulingState() == TaskSchedulingStateType.CLOSED) {
                return 4;
            } else {
                return 5;
            }
        }));
    }

    public @NotNull List<Task> getCurrentWorkers(OperationResult result) throws SchemaException {
        List<? extends Task> allChildren = coordinatorTask.listSubtasks(true, result);
        List<Task> relevantChildren = allChildren.stream()
                .filter(this::isRelevantWorker)
                .collect(Collectors.toList());
        LOGGER.trace("Found {} relevant workers out of {} children: {}",
                relevantChildren.size(), allChildren.size(), relevantChildren);
        return relevantChildren;
    }

    private boolean isRelevantWorker(Task worker) {
        TaskActivityStateType workState = worker.getWorkState();
        return workState != null &&
                workState.getTaskRole() == TaskRoleType.WORKER &&
                activityPath.equalsBean(workState.getLocalRoot());
    }

    /**
     * The easiest step: we just match current workers with the 'should be' state. Matching item pairs are skipped
     * from further processing.
     */
    private void skipMatchingWorkers() {
        int count = 0;
        for (Task currentWorker : new ArrayList<>(currentWorkers)) {
            Optional<WorkerCharacterization> matching = WorkerCharacterization.find(
                    shouldBeWorkers,
                    currentWorker.getGroup(),
                    PolyString.getOrig(currentWorker.getName()),
                    BucketingUtil.isScavenger(currentWorker.getWorkState(), activityPath));
            if (matching.isPresent()) {
                LOGGER.trace("Found fully matching as-is/to-be pair: {} and {}", matching.get(), currentWorker);
                scheduleToResumeIfSuspended(currentWorker);
                shouldBeWorkers.remove(matching.get());
                currentWorkers.remove(currentWorker);
                count++;
            }
        }
        LOGGER.trace("After skipMatchingWorkers (matched: {}):\nCurrent workers: {}\nShould be workers: {}",
                count, currentWorkers, shouldBeWorkers);
        reconciliationResult.setMatched(count);
    }

    private void scheduleToResumeIfSuspended(Task currentWorker) {
        if (currentWorker.isSuspended()) {
            LOGGER.info("Worker {} is needed. It will be resumed.", currentWorker);
            workersToResume.add(currentWorker.getOid());
        }
    }

    /**
     * Workers that are compatible with the "to be" state (i.e. have correct group + scavenger flag) but not matching
     * name are simply renamed.
     */
    private void renameCompatibleWorkers(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, TaskModificationConflictException {
        reconciliationResult.setRenamed(0);

        for (Task currentWorker : new ArrayList<>(currentWorkers)) {

            Optional<WorkerCharacterization> compatible = WorkerCharacterization.find(
                    shouldBeWorkers,
                    currentWorker.getGroup(),
                    BucketingUtil.isScavenger(currentWorker.getWorkState(), activityPath));

            if (compatible.isPresent()) {
                LOGGER.trace("Found compatible as-is/to-be pair: {} and {}", compatible.get(), currentWorker);
                renameWorker(currentWorker, compatible.get().name, result);
                scheduleToResumeIfSuspended(currentWorker);
                shouldBeWorkers.remove(compatible.get());
                currentWorkers.remove(currentWorker);

                // Placed in the cycle to have the value recorded even if an exception occurs later.
                reconciliationResult.setRenamed(reconciliationResult.getRenamed() + 1);
            }
        }
        LOGGER.trace("After renameCompatibleWorkers (result: {}):\nCurrent workers: {}\nShould be workers: {}",
                reconciliationResult.getRenamed(), currentWorkers, shouldBeWorkers);
    }

    /**
     * Group-compatible workers that are placed in correct group (but otherwise wrong)
     * are fixed by renaming and setting scavenger flag.
     */
    private void adaptGroupCompatibleWorkers(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, TaskModificationConflictException {
        reconciliationResult.setAdapted(0);

        for (Task currentWorker : new ArrayList<>(currentWorkers)) {
            Optional<WorkerCharacterization> groupCompatible = WorkerCharacterization.find(
                    shouldBeWorkers,
                    currentWorker.getGroup());
            if (groupCompatible.isPresent()) {
                LOGGER.trace("Found group-compatible as-is/to-be pair: {} and {}", groupCompatible.get(), currentWorker);
                adaptWorker(currentWorker, groupCompatible.get().name, groupCompatible.get().scavenger, result);
                scheduleToResumeIfSuspended(currentWorker);
                shouldBeWorkers.remove(groupCompatible.get());
                currentWorkers.remove(currentWorker);

                // Placed in the cycle to have the value recorded even if an exception occurs later.
                reconciliationResult.setAdapted(reconciliationResult.getAdapted() + 1);
            }
        }
        LOGGER.trace("After adaptGroupCompatibleWorkers (result: {}):\nCurrent workers: {}\nShould be workers: {}",
                reconciliationResult.getAdapted(), currentWorkers, shouldBeWorkers);
    }

    /**
     * We suspend only those workers that run on live nodes!
     *
     * It is because we expect that the task will return its buckets when it is suspended. And in order to do this,
     * it must be running - obviously.
     *
     * As for the tasks on dead and semi-dead (up but not checking-in) nodes, their buckets were or will be released
     * by the cluster manager thread - when these nodes are discovered to be dead.
     *
     * TODO What if a task runs on a recently-killed node (still in grace period of 30 seconds)?
     *  It will get suspended here, but the buckets will not be released.
     *  (This can also occur if a task is suspended, but the node is killed shortly after that.)
     *  See MID-7180.
     */
    private void suspendRunningWorkersOnLiveNodes(OperationResult result) {
        List<Task> runningWorkersOnLiveNodes = currentWorkers.stream()
                .filter(Task::isRunning)
                .filter(t -> expectedSetup.getNodesUpAndAlive().contains(t.getNode()))
                .collect(Collectors.toList());

        Collection<String> workerOids = runningWorkersOnLiveNodes.stream()
                .map(Task::getOid)
                .collect(Collectors.toSet());

        if (!workerOids.isEmpty()) {
            // TODO suspend gracefully (let the task finish current bucket, to avoid objects being re-processed)
            beans.taskManager.suspendTasks(workerOids, SUSPENSION_WAIT_TIME, result);
        }

        int count = workerOids.size();
        LOGGER.trace("After suspendRunningWorkersOnLiveNodes (suspended: {}):\nCurrent workers: {}", count, currentWorkers);
        reconciliationResult.setSuspended(count);
    }

    /**
     * Finally, create remaining workers.
     */
    private void createWorkers(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {

        Map<WorkerCharacterization, WorkersPerNodeDefinitionType> perNodeDefinitionMap =
                expectedSetup.getWorkersDefinition();

        WorkerState workerState = shouldCreateSuspended(options) ?
                WorkerState.SUSPENDED : determineWorkerState();

        int count = 0;
        for (WorkerCharacterization shouldBeWorker : shouldBeWorkers) {
            if (isScavenging() && !shouldBeWorker.scavenger) {
                LOGGER.trace("Skipping creation of non-scavenger, as we are in scavenging phase: {}", shouldBeWorker);
            } else {
                createWorker(shouldBeWorker, perNodeDefinitionMap, workerState, result);
                count++;
            }
        }
        reconciliationResult.setCreated(count);
    }

    private WorkerState determineWorkerState() {
        if (coordinatorTask.getSchedulingState() == null) {
            throw new IllegalStateException("Null scheduling state of " + coordinatorTask);
        }
        switch (coordinatorTask.getSchedulingState()) {
            case WAITING:
                return WorkerState.READY;
            case SUSPENDED:
            case READY:
                return WorkerState.SUSPENDED;
            case CLOSED: // not very useful
                return WorkerState.CLOSED;
            default:
                throw new IllegalStateException("Unsupported scheduling state of " + coordinatorTask + ": " +
                        coordinatorTask.getSchedulingState());
        }
    }

    private boolean isScavenging() {
        return Boolean.TRUE.equals(coordinatorActivityState.getBucketing().isScavenging());
    }

    private void createWorker(WorkerCharacterization workerCharacterization,
            Map<WorkerCharacterization, WorkersPerNodeDefinitionType> perNodeDefinitionMap,
            WorkerState workerState, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        TaskType worker = new TaskType();
        worker.setName(PolyStringType.fromOrig(workerCharacterization.name));
        if (workerCharacterization.group != null) {
            worker.beginExecutionConstraints().group(workerCharacterization.group).end();
        }
        applyDeltas(worker, workersDefinitionBean.getOtherDeltas());
        applyDeltas(worker, perNodeDefinitionMap.get(workerCharacterization).getOtherDeltas());

        worker.setExecutionState(workerState.executionState);
        worker.setSchedulingState(workerState.schedulingState);
        worker.setOwnerRef(CloneUtil.clone(coordinatorTask.getOwnerRef()));
        worker.setParent(coordinatorTask.getTaskIdentifier());
        worker.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
        worker.beginActivityState()
                .localRoot(activityPath.toBean())
                .taskRole(TaskRoleType.WORKER)
                .beginActivity()
                    .beginBucketing()
                        .bucketsProcessingRole(BucketsProcessingRoleType.WORKER)
                        .scavenger(workerCharacterization.scavenger);

        LOGGER.info("Creating worker task on {}: {} for activity path '{}'",
                workerCharacterization.group, workerCharacterization.name, activityPath);
        beans.taskManager.addTask(worker.asPrismObject(), result);
    }

    private void applyDeltas(TaskType worker, List<ItemDeltaType> deltas) throws SchemaException {
        Collection<? extends ItemDelta<?, ?>> itemDeltas =
                DeltaConvertor.toModifications(deltas, worker.asPrismObject().getDefinition());
        ItemDeltaCollectionsUtil.applyTo(itemDeltas, worker.asPrismContainerValue());
    }

    private void renameWorker(Task old, String newName, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, TaskModificationConflictException {
        List<ItemDelta<?, ?>> itemDeltas = beans.prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_NAME).replace(PolyString.fromOrig(newName))
                .asItemDeltas();
        LOGGER.info("Renaming worker task {} to {}", old, newName);

        ModificationPrecondition<TaskType> precondition = current -> isNameOk(old, current, newName);
        try {
            beans.repositoryService.modifyObject(TaskType.class, old.getOid(), itemDeltas, precondition, null, result);
        } catch (PreconditionViolationException e) {
            throw new TaskModificationConflictException();
        }
    }

    /**
     * @param old Worker we fetched from repo at the beginning
     * @param current Worker that is currently in the repo (at the transaction start)
     * @param newValue Name to be set
     */
    private boolean isNameOk(Task old, PrismObject<TaskType> current, String newValue) {
        String oldValue = old.getName().getOrig();
        String currentValue = current.asObjectable().getName().getOrig();

        // we accept also the situation where someone else fixes the name
        return currentValue.equals(oldValue) || currentValue.equals(newValue);
    }

    /** @see #isNameOk(Task, PrismObject, String) */
    private boolean isScavengerFlagOk(Task old, PrismObject<TaskType> current, boolean newValue) {
        boolean oldValue = BucketingUtil.isScavenger(old.getWorkState(), activityPath);
        boolean currentValue = BucketingUtil.isScavenger(current.asObjectable().getActivityState(), activityPath);

        // we accept also the situation where someone else fixes the value
        // (if oldValue != newValue then this returns always true; but there might be cases where oldValue = newValue)
        return currentValue == oldValue || currentValue == newValue;
    }

    private void adaptWorker(Task old, String newName, boolean newScavengerFlag, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, TaskModificationConflictException {
        List<ItemDelta<?, ?>> itemDeltas = beans.prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_NAME).replace(PolyString.fromOrig(newName))
                .item(SCAVENGER_PATH).replace(newScavengerFlag)
                .asItemDeltas();
        LOGGER.info("Adapting worker task {} to {} (scavenger = {})", old, newName, newScavengerFlag);

        ModificationPrecondition<TaskType> precondition =
                current -> isNameOk(old, current, newName) && isScavengerFlagOk(old, current, newScavengerFlag);

        try {
            beans.repositoryService.modifyObject(TaskType.class, old.getOid(), itemDeltas, precondition, null, result);
        } catch (PreconditionViolationException e) {
            throw new TaskModificationConflictException();
        }
    }

    private void closeAllWorkers(OperationResult result) throws SchemaException {
        int count = 0;
        for (Task worker : getCurrentWorkers(result)) {
            if (worker.getSchedulingState() != TaskSchedulingStateType.CLOSED) {
                LOGGER.info("Closing worker because the work is done: {}", worker);
                try {
                    beans.taskManager.suspendAndCloseTaskNoException(worker, TaskManager.DO_NOT_WAIT, result);
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close task {}", e, worker);
                }
                count++;
            }
        }
        reconciliationResult.setClosedDone(count);
    }

    private enum WorkerState {
        READY(TaskExecutionStateType.RUNNABLE, TaskSchedulingStateType.READY),
        SUSPENDED(TaskExecutionStateType.SUSPENDED, TaskSchedulingStateType.SUSPENDED),
        CLOSED(TaskExecutionStateType.CLOSED, TaskSchedulingStateType.CLOSED);

        private final TaskExecutionStateType executionState;
        private final TaskSchedulingStateType schedulingState;

        WorkerState(TaskExecutionStateType executionState, TaskSchedulingStateType schedulingState) {
            this.executionState = executionState;
            this.schedulingState = schedulingState;
        }
    }
}
