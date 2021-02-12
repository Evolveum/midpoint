/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.simple;

import com.evolveum.midpoint.model.impl.tasks.AbstractModelTaskHandler;
import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.AbstractTaskExecution;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskPartExecution;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>Enables writing simple search-iterative task handlers without having to deal with the complexity
 * of task executions, task part executions, and result handlers.</p>
 *
 * <p>This micro-framework requires only two classes to be provided by the client:</p>
 *
 * <ol>
 *     <li>An implementation of {@link ExecutionContext}: holds the data needed for the task execution</li>
 *     <li>An implementation of {@link Processing}: contains specification of the object type,
 *     query, search options, and processing logic</li>
 * </ol>
 *
 * TODO TODO TODO finish this description
 */
public abstract class SimpleIterativeTaskHandler<O extends ObjectType, EC extends ExecutionContext, P extends Processing<O, EC>>
        extends AbstractModelTaskHandler
        <SimpleIterativeTaskHandler<O, EC, P>, SimpleIterativeTaskHandler<O, EC, P>.TaskExecution> {

    protected SimpleIterativeTaskHandler(Trace logger, String taskName, String taskOperationPrefix) {
        super(logger, taskName, taskOperationPrefix);
    }

    @Override
    protected @NotNull SimpleIterativeTaskHandler<O, EC, P>.TaskExecution createTaskExecution(
            RunningTask localCoordinatorTask, WorkBucketType workBucket,
            TaskPartitionDefinitionType partition, TaskWorkBucketProcessingResult previousRunResult) {
        EC executionContext = createExecutionContext();
        return new TaskExecution(this, localCoordinatorTask, workBucket, partition, previousRunResult, executionContext);
    }

    protected abstract EC createExecutionContext();

    protected abstract P createProcessing(EC ctx);

    protected class TaskExecution
            extends AbstractTaskExecution
            <SimpleIterativeTaskHandler<O, EC, P>,
                                            TaskExecution> {

        private final EC executionContext;

        public TaskExecution(SimpleIterativeTaskHandler<O, EC, P> taskHandler, RunningTask localCoordinatorTask,
                WorkBucketType workBucket, TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult, EC executionContext) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
            this.executionContext = executionContext;
            executionContext.setTaskExecution(this);
        }

        @Override
        protected void initialize(OperationResult opResult) throws TaskException, CommunicationException, SchemaException,
                ConfigurationException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException {
            super.initialize(opResult);
            executionContext.initialize(opResult);
        }

        @Override
        public List<? extends AbstractSearchIterativeTaskPartExecution<?, ?, ?, ?, ?>> createPartExecutions() {
            return Collections.singletonList(new PartExecution(this));
        }
    }

    protected class PartExecution
            extends AbstractIterativeModelTaskPartExecution
            <O, SimpleIterativeTaskHandler<O, EC, P>,
                                            TaskExecution, PartExecution, ItemProcessor> {

        private final P processing;

        public PartExecution(TaskExecution taskExecution) {
            super(taskExecution);
            processing = createProcessing(taskExecution.executionContext);
        }

        @Override
        protected @NotNull SimpleIterativeTaskHandler.ItemProcessor createItemProcessor(OperationResult opResult)
                throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException {
            return new ItemProcessor(this);
        }

        @Override
        protected Class<O> determineObjectType() {
            Class<O> configuredType = getTypeFromTask();
            //noinspection unchecked
            return (Class<O>) processing.determineObjectType(configuredType); // FIXME resolve "<O>" vs "<? extends O>"
        }

        @Override
        protected ObjectQuery createQuery(OperationResult opResult)
                throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
                ExpressionEvaluationException, SecurityViolationException {
            ObjectQuery configuredQuery = createQueryFromTask();
            return processing.createQuery(configuredQuery);
        }

        @Override
        protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(OperationResult opResult) {
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions = createSearchOptionsFromTask();
            return processing.createSearchOptions(configuredOptions);
        }
    }

    protected class ItemProcessor
            extends AbstractSearchIterativeItemProcessor
            <O, SimpleIterativeTaskHandler<O, EC, P>, TaskExecution, PartExecution, ItemProcessor> {

        ItemProcessor(PartExecution partExecution) {
            super(partExecution);
        }

        @Override
        protected boolean processObject(PrismObject<O> object,
                ItemProcessingRequest<PrismObject<O>> request,
                RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            partExecution.processing.handleObject(object, workerTask, result);
            return true;
        }
    }
}
