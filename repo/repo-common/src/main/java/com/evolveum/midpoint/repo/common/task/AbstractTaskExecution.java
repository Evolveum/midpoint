/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;

/**
 * Holds data common for the whole task execution, i.e. data that are shared among all the parts.
 *
 * *TODO: thread safety ... hopefully, currentTaskPartExecution is the only field accessed by external threads*
 *
 * *TODO: Generalize this class a bit. Consider moving to the task module.*
 */
public class AbstractTaskExecution
        <TH extends AbstractTaskHandler<TH, TE>,
                TE extends AbstractTaskExecution<TH, TE>> {

    /** The task handler. Used mainly to access Spring beans. */
    @NotNull public final TH taskHandler;

    /** Worker task scheduled by Quartz. Can have worker threads. */
    public final RunningTask localCoordinatorTask;

    /** Bucket currently processed. Provided by task manager. */
    protected final WorkBucketType workBucket;

    /** Definition of task part currently processed. Provided by task manager. */
    public final TaskPartitionDefinitionType partDefinition;

    /** TODO */
    final TaskWorkBucketProcessingResult previousRunResult;

    /**
     * "Root" operation result that will be eventually returned in runResult.
     */
    @NotNull private final OperationResult taskOperationResult;

    /**
     * Current task run result. It will be returned from the task handler run method.
     *
     * TODO specify better - does each part execution supply its own result?
     */
    @NotNull private final TaskWorkBucketProcessingResult currentRunResult;

    private final AtomicReference<AbstractIterativeTaskPartExecution<?, ?, ?, ?, ?>> currentTaskPartExecution
            = new AtomicReference<>();

    public AbstractTaskExecution(@NotNull TH taskHandler,
            RunningTask localCoordinatorTask, WorkBucketType workBucket, TaskPartitionDefinitionType partDefinition,
            TaskWorkBucketProcessingResult previousRunResult) {
        this.taskHandler = taskHandler;
        this.localCoordinatorTask = localCoordinatorTask;
        this.workBucket = workBucket;
        this.partDefinition = partDefinition;
        this.previousRunResult = previousRunResult;
        this.taskOperationResult = createOperationResult();
        this.currentRunResult = createRunResult();
    }

    // TODO revisit this
    @NotNull
    private TaskWorkBucketProcessingResult createRunResult() {
        TaskWorkBucketProcessingResult runResult = new TaskWorkBucketProcessingResult();
        if (previousRunResult != null) {
            runResult.setProgress(previousRunResult.getProgress());
        } else {
            runResult.setProgress(0L);
        }
        runResult.setOperationResult(taskOperationResult);
        return runResult;
    }

    @NotNull
    private OperationResult createOperationResult() {
        OperationResult opResult = new OperationResult(taskHandler.taskOperationPrefix + ".run");
        opResult.setStatus(OperationResultStatus.IN_PROGRESS);
        return opResult;
    }

    /**
     * Main execution method. Iterates through all the part executions, running each of them
     * until finished or until the execution stops.
     *
     * Note that the final handling of exceptions is NOT done here, but within the task handler.
     */
    public TaskWorkBucketProcessingResult run() throws SchemaException, TaskException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException, PolicyViolationException, PreconditionViolationException {
        try {
            //noinspection unchecked
            taskHandler.registerExecution(localCoordinatorTask, (TE) this);

            initialize(taskOperationResult);

            List<? extends AbstractIterativeTaskPartExecution<?, ?, ?, ?, ?>> partExecutions = createPartExecutions();
            for (int i = 0, partExecutionsSize = partExecutions.size(); i < partExecutionsSize; i++) {
                AbstractIterativeTaskPartExecution<?, ?, ?, ?, ?> partExecution = partExecutions.get(i);
                currentTaskPartExecution.set(partExecution);

                OperationResult opResult = taskOperationResult.createSubresult(taskHandler.taskOperationPrefix + ".part" + (i+1)); // TODO
                try {
                    partExecution.run(opResult);
                } catch (Throwable t) {
                    opResult.recordFatalError(t);
                    throw t;
                } finally {
                    opResult.computeStatusIfUnknown();
                }

                // Note that we continue even in the presence of errors in previous part.
                // It is OK because this is how it was implemented in the only (in-task) multi-part execution: reconciliation.
                // But generally, this behaviour should be controlled using a policy.
                if (!localCoordinatorTask.canRun()) {
                    break;
                }
            }

            finish(taskOperationResult, null);
            return currentRunResult;
        } catch (Throwable t) {
            finish(currentRunResult.getOperationResult(), t);
            throw t;
        } finally {
            taskHandler.unregisterExecution(localCoordinatorTask);
        }
    }

    /**
     * Creates executions for individual task parts. Overridden for handlers that have more than one part
     * and therefore cannot rely on class annotations.
     */
    public List<? extends AbstractIterativeTaskPartExecution<?, ?, ?, ?, ?>> createPartExecutions() {
        return createPartExecutionsFromAnnotation();
    }

    private List<AbstractIterativeTaskPartExecution<?, ?, ?, ?, ?>> createPartExecutionsFromAnnotation() {
        try {
            PartExecutionClass annotation =
                    java.util.Objects.requireNonNull(taskHandler.getClass().getAnnotation(PartExecutionClass.class),
                            "The @PartExecutionClass annotation is missing.");
            return singletonList(AnnotationSupportUtil.instantiate(annotation.value(), this, taskHandler));
        } catch (Throwable t) {
            throw new SystemException("Cannot instantiate task part execution class learned from annotation of "
                    + taskHandler.getClass() + ": " + t.getMessage(), t);
        }
    }

    /**
     * Called right on the start of the task execution, even before parts are created.
     * Used for things like ResourceType object resolution, and so on.
     */
    protected void initialize(OperationResult opResult) throws TaskException, CommunicationException,
            SchemaException, ConfigurationException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException {
    }

    /**
     * Called right before the execution stops, even in the case of exceptions.
     */
    protected void finish(OperationResult opResult, Throwable t) throws TaskException, SchemaException {
    }

    protected <X> X getTaskPropertyRealValue(ItemName propertyName) {
        PrismProperty<X> property = localCoordinatorTask.getExtensionPropertyOrClone(propertyName);
        return property != null ? property.getRealValue() : null;
    }

    public PrismContext getPrismContext() {
        return taskHandler.getPrismContext();
    }

    public @NotNull TaskWorkBucketProcessingResult getCurrentRunResult() {
        return currentRunResult;
    }

    /**
     * TODO reconsider this method
     */
    public Long heartbeat() {
        AbstractIterativeTaskPartExecution<?, ?, ?, ?, ?> currentTaskPartExecution = this.currentTaskPartExecution.get();
        return currentTaskPartExecution != null ? currentTaskPartExecution.heartbeat() : null;
    }


    // ??? here

//
//    public void completeProcessing(Task task, OperationResult result) {
//        signalAllItemsSubmitted();
//        waitForCompletion(result); // in order to provide correct statistics results, we have to wait until all child tasks finish
//        updateOperationResult(result);
//    }
//    private void signalAllItemsSubmitted() {
//        allItemsSubmitted = true;
//    }
//    private void waitForCompletion(OperationResult opResult) {
//        getTaskManager().waitForTransientChildren(coordinatorTask, opResult);
//    }
//    private void updateOperationResult(OperationResult opResult) {
//        if (workerSpecificResults != null) { // not null in the parallel case
//            for (OperationResult workerSpecificResult : workerSpecificResults) {
//                workerSpecificResult.computeStatus();
//                workerSpecificResult.summarize();
//                opResult.addSubresult(workerSpecificResult);
//            }
//        }
//        opResult.computeStatus("Issues during processing");
//
//        if (getErrors() > 0) {
//            opResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
//        }
//    }

}
