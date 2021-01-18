/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;

import static java.util.Objects.requireNonNull;

/**
 * TODO remove
 */
@Component
@TaskExecutionClass(AbstractSearchIterativeTaskExecution.class)
@PartExecutionClass(ExecuteChangesTaskHandlerOld.Execution.class)
@DefaultHandledObjectType(UserType.class) // TODO consider switching to FocusType
public class ExecuteChangesTaskHandlerOld
        extends AbstractSearchIterativeModelTaskHandler
        <ExecuteChangesTaskHandlerOld, ExecuteChangesTaskHandlerOld.TaskExecution> {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/execute/handler-3.OLD";

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteChangesTaskHandlerOld.class);

    public ExecuteChangesTaskHandlerOld() {
        super("Execute", OperationConstants.EXECUTE);
        setLogFinishInfo(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @ResultHandlerClass(Execution.Handler.class)
    protected class Execution
            extends AbstractSearchIterativeModelTaskPartExecution
            <FocusType,
                    ExecuteChangesTaskHandlerOld,
                    ExecuteChangesTaskHandlerOld.TaskExecution,
                    Execution,
                    Execution.Handler> {

        public Execution(ExecuteChangesTaskHandlerOld.TaskExecution taskExecution) {
            super(taskExecution);
        }

        @Override
        protected void initialize(OperationResult opResult) throws SchemaException {
            // Try to fail fast if there's an obvious problem.
            if (createDeltaFromTask() == null) {
                throw new SchemaException("No delta in the task");
            }
        }

        private <T extends ObjectType> ObjectDelta<T> createDeltaFromTask() throws SchemaException {
            ObjectDeltaType objectDeltaBean = getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
            if (objectDeltaBean != null) {
                return DeltaConvertor.createObjectDelta(objectDeltaBean, prismContext);
            } else {
                return null;
            }
        }

        protected class Handler
                extends AbstractSearchIterativeResultHandler
                <FocusType,
                        ExecuteChangesTaskHandlerOld,
                        ExecuteChangesTaskHandlerOld.TaskExecution,
                        Execution, Handler> {

            public Handler(Execution taskExecution) {
                super(taskExecution);
            }

            @Override
            protected boolean handleObject(PrismObject<FocusType> object, RunningTask workerTask, OperationResult result)
                    throws CommonException, PreconditionViolationException {
                executeChange(object, workerTask, result);
                return true;
            }

            private <T extends ObjectType> void executeChange(PrismObject<T> focalObject, Task workerTask, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                    ObjectAlreadyExistsException, ConfigurationException, PolicyViolationException, SecurityViolationException {
                LOGGER.trace("Executing change on object {}", focalObject);

                ObjectDelta<T> delta = requireNonNull(createDeltaFromTask());
                delta.setOid(focalObject.getOid());
                if (focalObject.getCompileTimeClass() != null) {
                    delta.setObjectTypeClass(focalObject.getCompileTimeClass());
                }
                prismContext.adopt(delta);

                model.executeChanges(Collections.singletonList(delta), getExecuteOptionsFromTask(localCoordinatorTask), workerTask, result);
                LOGGER.trace("Execute changes {} for object {}: {}", delta, focalObject, result.getStatus());
            }
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.EXECUTE_CHANGES;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return null; // The channel URI should be provided by the task creator.
    }

    /** Just to make Java compiler happy. */
    protected static class TaskExecution
            extends AbstractSearchIterativeTaskExecution<ExecuteChangesTaskHandlerOld, ExecuteChangesTaskHandlerOld.TaskExecution> {

        public TaskExecution(ExecuteChangesTaskHandlerOld taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }

//    /** Just to make Java compiler happy. */
//    protected static class TaskExecution
//            extends AbstractSearchIterativeTaskExecution<XXX, XXX.TaskExecution> {
//
//        public TaskExecution(XXX taskHandler,
//                RunningTask localCoordinatorTask, WorkBucketType workBucket,
//                TaskPartitionDefinitionType partDefinition,
//                TaskWorkBucketProcessingResult previousRunResult) {
//            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
//        }
//    }
}
