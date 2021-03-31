/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.repo.common.task.AnnotationSupportUtil.createFromAnnotation;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.schema.util.task.TaskPartPerformanceInformation;
import com.evolveum.midpoint.schema.util.task.TaskProgressUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

import java.util.Locale;
import java.util.Objects;

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
     * (I.e. current bucket.)
     */
    @NotNull protected final CurrentBucketStatistics bucketStatistics;

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

    /**
     * Reporting options specific for this task part. They are copied from the main options
     * present in the task handler - by default. Note this will change when we allow configuring
     * them for individual tasks.
     */
    @NotNull protected final TaskReportingOptions reportingOptions;

    /**
     * URI of this task part (unique within the task).
     */
    @Experimental
    String partUri;

    /**
     * Relative number of the part within this physical task. Starting at 1.
     */
    @Experimental
    private int partNumber;

    /**
     * Expected number of parts.
     */
    @Experimental
    private int expectedParts;

    protected AbstractIterativeTaskPartExecution(@NotNull TE taskExecution) {
        this.taskHandler = taskExecution.taskHandler;
        this.taskExecution = taskExecution;
        this.localCoordinatorTask = taskExecution.localCoordinatorTask;
        this.logger = taskHandler.getLogger();
        this.runResult = taskExecution.getCurrentRunResult();
        this.bucketStatistics = new CurrentBucketStatistics();
        this.processShortNameCapitalized = taskHandler.getTaskTypeName();
        this.contextDescription = "";
        this.errorHandlingStrategyExecutor = new ErrorHandlingStrategyExecutor(
                taskExecution.localCoordinatorTask, taskHandler.prismContext, taskHandler.repositoryService,
                getDefaultErrorAction());
        this.reportingOptions = taskHandler.getGlobalReportingOptions()
                .cloneWithConfiguration(taskExecution.getTaskContainerRealValue(SchemaConstants.MODEL_EXTENSION_REPORTING_OPTIONS));
    }

    public @NotNull TaskWorkBucketProcessingResult run(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException,
            TaskException, ObjectAlreadyExistsException, PolicyViolationException, PreconditionViolationException {

        logger.trace("{} run starting: local coordinator task {}, previous run result {}",
                processShortNameCapitalized, localCoordinatorTask, taskExecution.previousRunResult);

        checkTaskPersistence();

        updateStatisticsOnStart();

        initialize(opResult);

        prepareItemSource(opResult);
        setExpectedTotal(opResult);

        itemProcessor = setupItemProcessor(opResult);
        coordinator = setupCoordinator();

        try {
            coordinator.createWorkerTasks(getReportingOptions());
            processItems(opResult);
        } finally {
            // This is redundant in the case of live sync event handling (because the handler gets a notification when all
            // items are submitted, and must stop the threads in order to allow provisioning to update the token).
            //
            // But overall, it is necessary to do this here in order to avoid endless waiting if any exception occurs.
            coordinator.finishProcessing(opResult);
        }

        setOperationResultStatus(opResult);

        updateStatisticsOnFinish(opResult);
        logFinishInfo(opResult);

        finish(opResult);

        logger.trace("{} run finished (task {}, run result {})", processShortNameCapitalized, localCoordinatorTask, runResult);
        runResult.setBucketComplete(localCoordinatorTask.canRun()); // TODO
        return runResult;
    }

    // TODO finish this method
    private void setOperationResultStatus(OperationResult opResult) {
        if (taskExecution.getErrorState().isPermanentErrorEncountered()) {
            // We assume the error occurred within this part (otherwise that part would not even start).
            opResult.setStatus(FATAL_ERROR);
        } else if (bucketStatistics.getErrors() > 0) {
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
    protected void setExpectedTotal(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
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
        return bucketStatistics.startTimeMillis;
    }

    public boolean isMultithreaded() {
        return coordinator.isMultithreaded();
    }

    public Long heartbeat() {
        // If we exist then we run. So just return the progress count.
        return localCoordinatorTask.getProgress();
    }

    private void updateStatisticsOnStart() {
        setStructuredProgressPartInformation();
        bucketStatistics.recordStart();
    }

    private void setStructuredProgressPartInformation() {
        taskExecution.localCoordinatorTask.setStructuredProgressPartInformation(partUri, partNumber, expectedParts);

        // The task progress can be out of sync with the actual progress e.g. because of open items (that have been zeroed above).
        StructuredTaskProgressType structuredProgress = localCoordinatorTask.getStructuredProgressOrClone();
        localCoordinatorTask.setProgress((long) TaskProgressUtil.getTotalProgressForCurrentPart(structuredProgress));
    }

    private void updateStatisticsOnFinish(OperationResult result) {
        bucketStatistics.recordEnd();
        localCoordinatorTask.recordPartExecutionEnd(partUri, getPartStartTimestamp(), bucketStatistics.getEndTimeMillis());
        localCoordinatorTask.updateStatisticsInTaskPrism(true);

        // TODO eventually remove
        TaskHandlerUtil.appendLastFailuresInformation(getTaskOperationPrefix(), localCoordinatorTask, result);
    }

    // TODO deduplicate with statistics output in ItemProcessingGatekeeper
    // TODO decide on the final form of these messages
    private void logFinishInfo(OperationResult result) {

        long endTime = Objects.requireNonNull(bucketStatistics.endTimeMillis, "No end timestamp?");
        // Note: part statistics should be consistent with this end time.

        OperationStatsType operationStats = localCoordinatorTask.getStoredOperationStatsOrClone();
        StructuredTaskProgressType structuredProgress = localCoordinatorTask.getStructuredProgressOrClone();
        TaskPartPerformanceInformation partStatistics =
                TaskPartPerformanceInformation.forCurrentPart(operationStats, structuredProgress);

        String currentPartUri = TaskProgressUtil.getCurrentPartUri(structuredProgress);

        String shortMessage =
                String.format("Finished bucket for %s (%s). Resulting status: %s.%s",
                        getProcessShortName(), localCoordinatorTask, result.getStatus(),
                        localCoordinatorTask.canRun() ? "" : " Task was interrupted during processing.");

        String bucketStatMsg = String.format(Locale.US, "Current bucket: processed %,d objects in %.1f seconds, got %,d errors.",
                bucketStatistics.getItemsProcessed(), bucketStatistics.getWallClockTime(endTime) / 1000.0,
                bucketStatistics.getErrors());
        if (bucketStatistics.getItemsProcessed() > 0) {
            bucketStatMsg += String.format(Locale.US, " Average processing time for one object: %,.1f milliseconds. "
                            + "Wall clock average: %,.1f milliseconds, throughput: %,.1f items per minute.",
                    bucketStatistics.getAverageTime(), bucketStatistics.getAverageWallClockTime(endTime),
                    bucketStatistics.getThroughput(endTime));
        }

        String partStatMsg = String.format(Locale.US, "The whole part: processed %,d objects in %.1f seconds, got %,d errors. Real progress: %,d.",
                partStatistics.getItemsProcessed(), TaskOperationStatsUtil.toSeconds(partStatistics.getWallClockTime()),
                partStatistics.getErrors(), partStatistics.getProgress());
        if (partStatistics.getItemsProcessed() > 0) {
            partStatMsg += String.format(Locale.US, " Average processing time for one object: %,.1f milliseconds. "
                            + "Wall clock average: %,.1f milliseconds, throughput: %,.1f items per minute.",
                    partStatistics.getAverageTime(), partStatistics.getAverageWallClockTime(),
                    partStatistics.getThroughput());
        }

        String fullStatMessage = String.format(Locale.US,
                "%s of a bucket done. Current part URI: %s\n\n"
                        + "Items processed: %,d in current bucket and %,d in current part.\n"
                        + "Errors: %,d in current bucket and %,d in current part.\n"
                        + "Real progress is %,d.\n\n"
                        + "Average duration is %,.1f ms (in current bucket) and %,.1f ms (in current part).\n"
                        + "Wall clock average is %,.1f ms (in current bucket) and %,.1f ms (in current part).\n"
                        + "Average throughput is %,.1f items per minute (in current bucket) and %,.1f items per minute (in current part).\n\n"
                        + "Processing time is %,.1f ms (for current bucket) and %,.1f ms (for current part)\n"
                        + "Wall-clock time is %,d ms (for current bucket) and %,d ms (for current part)\n"
                        + "Start time was:\n"
                        + " - for current bucket: %s\n"
                        + " - for current part:   %s\n",
                getProcessShortNameCapitalized(), currentPartUri,
                bucketStatistics.getItemsProcessed(), partStatistics.getItemsProcessed(),
                bucketStatistics.getErrors(), partStatistics.getErrors(),
                partStatistics.getProgress(),
                bucketStatistics.getAverageTime(), partStatistics.getAverageTime(),
                bucketStatistics.getAverageWallClockTime(endTime), partStatistics.getAverageWallClockTime(),
                bucketStatistics.getThroughput(endTime), partStatistics.getThroughput(),
                bucketStatistics.getProcessingTime(), partStatistics.getProcessingTime(),
                bucketStatistics.getWallClockTime(endTime), partStatistics.getWallClockTime(),
                XmlTypeConverter.createXMLGregorianCalendar(bucketStatistics.getStartTimeMillis()),
                partStatistics.getEarliestStartTime());

        result.createSubresult(getTaskOperationPrefix() + ".statistics")
                .recordStatus(OperationResultStatus.SUCCESS, bucketStatMsg);
        result.createSubresult(getTaskOperationPrefix() + ".statistics")
                .recordStatus(OperationResultStatus.SUCCESS, partStatMsg);

        TaskLoggingOptionType logging = reportingOptions.getBucketCompletionLogging();
        if (logging == TaskLoggingOptionType.FULL) {
            logger.info("{}", fullStatMessage);
        } else if (logging == TaskLoggingOptionType.BRIEF) {
            logger.info("{}\n{}\n{}", shortMessage, bucketStatMsg, partStatMsg);
            logger.debug("{}", fullStatMessage);
        } else {
            logger.debug("{}", fullStatMessage);
        }
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

    ErrorHandlingStrategyExecutor.Action determineErrorAction(@NotNull OperationResultStatus status,
            @NotNull Throwable exception, ItemProcessingRequest<?> request, OperationResult result) {
        return errorHandlingStrategyExecutor.determineAction(status, exception, request.getObjectOidToRecordRetryTrigger(), result);
    }

    /**
     * @return Default error action if no policy is specified or if no policy entry matches.
     */
    protected @NotNull abstract ErrorHandlingStrategyExecutor.Action getDefaultErrorAction();

    public @NotNull TaskReportingOptions getReportingOptions() {
        return reportingOptions;
    }

    public boolean isSimulate() {
        TaskPartitionDefinitionType partDefinition = taskExecution.partDefinition;
        return partDefinition != null && partDefinition.getStage() == ExecutionModeType.SIMULATE;
    }

    public @NotNull String getRootTaskOid() {
        return taskExecution.getRootTaskOid();
    }

    protected @NotNull Task getRootTask(OperationResult result) throws SchemaException {
        String rootTaskOid = getRootTaskOid();
        if (localCoordinatorTask.getOid().equals(rootTaskOid)) {
            return localCoordinatorTask;
        } else {
            try {
                return taskHandler.taskManager.getTaskPlain(rootTaskOid, result);
            } catch (ObjectNotFoundException e) {
                // This is quite unexpected so it can be rethrown as SystemException
                throw new SystemException("The root task was not found", e);
            }
        }
    }

    public String getPartUri() {
        return partUri;
    }

    public void setPartUri(String partUri) {
        this.partUri = partUri;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public int getExpectedParts() {
        return expectedParts;
    }

    public void setExpectedParts(int expectedParts) {
        this.expectedParts = expectedParts;
    }

    void markStructuredProgressComplete(OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException {
        localCoordinatorTask.markStructuredProgressAsComplete();
        localCoordinatorTask.flushPendingModifications(result);
    }

    /**
     * When did current part start?
     *
     * We need this for execution record creation that is needed for wall clock average and throughput computation.
     *
     * However, answering this question is a bit tricky because of bucketed runs. By recording part start when the bucket
     * starts is precise, but
     *
     * 1. does not take bucket management overhead into account,
     * 2. generates a lot of execution records - more than we can reasonably handle.
     *
     * So, for all except multi-part runs (that are never bucketed by definition) we use regular task start information.
     *
     * All of this will change when bucketing will be (eventually) rewritten.
     */
    public long getPartStartTimestamp() {
        if (taskExecution.isInternallyMultipart()) {
            return bucketStatistics.getStartTimeMillis();
        } else {
            return Objects.requireNonNull(localCoordinatorTask.getLastRunStartTimestamp(),
                    () -> "No last run start timestamp in " + localCoordinatorTask);
        }
    }
}
