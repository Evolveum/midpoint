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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.content.WorkBucketContentHandler;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.content.WorkBucketContentHandlerRegistry;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult.FoundExisting;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult.NewBuckets;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult.NothingFound;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategyFactory;
import com.evolveum.midpoint.util.backoff.BackoffComputer;
import com.evolveum.midpoint.util.backoff.ExponentialBackoffComputer;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil.findBucketByNumber;
import static java.util.Collections.singletonList;

/**
 * Responsible for managing task work state.
 *
 * @author mederly
 */

@Component
public class WorkStateManager {

	private static final Trace LOGGER = TraceManager.getTrace(WorkStateManager.class);

	@Autowired private TaskManager taskManager;
	@Autowired private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;
	@Autowired private WorkSegmentationStrategyFactory strategyFactory;
	@Autowired private WorkBucketContentHandlerRegistry handlerFactory;

	private static final long DYNAMIC_SLEEP_INTERVAL = 100L;

	private Long freeBucketWaitIntervalOverride = null;

	private class Context {
		Task workerTask;
		Task coordinatorTask;           // null for standalone worker tasks
		final Supplier<Boolean> canRunSupplier;

		public Context(Supplier<Boolean> canRunSupplier) {
			this.canRunSupplier = canRunSupplier;
		}

		public boolean isStandalone() {
			if (workerTask.getWorkManagement() == null) {
				return true;
			}
			TaskKindType kind = workerTask.getWorkManagement().getTaskKind();
			return kind == null || kind == TaskKindType.STANDALONE;
		}

		public void reloadCoordinatorTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
			coordinatorTask = taskManager.getTask(coordinatorTask.getOid(), null, result);
		}

		public void reloadWorkerTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
			workerTask = taskManager.getTask(workerTask.getOid(), null, result);
		}

		public TaskWorkManagementType getWorkStateConfiguration() {
			return isStandalone() ? workerTask.getWorkManagement() : coordinatorTask.getWorkManagement();
		}
	}

	public boolean canRun(Supplier<Boolean> canRunSupplier) {
		return canRunSupplier == null || BooleanUtils.isTrue(canRunSupplier.get());
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
	public WorkBucketType getWorkBucket(@NotNull String workerTaskOid, long freeBucketWaitTime,
			Supplier<Boolean> canRun, @NotNull OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
		Context ctx = createContext(workerTaskOid, canRun, result);
		WorkBucketType bucket = findSelfAllocatedBucket(ctx);
		if (bucket != null) {
			LOGGER.trace("Returning self-allocated bucket for {}: {}", workerTaskOid, bucket);
			return bucket;
		}
		if (ctx.isStandalone()) {
			return getWorkBucketStandalone(ctx, result);
		} else {
			return getWorkBucketMultiNode(ctx, freeBucketWaitTime, result);
		}
	}

	private WorkBucketType findSelfAllocatedBucket(Context ctx) {
		TaskWorkStateType workState = ctx.workerTask.getTaskType().getWorkState();
		if (workState == null || workState.getBucket().isEmpty()) {
			return null;
		}
		List<WorkBucketType> buckets = new ArrayList<>(workState.getBucket());
		TaskWorkStateTypeUtil.sortBucketsBySequentialNumber(buckets);
		for (WorkBucketType bucket : buckets) {
			if (bucket.getState() == WorkBucketStateType.READY) {
				return bucket;
			}
		}
		return null;
	}

	private WorkBucketType getWorkBucketMultiNode(Context ctx, long freeBucketWaitTime, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, InterruptedException {
		long start = System.currentTimeMillis();
		WorkSegmentationStrategy workStateStrategy = strategyFactory.createStrategy(ctx.coordinatorTask.getWorkManagement());
		setOrUpdateEstimatedNumberOfBuckets(ctx.coordinatorTask, workStateStrategy, result);

waitForAvailableBucket:    // this cycle exits when something is found OR when a definite 'no more buckets' answer is received
	    for (;;) {
		    BackoffComputer backoffComputer = createBackoffComputer();
		    int retry = 0;
waitForConflictLessUpdate: // this cycle exits when coordinator task update succeeds
			for (;;) {
				TaskWorkStateType coordinatorWorkState = getWorkStateOrNew(ctx.coordinatorTask.getTaskPrismObject());
				GetBucketResult response = workStateStrategy.getBucket(coordinatorWorkState);
				LOGGER.trace("getWorkBucketMultiNode: workStateStrategy returned {} for worker task {}, coordinator {}", response, ctx.workerTask, ctx.coordinatorTask);
				try {
					if (response instanceof NewBuckets) {
						NewBuckets newBucketsResponse = (NewBuckets) response;
						List<WorkBucketType> newCoordinatorBuckets = new ArrayList<>(coordinatorWorkState.getBucket());
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
						WorkBucketType foundBucket = existingResponse.bucket.clone();
						repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
								bucketsAddDeltas(singletonList(foundBucket)), null, result);
						return foundBucket;
					} else if (response instanceof NothingFound) {
						if (((NothingFound) response).definite || freeBucketWaitTime == 0L) {
							markWorkComplete(ctx.coordinatorTask, result);       // TODO also if response is not definite?
							return null;
						} else {
							long waitDeadline = freeBucketWaitTime >= 0 ? start + freeBucketWaitTime : Long.MAX_VALUE;
							long toWait = waitDeadline - System.currentTimeMillis();
							if (toWait <= 0) {
								markWorkComplete(ctx.coordinatorTask, result);       // TODO also if response is not definite?
								return null;
							}
							//System.out.println("*** No free work bucket -- waiting ***");
							dynamicSleep(Math.min(toWait, getFreeBucketWaitInterval()), ctx);
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
					retry++;
					long delay;
					try {
						delay = backoffComputer.computeDelay(retry);
					} catch (BackoffComputer.NoMoreRetriesException e1) {
						throw new SystemException(
								"Couldn't allocate work bucket because of repeated database conflicts (retry limit reached); coordinator task = " + ctx.coordinatorTask, e1);
					}
					LOGGER.info("getWorkBucketMultiNode: conflict; continuing as retry #{}; waiting {} ms in {}, worker {}",
							retry, delay, ctx.coordinatorTask, ctx.workerTask, e);
					dynamicSleep(delay, ctx);
					ctx.reloadCoordinatorTask(result);
					ctx.reloadWorkerTask(result);
					//noinspection UnnecessaryContinue,UnnecessaryLabelOnContinueStatement
					continue waitForConflictLessUpdate;
				}
			}
		}
	}

	private BackoffComputer createBackoffComputer() {
		TaskManagerConfiguration c = getConfiguration();
		return new ExponentialBackoffComputer(c.getWorkAllocationMaxRetries(), c.getWorkAllocationInitialDelay(),
				c.getWorkAllocationRetryExponentialThreshold());
	}

	private long getFreeBucketWaitInterval() {
		return freeBucketWaitIntervalOverride != null ? freeBucketWaitIntervalOverride :
				getConfiguration().getWorkAllocationDefaultFreeBucketWaitInterval();
	}

	private long getInitialDelay() {
		return getConfiguration().getWorkAllocationInitialDelay();
	}

	private TaskManagerConfiguration getConfiguration() {
		return ((TaskManagerQuartzImpl) taskManager).getConfiguration();
	}

	private void setOrUpdateEstimatedNumberOfBuckets(Task task, WorkSegmentationStrategy workStateStrategy, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		Integer number = workStateStrategy.estimateNumberOfBuckets(task.getWorkState());
		if (number != null && (task.getWorkState() == null || !number.equals(task.getWorkState().getNumberOfBuckets()))) {
			List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(TaskType.class, prismContext)
					.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_NUMBER_OF_BUCKETS).replace(number)
					.asItemDeltas();
			repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
		}
	}

	private void dynamicSleep(long delay, Context ctx) throws InterruptedException {
		dynamicSleep(delay, ctx.canRunSupplier);
	}

	private void dynamicSleep(long delay, Supplier<Boolean> canRunSupplier) throws InterruptedException {
		while (delay > 0) {
			if (!canRun(canRunSupplier)) {
				throw new InterruptedException();
			}
			Thread.sleep(Math.min(delay, DYNAMIC_SLEEP_INTERVAL));
			delay -= DYNAMIC_SLEEP_INTERVAL;
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
		int reclaiming = 0;
		for (WorkBucketType bucket : newState.getBucket()) {
			if (bucket.getState() == WorkBucketStateType.DELEGATED) {
				Task worker = TaskWorkStateUtil.findWorkerByBucketNumber(workers, bucket.getSequentialNumber());
				if (worker == null || worker.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
					LOGGER.info("Reclaiming wrongly allocated work bucket {} from worker task {}", bucket, worker);
					bucket.setState(WorkBucketStateType.READY);
					// TODO modify also the worker if it exists (maybe)
					reclaiming++;
				}
			}
		}
		LOGGER.trace("Reclaiming wrongly allocated buckets found {} buckets to reclaim in {}", reclaiming, coordinatorTask);
		if (reclaiming > 0) {
			repositoryService.modifyObject(TaskType.class, coordinatorTask.getOid(),
					bucketsReplaceDeltas(newState.getBucket()),
					bucketsReplacePrecondition(originalState.getBucket()), null, result);
		}
		return reclaiming > 0;
	}

	private WorkBucketType getWorkBucketStandalone(Context ctx, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		WorkSegmentationStrategy workStateStrategy = strategyFactory.createStrategy(ctx.workerTask.getWorkManagement());
		setOrUpdateEstimatedNumberOfBuckets(ctx.workerTask, workStateStrategy, result);
		TaskWorkStateType workState = getWorkStateOrNew(ctx.workerTask.getTaskPrismObject());
		GetBucketResult response = workStateStrategy.getBucket(workState);
		LOGGER.trace("getWorkBucketStandalone: workStateStrategy returned {} for standalone task {}", response, ctx.workerTask);
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
			markWorkComplete(ctx.workerTask, result);
			return null;
		} else {
			throw new AssertionError(response);
		}
	}

	private void markWorkComplete(Task task, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_ALL_WORK_COMPLETE).replace(true)
				.asItemDeltas();
		repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
	}

	private Context createContext(String workerTaskOid, Supplier<Boolean> canRun,
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		Context ctx = new Context(canRun);
		ctx.workerTask = taskManager.getTask(workerTaskOid, result);
		TaskWorkManagementType wsConfig = ctx.workerTask.getWorkManagement();
		if (wsConfig != null && wsConfig.getTaskKind() != null && wsConfig.getTaskKind() != TaskKindType.WORKER &&
				wsConfig.getTaskKind() != TaskKindType.STANDALONE) {
			throw new IllegalStateException("Wrong task kind for worker task " + ctx.workerTask + ": " + wsConfig.getTaskKind());
		}
		if (wsConfig != null && wsConfig.getTaskKind() == TaskKindType.WORKER) {
			ctx.coordinatorTask = getCoordinatorTask(ctx.workerTask, result);
		}
		return ctx;
	}

	private Task getCoordinatorTask(Task workerTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Task parent = workerTask.getParentTask(result);
		if (parent == null) {
			throw new IllegalStateException("No coordinator task for worker task " + workerTask);
		}
		TaskWorkManagementType wsConfig = parent.getWorkManagement();
		if (wsConfig == null || wsConfig.getTaskKind() != TaskKindType.COORDINATOR) {
			throw new IllegalStateException("Coordinator task for worker task " + workerTask + " is not marked as such: " + parent);
		}
		return parent;
	}

	public void completeWorkBucket(String workerTaskOid, int sequentialNumber, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		Context ctx = createContext(workerTaskOid, null, result);
		LOGGER.trace("Completing work bucket {} in {} (coordinator {})", workerTaskOid, ctx.workerTask, ctx.coordinatorTask);
		if (ctx.isStandalone()) {
			completeWorkBucketStandalone(ctx, sequentialNumber, result);
		} else {
			completeWorkBucketMultiNode(ctx, sequentialNumber, result);
		}
	}

	private void completeWorkBucketMultiNode(Context ctx, int sequentialNumber, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		TaskWorkStateType workState = getWorkState(ctx.coordinatorTask);
		WorkBucketType bucket = TaskWorkStateTypeUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
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
		WorkBucketType workerBucket = TaskWorkStateTypeUtil.findBucketByNumber(workerWorkState.getBucket(), sequentialNumber);
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
		WorkBucketType bucket = TaskWorkStateTypeUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
		if (bucket == null) {
			throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + ctx.workerTask);
		}
		if (bucket.getState() != WorkBucketStateType.READY && bucket.getState() != null) {
			throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + ctx.coordinatorTask
					+ " cannot be marked as complete, as it is not ready; its state = " + bucket.getState());
		}
		Collection<ItemDelta<?, ?>> modifications = bucketStateChangeDeltas(bucket, WorkBucketStateType.COMPLETE);
		repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(), modifications, null, result);
		ItemDelta.applyTo(modifications, ctx.workerTask.getTaskPrismObject());
		compressCompletedBuckets(ctx.workerTask, result);
	}

	/**
	 * Releases work bucket.
	 * Should be called from the worker task.
	 */
	public void releaseWorkBucket(String workerTaskOid, int sequentialNumber, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		Context ctx = createContext(workerTaskOid, null, result);
		LOGGER.trace("Releasing bucket {} in {} (coordinator {})", sequentialNumber, ctx.workerTask, ctx.coordinatorTask);
		if (ctx.isStandalone()) {
			throw new UnsupportedOperationException("Cannot release work bucket from standalone task " + ctx.workerTask);
		} else {
			releaseWorkBucketMultiNode(ctx, sequentialNumber, result);
		}
	}

	private void releaseWorkBucketMultiNode(Context ctx, int sequentialNumber, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		TaskWorkStateType workState = getWorkState(ctx.coordinatorTask);
		WorkBucketType bucket = TaskWorkStateTypeUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
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
		WorkBucketType workerBucket = TaskWorkStateTypeUtil.findBucketByNumber(workerWorkState.getBucket(), sequentialNumber);
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
		List<WorkBucketType> buckets = new ArrayList<>(getWorkState(task).getBucket());
		TaskWorkStateTypeUtil.sortBucketsBySequentialNumber(buckets);
		List<WorkBucketType> completeBuckets = buckets.stream()
				.filter(b -> b.getState() == WorkBucketStateType.COMPLETE)
				.collect(Collectors.toList());
		if (completeBuckets.size() <= 1) {
			LOGGER.trace("Compression of completed buckets: # of complete buckets is too small ({}) in {}, exiting",
					completeBuckets.size(), task);
			return;
		}

		List<ItemDelta<?, ?>> deleteItemDeltas = new ArrayList<>();
		for (int i = 0; i < completeBuckets.size() - 1; i++) {
			deleteItemDeltas.addAll(bucketDeleteDeltas(completeBuckets.get(i)));
		}
		LOGGER.trace("Compression of completed buckets: deleting {} buckets before last completed one in {}", deleteItemDeltas.size(), task);
		// these buckets should not be touched by anyone (as they are already completed); so we can execute without preconditions
		if (!deleteItemDeltas.isEmpty()) {
			repositoryService.modifyObject(TaskType.class, task.getOid(), deleteItemDeltas, null, result);
		}
	}

	private Collection<ItemDelta<?, ?>> bucketsReplaceDeltas(List<WorkBucketType> buckets) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
				.replaceRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
	}

	private Collection<ItemDelta<?, ?>> bucketsAddDeltas(List<WorkBucketType> buckets) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
				.addRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
	}

	private ModificationPrecondition<TaskType> bucketsReplacePrecondition(List<WorkBucketType> originalBuckets) {
		// performance is not optimal but OK for precondition checking
		return taskObject -> cloneNoId(originalBuckets).equals(cloneNoId(getWorkStateOrNew(taskObject).getBucket()));
	}

	private Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(WorkBucketType bucket, WorkBucketStateType newState) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET, bucket.getId(), WorkBucketType.F_STATE)
				.replace(newState).asItemDeltas();
	}

	private Collection<ItemDelta<?, ?>> bucketDeleteDeltas(WorkBucketType bucket) throws SchemaException {
		return DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
				.delete(bucket.clone()).asItemDeltas();
	}

	private ModificationPrecondition<TaskType> bucketUnchangedPrecondition(WorkBucketType originalBucket) {
		return taskObject -> {
			WorkBucketType currentBucket = findBucketByNumber(getWorkStateOrNew(taskObject).getBucket(),
					originalBucket.getSequentialNumber());
			// performance is not optimal but OK for precondition checking
			boolean rv = currentBucket != null && cloneNoId(currentBucket).equals(cloneNoId(originalBucket));
			if (!rv) {
				System.out.println("Hi");
			}
			return rv;
		};
	}

	private WorkBucketType cloneNoId(WorkBucketType bucket) {
		return bucket.clone().id(null);
	}

	private List<WorkBucketType> cloneNoId(List<WorkBucketType> buckets) {
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

	public void setFreeBucketWaitIntervalOverride(Long value) {
		this.freeBucketWaitIntervalOverride = value;
	}

	// TODO
	public ObjectQuery narrowQueryForWorkBucket(Task workerTask, ObjectQuery query, Class<? extends ObjectType> type,
			Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider,
			WorkBucketType workBucket, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Context ctx = createContext(workerTask.getOid(), () -> true, result);

		TaskWorkManagementType config = ctx.getWorkStateConfiguration();
		AbstractWorkSegmentationType bucketsConfig = TaskWorkStateTypeUtil.getWorkSegmentationConfiguration(config);
		WorkBucketContentHandler handler = handlerFactory.getHandler(workBucket.getContent());
		List<ObjectFilter> conjunctionMembers = new ArrayList<>(
				handler.createSpecificFilters(workBucket, bucketsConfig, type, itemDefinitionProvider));
		if (conjunctionMembers.isEmpty()) {
			return query;
		}
		ObjectFilter existingFilter = query.getFilter();
		if (existingFilter != null) {
			conjunctionMembers.add(existingFilter);
		}
		ObjectFilter updatedFilter;
		if (conjunctionMembers.isEmpty()) {
			updatedFilter = null;
		} else if (conjunctionMembers.size() == 1) {
			updatedFilter = conjunctionMembers.get(0);
		} else {
			updatedFilter = AndFilter.createAnd(conjunctionMembers);
		}

		ObjectQuery updatedQuery = query.clone();
		updatedQuery.setFilter(updatedFilter);

		// TODO update sorting criteria
		return updatedQuery;
	}

	public void executeInitialDelay(TaskQuartzImpl task) throws InterruptedException {
		if (task.getWorkManagement() != null && task.getWorkManagement().getTaskKind() == TaskKindType.WORKER) {
			long delay = (long) (Math.random() * getInitialDelay());
			if (delay != 0) {
				// temporary info level logging
				LOGGER.info("executeInitialDelay: waiting {} ms in {}", delay, task);
				dynamicSleep(delay, task::canRun);
			}
		}
	}
}
