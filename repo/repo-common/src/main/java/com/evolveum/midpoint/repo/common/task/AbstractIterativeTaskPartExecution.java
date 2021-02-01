/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.repo.common.task.AnnotationSupportUtil.createFromAnnotation;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * Represents an execution of a generic iterative part of a task.
 *
 * Responsibilities:
 *
 * 1. Prepares _item gathering_ operation, like the iterative search, live sync, or async update listening.
 *    For example, for the search, it determines the object type, the query, and the options.
 *    See {@link #prepareItemSource(OperationResult)}.
 *
 * 2. Initiates item gathering operation. See {@link #processItems(OperationResult)} method.
 *
 * 3. Funnels all items received into the {@link ProcessingCoordinator}.
 *
 * 4. Holds all relevant state (including e.g. statistics) of the execution.
 *
 * *TODO finish the cleanup*
 */
@SuppressWarnings("JavadocReference")
public abstract class AbstractIterativeTaskPartExecution<I,
        TH extends AbstractTaskHandler<TH, TE>,
        TE extends AbstractTaskExecution<TH, TE>,
        PE extends AbstractIterativeTaskPartExecution<I, TH, TE, PE, IP>,
        IP extends AbstractIterativeItemProcessor<I, TH, TE, PE, IP>> {

    /**
     * Task execution. Maintains objects (e.g. resource definition, scan timestamps, and so on) common to all task parts.
     */
    @NotNull protected final TE taskExecution;

    /**
     * The task handler. Used to access e.g. necessary Spring beans.
     */
    @NotNull protected final TH taskHandler;

    /**
     * The persistent task that carries out the work.
     * It can have lightweight asynchronous worker tasks (threads), hence the name.
     *
     * TODO better name
     */
    @NotNull protected final RunningTask localCoordinatorTask;

    /**
     * Logger that is specific to the concrete task handler class. This is to avoid logging everything under
     * common {@link AbstractIterativeTaskPartExecution} or some of its generic subclasses. Also, it allows
     * to group all processing related to the given task under a single logger.
     *
     * See {@link AbstractTaskHandler#logger}.
     */
    @NotNull protected final Trace logger;

    /**
     * Processes individual items - in cooperation with {@link ProcessingCoordinator} and {@link ItemProcessingGatekeeper}.
     */
    protected IP itemProcessor;

    /**
     * Schedules individual items for processing by worker tasks (if running in multiple threads).
     */
    protected ProcessingCoordinator<I> coordinator;

    /**
     * Result of the processing of the current bucket in the current task part.
     * TODO decide its fate
     */
    @NotNull protected final TaskWorkBucketProcessingResult runResult;

    /**
     * Maintains selected statistical information related to processing items in during task part execution.
     */
    @NotNull protected final ItemProcessingStatistics statistics;

    /**
     * Things like "Import", "Reconciliation (on resource)", and so on. Used e.g. in log messages like:
     * "Import of UserType:jack (Jack Sparrow, c0c010c0-d34d-b33f-f00d-111111111111) from Crew Management has been started"
     */
    @NotNull private String processShortNameCapitalized;

    /**
     * Information that augments the process short name. Used e.g. in log messages.
     * An example: "from [resource]".
     */
    @NotNull private String contextDescription;

    /**
     * Determines and executes error handling strategy for this task part.
     */
    @NotNull private final ErrorHandlingStrategyExecutor errorHandlingStrategyExecutor;

    protected AbstractIterativeTaskPartExecution(@NotNull TE taskExecution) {
        this.taskHandler = taskExecution.taskHandler;
        this.taskExecution = taskExecution;
        this.localCoordinatorTask = taskExecution.localCoordinatorTask;
        this.logger = taskHandler.getLogger();
        this.runResult = taskExecution.getCurrentRunResult();
        this.statistics = new ItemProcessingStatistics(localCoordinatorTask.getProgress());
        this.processShortNameCapitalized = taskHandler.getTaskTypeName();
        this.contextDescription = "";
        this.errorHandlingStrategyExecutor = new ErrorHandlingStrategyExecutor(
                taskExecution.localCoordinatorTask, taskHandler.prismContext, taskHandler.repositoryService,
                getDefaultErrorAction());
    }

    public @NotNull TaskWorkBucketProcessingResult run(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException,
            TaskException, ObjectAlreadyExistsException, PolicyViolationException, PreconditionViolationException {

        logger.trace("{} run starting: local coordinator task {}, previous run result {}",
                processShortNameCapitalized, localCoordinatorTask, taskExecution.previousRunResult);

        checkTaskPersistence();

        initialize(opResult);

        prepareItemSource(opResult);
        setProgressAndExpectedItems(opResult);

        itemProcessor = setupItemProcessor(opResult);
        coordinator = setupCoordinator();

        try {
            coordinator.createWorkerTasks();
            processItems(opResult);
        } finally {
            // This is redundant in the case of live sync event handling (because the handler gets a notification when all
            // items are submitted, and must stop the threads in order to allow provisioning to update the token).
            //
            // But overall, it is necessary to do this here in order to avoid endless waiting if any exception occurs.
            coordinator.finishProcessing(opResult);
        }

        setOperationResult(opResult);

        runResult.setProgress(getTotalProgress());

        if (taskHandler.getReportingOptions().isLogFinishInfo()) {
            logFinishInfo(opResult);
        }

        finish(opResult);

        logger.trace("{} run finished (task {}, run result {})", processShortNameCapitalized, localCoordinatorTask, runResult);
        runResult.setBucketComplete(localCoordinatorTask.canRun()); // TODO
        runResult.setShouldContinue(localCoordinatorTask.canRun()); // TODO
        return runResult;
    }

    // TODO finish this method
    private void setOperationResult(OperationResult opResult) {
        if (taskExecution.getErrorState().isPermanentErrorEncountered()) {
            // We assume the error occurred within this part (otherwise that part would not even start).
            opResult.setStatus(FATAL_ERROR);
        } else if (statistics.getErrors() > 0) {
            opResult.setStatus(PARTIAL_ERROR);
        } else {
            opResult.setStatus(SUCCESS);
        }
    }

    /**
     * Initializes task part execution.
     */
    protected void initialize(OperationResult opResult) throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException, TaskException {
    }

    /**
     * Prepares the item source. E.g. for search-iterative tasks we prepare object type, query, and options here.
     */
    protected void prepareItemSource(OperationResult opResult) throws TaskException, CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException {
    }

    /**
     * Computes expected total and sets the value in the task. E.g. for search-iterative tasks we count the objects here.
     * TODO reconsider
     */
    protected void setProgressAndExpectedItems(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException {
    }

    /**
     * Sets up the item processor. This is just delegated to the subclass (or default method is used).
     */
    private IP setupItemProcessor(OperationResult opResult) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return createItemProcessor(opResult);
    }

    /**
     * Creates the item processor. This method should not do anything more. For initialization there are other methods.
     */
    protected @NotNull IP createItemProcessor(OperationResult opResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return createItemProcessorFromAnnotation();
    }

    private @NotNull IP createItemProcessorFromAnnotation() {
        //noinspection unchecked
        return (IP) createFromAnnotation(this, this, this,
                ItemProcessorClass.class, ItemProcessorClass::value, "item processor");
    }

    /**
     * Creates the processing coordinator. Usually no customization is needed here.
     */
    private ProcessingCoordinator<I> setupCoordinator() {
        return new ProcessingCoordinator<>(taskHandler, localCoordinatorTask);
    }

    /**
     * Starts the item source (e.g. searchObjectsIterative call or synchronize call) and begins processing items
     * generated by it. Returns when the source finishes. For example:
     * - for search-based tasks, this call returns immediately after the search is over ... TODO ok?
     * - for live sync task, this returns after all changes were fetched and acknowledged, and the resulting token was written
     * - for async update task, this returns also after all changes were fetched and acknowledged and confirmed to the source
     */
    protected abstract void processItems(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException,
            PolicyViolationException, PreconditionViolationException;

    /**
     * Ends the processing.
     */
    protected void finish(OperationResult opResult) throws SchemaException {
    }

    private void checkTaskPersistence() {
        if (localCoordinatorTask.getOid() == null) {
            throw new IllegalArgumentException(
                    "Transient tasks cannot be run by " + getClass() + ": " + localCoordinatorTask);
        }
    }

    public @NotNull Trace getLogger() {
        return taskHandler.getLogger();
    }

    /**
     * @return True if this task execution should periodically write traces, according to relevant task extension items.
     * It is also used to drive dynamic profiling (which is going to be deprecated).
     */
    public abstract boolean providesTracingAndDynamicProfiling();

    public @NotNull TE getTaskExecution() {
        return taskExecution;
    }

    public long getStartTimeMillis() {
        return statistics.startTimeMillis;
    }

    public boolean isMultithreaded() {
        return coordinator.isMultithreaded();
    }

    public Long heartbeat() {
        // If we exist then we run. So just return the progress count.
        return getTotalProgress();
    }

    private void logFinishInfo(OperationResult opResult) {
        String finishMessage = "Finished " + getProcessShortName() + " (" + localCoordinatorTask + "). ";
        String statMsg =
                "Processed " + statistics.getItemsProcessed() + " objects in " + statistics.getWallTime() / 1000
                        + " seconds, got " + statistics.getErrors() + " errors.";
        if (statistics.getItemsProcessed() > 0) {
            statMsg += " Average time for one object: " + statistics.getAverageTime() + " milliseconds" +
                    " (wall clock time average: " + statistics.getWallAverageTime() + " ms).";
        }
        if (!localCoordinatorTask.canRun()) {
            statMsg += " Task was interrupted during processing.";
        }
        statMsg += " Resulting status: " + opResult.getStatus();

        opResult.createSubresult(getTaskOperationPrefix() + ".statistics")
                .recordStatus(OperationResultStatus.SUCCESS, statMsg);
        TaskHandlerUtil.appendLastFailuresInformation(getTaskOperationPrefix(), localCoordinatorTask, opResult);

        logger.info("{}", finishMessage + statMsg);
    }

    private String getTaskOperationPrefix() {
        return taskHandler.getTaskOperationPrefix();
    }

    private Integer getWorkerThreadsCount() {
        return taskExecution.getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
    }

    protected void ensureNoWorkerThreads() {
        Integer tasks = getWorkerThreadsCount();
        if (tasks != null && tasks != 0) {
            throw new UnsupportedOperationException("Unsupported number of worker threads: " + tasks +
                    ". This task cannot be run with worker threads. Please remove workerThreads "
                    + "extension property or set its value to 0.");
        }
    }

    public @NotNull String getProcessShortNameCapitalized() {
        return processShortNameCapitalized;
    }

    public @NotNull String getProcessShortName() {
        return StringUtils.uncapitalize(processShortNameCapitalized);
    }

    public void setProcessShortNameCapitalized(String value) {
        this.processShortNameCapitalized = ObjectUtils.defaultIfNull(value, "");
    }

    public @NotNull String getContextDescription() {
        return contextDescription;
    }

    public void setContextDescription(String value) {
        this.contextDescription = ObjectUtils.defaultIfNull(value, "");
    }

    public long getTotalProgress() {
        return statistics.getTotalProgress();
    }

    ErrorHandlingStrategyExecutor.Action determineErrorAction(@NotNull OperationResultStatus status,
            @NotNull Throwable exception, ItemProcessingRequest<?> request, OperationResult result) {
        return errorHandlingStrategyExecutor.determineAction(status, exception, request.getObjectOidToRecordRetryTrigger(), result);
    }

    /**
     * @return Default error action if no policy is specified or if no policy entry matches.
     */
    protected @NotNull abstract ErrorHandlingStrategyExecutor.Action getDefaultErrorAction();
}
