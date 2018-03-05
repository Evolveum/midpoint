/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.work;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategy.GetBucketResult;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategy.GetBucketResult.FoundExisting;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategy.GetBucketResult.NewBuckets;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategy.GetBucketResult.NothingFound;
import com.evolveum.midpoint.task.quartzimpl.work.strategy.WorkStateManagementStrategyFactory;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.task.quartzimpl.work.WorkBucketUtil.findBucketByNumber;
import static java.util.Collections.singletonList;

/**
 * Responsible for managing task work state.
 *
 * @author mederly
 */

@Component
public class WorkStateManager {

	private static final Trace LOGGER = TraceManager.getTrace(WorkStateManager.class);

	@Autowired private TaskManagerQuartzImpl taskManager;
	@Autowired private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;
	@Autowired private WorkStateManagementStrategyFactory strategyFactory;

	public static final int MAX_ATTEMPTS = 40;                              // temporary
	public static final long DELAY_INTERVAL = 5000L;                        // temporary
	public static final long FREE_BUCKET_WAIT_INTERVAL = 120000L;

	private class Context {
		Task workerTask;
		Task coordinatorTask;           // null for standalone worker tasks

		public boolean isStandalone() {
			WorkStateManagementTaskKindType kind = workerTask.getWorkStateManagement().getTaskKind();
			return kind == null || kind == WorkStateManagementTaskKindType.STANDALONE;
		}

		public void reloadCoordinatorTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
			coordinatorTask = taskManager.getTask(coordinatorTask.getOid(), null, result);
		}

		public void reloadWorkerTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
			workerTask = taskManager.getTask(workerTask.getOid(), null, result);
		}
	}

	/**
	 * Allocates work bucket. If no free work buckets are currently present it tries to create one.
	 * If there is already allocated work bucket for given worker task, it is returned.
	 *
	 * Finding/creation of free bucket is delegated to the work state management strategy.
	 * This method implements mainly the act of allocation - i.e. modification of the task work state in repository.
	 *
	 * WE ASSUME THIS METHOD IS CALLED FROM THE WORKER TASK; SO IT IS NOT NECESSARY TO SYNCHRONIZE ACCESS TO THIS TASK WORK STATE.
	 *
	 * @pre task is persistent and has work state management configured
	 */
	public AbstractWorkBucketType getWorkBucket(@NotNull String workerTaskOid, long freeBucketWaitTime,
			@NotNull OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		Context ctx = createContext(workerTaskOid, result);
		AbstractWorkBucketType bucket = findSelfAllocatedBucket(ctx);
		if (bucket != null) {
			return bucket;
		}
		if (ctx.isStandalone()) {
			return getWorkBucketStandalone(ctx, result);
		} else {
			return getWorkBucketMultiNode(ctx, freeBucketWaitTime, result);
		}
	}

	private AbstractWorkBucketType findSelfAllocatedBucket(Context ctx) {
		TaskWorkStateType workState = ctx.workerTask.getTaskType().getWorkState();
		if (workState == null || workState.getBucket().isEmpty()) {
			return null;
		}
		List<AbstractWorkBucketType> buckets = new ArrayList<>(workState.getBucket());
		WorkBucketUtil.sortBucketsBySequentialNumber(buckets);
		for (AbstractWorkBucketType bucket : buckets) {
			if (bucket.getState() == WorkBucketStateType.READY) {
				return bucket;
			}
		}
		return null;
	}

	private AbstractWorkBucketType getWorkBucketMultiNode(Context ctx, long freeBucketWaitTime, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		long start = System.currentTimeMillis();
		WorkStateManagementStrategy workStateStrategy = strategyFactory.createStrategy(ctx.coordinatorTask.getWorkStateManagement());

waitForAvailableBucket:    // this cycle exits when something is found OR when a definite 'no more buckets' answer is received
	    for (;;) {
waitForConflictLessUpdate: // this cycle exits when coordinator task update succeeds
			for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
				TaskWorkStateType coordinatorWorkState = getWorkStateOrNew(ctx.coordinatorTask.getTaskPrismObject());
				GetBucketResult response = workStateStrategy.getBucket(coordinatorWorkState);
				try {
					if (response instanceof NewBuckets) {
						NewBuckets newBucketsResponse = (NewBuckets) response;
						List<AbstractWorkBucketType> newCoordinatorBuckets = new ArrayList<>(coordinatorWorkState.getBucket());
						for (int i = 0; i < newBucketsResponse.newBuckets.size(); i++) {
							if (i == 0) {
								newCoordinatorBuckets.add(newBucketsResponse.newBuckets.get(i).clone().state(WorkBucketStateType.DELEGATED));
							} else {
								newCoordinatorBuckets.add(newBucketsResponse.newBuckets.get(i).clone());
							}
						}
						repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
								bucketsReplaceDeltas(newCoordinatorBuckets),
								bucketsReplacePrecondition(coordinatorWorkState.getBucket()), null, result);
						repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
								bucketsAddDeltas(newBucketsResponse.newBuckets), null, result);
						return newBucketsResponse.newBuckets.get(0);
					} else if (response instanceof FoundExisting) {
						FoundExisting existingResponse = (FoundExisting) response;
						repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
								bucketStateChangeDeltas(existingResponse.bucket, WorkBucketStateType.DELEGATED),
								bucketUnchangedPrecondition(existingResponse.bucket), null, result);
						AbstractWorkBucketType foundBucket = existingResponse.bucket.clone();
						repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
								bucketsAddDeltas(singletonList(foundBucket)), null, result);
						return foundBucket;
					} else if (response instanceof NothingFound) {
						if (((NothingFound) response).definite || freeBucketWaitTime == 0L) {
							return null;
						} else {
							long waitDeadline = freeBucketWaitTime >= 0 ? start + freeBucketWaitTime : Long.MAX_VALUE;
							long toWait = waitDeadline - System.currentTimeMillis();
							if (toWait <= 0) {
								return null;
							}
							try {
								Thread.sleep(Math.min(toWait, FREE_BUCKET_WAIT_INTERVAL));
							} catch (InterruptedException e) {
								// TODO
							}
							ctx.reloadCoordinatorTask(result);
							ctx.reloadWorkerTask(result);
							if (reclaimWronglyAllocatedBuckets(ctx.coordinatorTask, result)) {
								ctx.reloadCoordinatorTask(result);
							}
							// we continue even if we could not find any wrongly allocated bucket -- maybe someone else found
							// them before us
							continue waitForAvailableBucket;
						}
					} else {
						throw new AssertionError(response);
					}
				} catch (PreconditionViolationException e) {
					long delay = (long) (Math.random() * DELAY_INTERVAL);
					LOGGER.debug("getWorkBucketMultiNode: conflict; this was attempt #{}; waiting {} ms", attempt, delay, e);
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e1) {
						// TODO
					}
					ctx.reloadCoordinatorTask(result);
					ctx.reloadWorkerTask(result);
					//noinspection UnnecessaryContinue,UnnecessaryLabelOnContinueStatement
					continue waitForConflictLessUpdate;
				}
			}
			throw new SystemException(
					"Couldn't allocate work bucket because of database conflicts; coordinator task = " + ctx.coordinatorTask);
		}
	}

	/**
	 * For each allocated work bucket we check if it is allocated to existing and non-closed child task.
	 * Returns true if there was something to reclaim.
	 */
	private boolean reclaimWronglyAllocatedBuckets(Task coordinatorTask, OperationResult result)
			throws SchemaException, PreconditionViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		List<Task> workers = coordinatorTask.listSubtasks(result);
		if (coordinatorTask.getWorkState() == null) {
			return false;
		}
		TaskWorkStateType originalState = coordinatorTask.getWorkState().clone();
		TaskWorkStateType newState = coordinatorTask.getWorkState().clone();
		boolean changed = false;
		for (AbstractWorkBucketType bucket : newState.getBucket()) {
			if (bucket.getState() == WorkBucketStateType.DELEGATED) {
				Task worker = WorkBucketUtil.findWorkerByBucketNumber(workers, bucket.getSequentialNumber());
				if (worker == null || worker.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
					LOGGER.info("Reclaiming wrongly allocated work bucket {} from worker task {}", bucket, worker);
					bucket.setState(WorkBucketStateType.READY);
					// TODO modify also the worker
					changed = true;
				}
			}
		}
		if (changed) {
			repositoryService.modifyObject(TaskType.class, coordinatorTask.getOid(),
					bucketsReplaceDeltas(newState.getBucket()),
					bucketsReplacePrecondition(originalState.getBucket()), null, result);
		}
		return changed;
	}

	private AbstractWorkBucketType getWorkBucketStandalone(Context ctx, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		WorkStateManagementStrategy workStateStrategy = strategyFactory.createStrategy(ctx.workerTask.getWorkStateManagement());
		TaskWorkStateType workState = getWorkStateOrNew(ctx.workerTask.getTaskPrismObject());
		GetBucketResult response = workStateStrategy.getBucket(workState);
		if (response instanceof FoundExisting) {
			throw new AssertionError("Found unallocated buckets in standalone worker task on a second pass: " + ctx.workerTask);
		} else if (response instanceof NewBuckets) {
			NewBuckets newBucketsResponse = (NewBuckets) response;
			repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
					bucketsAddDeltas(newBucketsResponse.newBuckets), null, result);
			return newBucketsResponse.newBuckets.get(0);
		} else if (response instanceof NothingFound) {
			if (!((NothingFound) response).definite) {
				throw new AssertionError("Unexpected 'indefinite' answer when looking for next bucket in a standalone task: " + ctx.workerTask);
			}
			return null;
		} else {
			throw new AssertionError(response);
		}
	}

	private Context createContext(String workerTaskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Context ctx = new Context();
		ctx.workerTask = taskManager.getTask(workerTaskOid, result);
		AbstractTaskWorkStateManagementConfigurationType workerWorkStateMgmt = ctx.workerTask.getWorkStateManagement();
		if (workerWorkStateMgmt == null) {
			throw new IllegalStateException("No state management configuration in worker task " + ctx.workerTask);
		}
		if (workerWorkStateMgmt.getTaskKind() != null && workerWorkStateMgmt.getTaskKind() != WorkStateManagementTaskKindType.WORKER &&
				workerWorkStateMgmt.getTaskKind() != WorkStateManagementTaskKindType.STANDALONE) {
			throw new IllegalStateException("Wrong task kind for worker task " + ctx.workerTask + ": " + workerWorkStateMgmt.getTaskKind());
		}
		if (workerWorkStateMgmt.getTaskKind() == WorkStateManagementTaskKindType.WORKER) {
			ctx.coordinatorTask = getCoordinatorTask(ctx.workerTask, result);
		}
		return ctx;
	}

	private Task getCoordinatorTask(Task workerTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Task parent = workerTask.getParentTask(result);
		if (parent == null) {
			throw new IllegalStateException("No coordinator task for worker task " + workerTask);
		}
		AbstractTaskWorkStateManagementConfigurationType workStateManagement = parent.getWorkStateManagement();
		if (workStateManagement == null || workStateManagement.getTaskKind() != WorkStateManagementTaskKindType.COORDINATOR) {
			throw new IllegalStateException("Coordinator task for worker task " + workerTask + " is not marked as such: " + parent);
		}
		return parent;
	}

	public void completeWorkBucket(String workerTaskOid, int sequentialNumber, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		Context ctx = createContext(workerTaskOid, result);
		if (ctx.isStandalone()) {
			completeWorkBucketStandalone(ctx, sequentialNumber, result);
		} else {
			completeWorkBucketMultiNode(ctx, sequentialNumber, result);
		}
	}

	private void completeWorkBucketMultiNode(Context ctx, int sequentialNumber, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		TaskWorkStateType workState = getWorkState(ctx.coordinatorTask);
		AbstractWorkBucketType bucket = WorkBucketUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
		if (bucket == null) {
			throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + ctx.coordinatorTask);
		}
		if (bucket.getState() != WorkBucketStateType.DELEGATED) {
			throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + ctx.coordinatorTask
					+ " cannot be marked as complete, as it is not delegated; its state = " + bucket.getState());
		}
		Collection<ItemDelta<?, ?>> modifications = bucketStateChangeDeltas(bucket, WorkBucketStateType.COMPLETE);
		try {
			repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
					modifications, bucketUnchangedPrecondition(bucket), null, result);
		} catch (PreconditionViolationException e) {
			throw new IllegalStateException("Unexpected concurrent modification of work bucket " + bucket + " in " + ctx.coordinatorTask, e);
		}
		ItemDelta.applyTo(modifications, ctx.coordinatorTask.getTaskPrismObject());
		compressCompletedBuckets(ctx.coordinatorTask, result);

		TaskWorkStateType workerWorkState = getWorkState(ctx.workerTask);
		AbstractWorkBucketType workerBucket = WorkBucketUtil.findBucketByNumber(workerWorkState.getBucket(), sequentialNumber);
		if (workerBucket == null) {
			//LOGGER.warn("No work bucket with sequential number of " + sequentialNumber + " in worker task " + ctx.workerTask);
			//return;
			// just during testing
			throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in worker task " + ctx.workerTask);
		}
		repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(), bucketDeleteDeltas(workerBucket), result);
	}

	private void completeWorkBucketStandalone(Context ctx, int sequentialNumber, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		TaskWorkStateType workState = getWorkState(ctx.workerTask);
		AbstractWorkBucketType bucket = WorkBucketUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
		if (bucket == null) {
			throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + ctx.workerTask);
		}
		if (bucket.getState() != WorkBucketStateType.READY && bucket.getState() != null) {
			throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + ctx.coordinatorTask
					+ " cannot be marked as complete, as it is not ready; its state = " + bucket.getState());
		}
		repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
				bucketStateChangeDeltas(bucket, WorkBucketStateType.COMPLETE),
				null, result);
		compressCompletedBuckets(ctx.workerTask, result);
	}

	/**
	 * Releases work bucket.
	 * Should be called from the worker task.
	 */
	public void releaseWorkBucket(String workerTaskOid, int sequentialNumber, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		Context ctx = createContext(workerTaskOid, result);
		if (ctx.isStandalone()) {
			throw new UnsupportedOperationException("Cannot release work bucket from standalone task " + ctx.workerTask);
		} else {
			releaseWorkBucketMultiNode(ctx, sequentialNumber, result);
		}
	}

	private void releaseWorkBucketMultiNode(Context ctx, int sequentialNumber, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		TaskWorkStateType workState = getWorkState(ctx.coordinatorTask);
		AbstractWorkBucketType bucket = WorkBucketUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
		if (bucket == null) {
			throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + ctx.coordinatorTask);
		}
		if (bucket.getState() != WorkBucketStateType.DELEGATED) {
			throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + ctx.coordinatorTask
					+ " cannot be released, as it is not delegated; its state = " + bucket.getState());
		}
		try {
			repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
					bucketStateChangeDeltas(bucket, WorkBucketStateType.READY),
					bucketUnchangedPrecondition(bucket), null, result);
		} catch (PreconditionViolationException e) {
			// just for sure
			throw new IllegalStateException("Unexpected concurrent modification of work bucket " + bucket + " in " + ctx.coordinatorTask, e);
		}

		TaskWorkStateType workerWorkState = getWorkState(ctx.workerTask);
		AbstractWorkBucketType workerBucket = WorkBucketUtil.findBucketByNumber(workerWorkState.getBucket(), sequentialNumber);
		if (workerBucket == null) {
			//LOGGER.warn("No work bucket with sequential number of " + sequentialNumber + " in worker task " + ctx.workerTask);
			//return;
			// just during testing
			throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in worker task " + ctx.workerTask);
		}
		repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(), bucketDeleteDeltas(workerBucket), result);
	}

	private void compressCompletedBuckets(Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		List<AbstractWorkBucketType> buckets = new ArrayList<>(getWorkState(task).getBucket());
		WorkBucketUtil.sortBucketsBySequentialNumber(buckets);
		int firstNotComplete;
		for (firstNotComplete = 0; firstNotComplete < buckets.size(); firstNotComplete++) {
			if (buckets.get(firstNotComplete).getState() != WorkBucketStateType.COMPLETE) {
				break;
			}
		}
		boolean allComplete = firstNotComplete == buckets.size();
		int eraseUpToExcluding = !allComplete ? firstNotComplete : buckets.size() - 1;
		if (eraseUpToExcluding <= 0) {
			return;        // nothing to be erased
		}
		List<ItemDelta<?, ?>> deleteItemDeltas = new ArrayList<>();
		for (int i = 0; i < eraseUpToExcluding; i++) {
			deleteItemDeltas.addAll(bucketDeleteDeltas(buckets.get(i)));
		}
		// these buckets should not be touched by anyone (as they are already complete); so we can execute without preconditions
		repositoryService.modifyObject(TaskType.class, task.getOid(), deleteItemDeltas, null, result);
	}

	private Collection<ItemDelta<?, ?>> bucketsReplaceDeltas(List<AbstractWorkBucketType> buckets) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
				.replaceRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
	}

	private Collection<ItemDelta<?, ?>> bucketsAddDeltas(List<AbstractWorkBucketType> buckets) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
				.addRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
	}

	private ModificationPrecondition<TaskType> bucketsReplacePrecondition(List<AbstractWorkBucketType> originalBuckets) {
		// performance is not optimal but OK for precondition checking
		return taskObject -> cloneNoId(originalBuckets).equals(cloneNoId(getWorkStateOrNew(taskObject).getBucket()));
	}

	private Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(AbstractWorkBucketType bucket, WorkBucketStateType newState) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET, bucket.getId(), AbstractWorkBucketType.F_STATE)
				.replace(newState).asItemDeltas();
	}

	private Collection<ItemDelta<?, ?>> bucketDeleteDeltas(AbstractWorkBucketType bucket) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
				.delete(bucket.clone()).asItemDeltas();
	}

	private ModificationPrecondition<TaskType> bucketUnchangedPrecondition(AbstractWorkBucketType originalBucket) {
		return taskObject -> {
			AbstractWorkBucketType currentBucket = findBucketByNumber(getWorkStateOrNew(taskObject).getBucket(),
					originalBucket.getSequentialNumber());
			// performance is not optimal but OK for precondition checking
			boolean rv = currentBucket != null && cloneNoId(currentBucket).equals(cloneNoId(originalBucket));
			if (!rv) {
				System.out.println("Hi");
			}
			return rv;
		};
	}

	private AbstractWorkBucketType cloneNoId(AbstractWorkBucketType bucket) {
		return bucket.clone().id(null);
	}

	private List<AbstractWorkBucketType> cloneNoId(List<AbstractWorkBucketType> buckets) {
		return buckets.stream().map(this::cloneNoId)
				.collect(Collectors.toCollection(() -> new ArrayList<>(buckets.size())));
	}

	@NotNull
	private TaskWorkStateType getWorkStateOrNew(PrismObject<TaskType> task) {
		if (task.asObjectable().getWorkState() != null) {
			return task.asObjectable().getWorkState();
		} else {
			return new TaskWorkStateType(prismContext);
		}
	}

	@NotNull
	private TaskWorkStateType getWorkState(Task task) throws SchemaException {
		if (task.getWorkState() != null) {
			return task.getWorkState();
		} else {
			throw new SchemaException("No work state in task " + task);
		}
	}
}
