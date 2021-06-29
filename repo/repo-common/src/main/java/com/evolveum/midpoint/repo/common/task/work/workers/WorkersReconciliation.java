/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work.workers;

import static com.evolveum.midpoint.repo.common.task.work.workers.WorkersReconciliationOptions.shouldCloseWorkersOnWorkDone;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiValuedMap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
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

public class WorkersReconciliation {

    private static final Trace LOGGER = TraceManager.getTrace(WorkersReconciliation.class);

    @NotNull private final Task rootTask;

    @NotNull private final Task coordinatorTask;

    @NotNull private final ActivityPath activityPath;

    private final WorkersReconciliationOptions options;

    @NotNull private final CommonTaskBeans beans;

    private Activity<?, ?> activity;

    private ActivityStateType coordinatorActivityState;

    private WorkersManagementType workersConfigBean;

    private ExpectedWorkersSetup expectedWorkersSetup;

    private List<Task> currentWorkers;

    private MultiValuedMap<String, WorkerSpec> shouldBeWorkers;

    @NotNull private final WorkersReconciliationResultType reconciliationResult;

    public WorkersReconciliation(@NotNull Task rootTask, @NotNull Task coordinatorTask,
            @NotNull ActivityPath activityPath, WorkersReconciliationOptions options,
            @NotNull CommonTaskBeans beans) {
        this.rootTask = rootTask;
        this.coordinatorTask = coordinatorTask;
        this.activityPath = activityPath;
        this.options = options;
        this.beans = beans;
        this.reconciliationResult = new WorkersReconciliationResultType(beans.prismContext);
    }

    public void execute(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        initialize();

        if (coordinatorActivityState == null) {
            result.recordNotApplicable("Activity has not run yet.");
            return;
        }

        expectedWorkersSetup = ExpectedWorkersSetup.create(activity, workersConfigBean, beans, coordinatorTask, rootTask, result);

        currentWorkers = getCurrentWorkers(result);
        shouldBeWorkers = expectedWorkersSetup.getWorkersMap();

        int startingWorkersCount = currentWorkers.size();
        int startingShouldBeWorkersCount = shouldBeWorkers.size();

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

        LOGGER.trace("Before reconciliation:\nCurrent workers: {}\nShould be workers: {}", currentWorkers, shouldBeWorkers);

        matchWorkers();
        renameWorkers(result);
        closeExecutingWorkers(result);
        moveWorkers(result);
        createWorkers(result);

        // TODO ensure that enough scavengers are present

        if (shouldCloseWorkersOnWorkDone(options) && BucketingUtil.isWorkComplete(coordinatorActivityState)) {
            closeAllWorkers(result);
        }

        int closedBecauseDone = or0(reconciliationResult.getClosedDone());
        result.recordStatus(OperationResultStatus.SUCCESS, String.format("Worker reconciliation finished. Original workers: %d,"
                        + " should be: %d, matched: %d, renamed: %d, closed because executing: %d, moved: %d, closed because"
                        + " superfluous: %d, created: %d worker task(s).%s",
                startingWorkersCount, startingShouldBeWorkersCount,
                reconciliationResult.getMatched(), reconciliationResult.getRenamed(), reconciliationResult.getClosedExecuting(),
                reconciliationResult.getMoved(), reconciliationResult.getClosedSuperfluous(), reconciliationResult.getCreated(),
                (closedBecauseDone > 0 ? " Closed " + closedBecauseDone + " workers because the work is done." : "")));
    }

    private void initialize() throws SchemaException {
        activity = beans.activityManager.getActivity(rootTask, activityPath);
        ActivityDistributionDefinition distributionDefinition = activity.getDistributionDefinition();

        // We will eventually remove this constraint, to be able to convert distributed to non-distributed tasks.
        workersConfigBean = distributionDefinition.getWorkers();
        argCheck(workersConfigBean != null, "Activity %s in %s (%s) has no workers defined",
                activityPath, rootTask, coordinatorTask);

        coordinatorActivityState = ActivityStateUtil.getActivityState(coordinatorTask.getActivitiesStateOrClone(), activityPath);

        if (coordinatorActivityState != null) {
            argCheck(BucketingUtil.isCoordinator(coordinatorActivityState), "Activity %s in %s (%s) is not a coordinator",
                    activityPath, rootTask, coordinatorTask);
        }
    }

    public @NotNull List<Task> getCurrentWorkers(OperationResult result) throws SchemaException {
        List<? extends Task> allChildren = coordinatorTask.listSubtasks(true, result);
        List<Task> relevantChildren = allChildren.stream()
                .filter(this::isRelevantWorker)
                .collect(Collectors.toList());
        LOGGER.debug("Found {} relevant workers out of {} children: {}",
                relevantChildren.size(), allChildren.size(), relevantChildren);
        return relevantChildren;
    }

    private boolean isRelevantWorker(Task worker) {
        TaskActivityStateType workState = worker.getWorkState();
        return workState != null &&
                workState.getLocalRootActivityExecutionRole() == ActivityExecutionRoleType.WORKER &&
                activityPath.equalsBean(workState.getLocalRoot());
    }

    /**
     * The easiest step: we just match current workers with the 'should be' state. Matching items are deleted from both sides.
     */
    private void matchWorkers() {
        int count = 0;
        for (Task currentWorker : new ArrayList<>(currentWorkers)) {
            WorkerSpec currentWorkerSpec = WorkerSpec.forTask(currentWorker, activityPath);
            if (shouldBeWorkers.containsValue(currentWorkerSpec)) {
                shouldBeWorkers.removeMapping(currentWorkerSpec.group, currentWorkerSpec);
                currentWorkers.remove(currentWorker);
                count++;
            }
        }
        LOGGER.trace("After matchWorkers (result: {}):\nCurrent workers: {}\nShould be workers: {}", count, currentWorkers, shouldBeWorkers);
        reconciliationResult.setMatched(count);
    }

    /**
     * Going through the groups and renaming wrongly-named tasks to the correct names.
     */
    private void renameWorkers(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        int count = 0;
        for (String shouldBeGroup : shouldBeWorkers.keySet()) {
            Collection<WorkerSpec> shouldBeWorkersInGroup = shouldBeWorkers.get(shouldBeGroup);
            for (Task currentWorker : new ArrayList<>(currentWorkers)) {
                if (Objects.equals(shouldBeGroup, currentWorker.getGroup())) {
                    if (!shouldBeWorkersInGroup.isEmpty()) {
                        WorkerSpec nextWorker = shouldBeWorkersInGroup.iterator().next();
                        renameWorker(currentWorker, nextWorker.name, result);
                        currentWorkers.remove(currentWorker);
                        shouldBeWorkersInGroup.remove(nextWorker);
                        count++;
                    } else {
                        break; // no more workers for this group
                    }
                }
            }
        }
        LOGGER.trace("After renameWorkers (result: {}):\nCurrent workers: {}\nShould be workers: {}",
                count, currentWorkers, shouldBeWorkers);
        reconciliationResult.setRenamed(count);
    }

    private void renameWorker(Task currentWorker, String newName, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> itemDeltas = beans.prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_NAME).replace(PolyString.fromOrig(newName))
                .asItemDeltas();
        LOGGER.info("Renaming worker task {} to {}", currentWorker, newName);
        beans.repositoryService.modifyObject(TaskType.class, currentWorker.getOid(), itemDeltas, result);
    }

    private void closeExecutingWorkers(OperationResult result) {
        int count = 0;
        for (Task worker : new ArrayList<>(currentWorkers)) {
            if (worker.getSchedulingState() == TaskSchedulingStateType.READY && worker.getNodeAsObserved() != null) { // todo
                LOGGER.info("Closing (also suspending if needed) misplaced worker task {}", worker);
                beans.taskManager.suspendAndCloseTaskNoException(worker, TaskManager.DO_NOT_WAIT, result);
                try {
                    worker.refresh(result);
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh worker task {}", t, worker);
                }
                count++;
            }
        }
        LOGGER.trace("After closeExecutingWorkers (result: {}):\nCurrent workers: {}", count, currentWorkers);
        reconciliationResult.setClosedExecuting(count);
    }

    /**
     * Moving workers to correct groups (and renaming them if needed).
     * We assume none of the workers are currently being executed.
     */
    private void moveWorkers(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        int moved = 0, closed = 0;
        Iterator<WorkerSpec> shouldBeIterator = shouldBeWorkers.values().iterator();
        for (Task worker : new ArrayList<>(currentWorkers)) {
            if (shouldBeIterator.hasNext()) {
                WorkerSpec shouldBeNext = shouldBeIterator.next();
                moveWorker(worker, shouldBeNext, result);
                currentWorkers.remove(worker);
                shouldBeIterator.remove();
                moved++;
            } else {
                if (!worker.isClosed()) {
                    LOGGER.info("Closing superfluous worker task {}", worker);
                    beans.taskManager.suspendAndCloseTaskNoException(worker, TaskManager.DO_NOT_WAIT, result);
                    closed++;
                }
            }
        }
        LOGGER.trace("After moveWorkers (result: {} moved, {} closed):\nCurrent workers: {}\nShould be workers: {}", moved,
                closed, currentWorkers, shouldBeWorkers);
        reconciliationResult.setMoved(moved);
        reconciliationResult.setClosedSuperfluous(closed);
    }

    /**
     * Finally, create remaining workers.
     */
    private void createWorkers(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {

        Map<WorkerSpec, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap =
                expectedWorkersSetup.getPerNodeConfigurationMap();

        WorkerState workerState = determineWorkerState();
        int count = 0;
        for (WorkerSpec workerSpec : shouldBeWorkers.values()) {
            createWorker(workerSpec, perNodeConfigurationMap, workerState, result);
            count++;
        }
        reconciliationResult.setCreated(count);
    }

    private WorkerState determineWorkerState() {
        if (WorkersReconciliationOptions.shouldCloseWorkersOnWorkDone(options)) {
            return WorkerState.SUSPENDED;
        }

        if (coordinatorTask.getSchedulingState() == null) {
            throw new IllegalStateException("Null executionStatus of " + coordinatorTask);
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

    private void createWorker(WorkerSpec workerSpec, Map<WorkerSpec, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap,
            WorkerState workerState, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        TaskType worker = new TaskType(beans.prismContext);
        worker.setName(PolyStringType.fromOrig(workerSpec.name));
        if (workerSpec.group != null) {
            worker.beginExecutionConstraints().group(workerSpec.group).end();
        }
        applyDeltas(worker, workersConfigBean.getOtherDeltas());
        applyDeltas(worker, perNodeConfigurationMap.get(workerSpec).getOtherDeltas());

        worker.setExecutionStatus(workerState.executionState);
        worker.setSchedulingState(workerState.schedulingState);
        worker.setOwnerRef(CloneUtil.clone(coordinatorTask.getOwnerRef()));
        worker.setRecurrence(TaskRecurrenceType.SINGLE);
        worker.setParent(coordinatorTask.getTaskIdentifier());
        worker.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
        worker.beginActivityState()
                .localRoot(activityPath.toBean())
                .localRootActivityExecutionRole(ActivityExecutionRoleType.WORKER)
                .beginActivity()
                    .beginBucketing()
                        .bucketsProcessingRole(BucketsProcessingRoleType.WORKER)
                        .scavenger(workerSpec.scavenger);
        LOGGER.info("Creating worker task on {}: {} for activity path '{}'", workerSpec.group, workerSpec.name, activityPath);
        beans.taskManager.addTask(worker.asPrismObject(), result);
    }

    private void applyDeltas(TaskType worker, List<ItemDeltaType> deltas) throws SchemaException {
        Collection<? extends ItemDelta<?, ?>> itemDeltas =
                DeltaConvertor.toModifications(deltas, worker.asPrismObject().getDefinition());
        ItemDeltaCollectionsUtil.applyTo(itemDeltas, worker.asPrismContainerValue());
    }

    private void moveWorker(Task worker, WorkerSpec shouldBe, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> itemDeltas = beans.prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_EXECUTION_CONSTRAINTS, TaskExecutionConstraintsType.F_GROUP).replace(shouldBe.group)
                .item(TaskType.F_NAME).replace(PolyString.fromOrig(shouldBe.name))
                .asItemDeltas();
        LOGGER.info("Moving worker task {} to {} as {}", worker, shouldBe.group, shouldBe.name);
        beans.taskManager.modifyTask(worker.getOid(), itemDeltas, result);
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
