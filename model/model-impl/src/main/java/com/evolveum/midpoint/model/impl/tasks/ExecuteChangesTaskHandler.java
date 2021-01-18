/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Collections;
import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext;
import com.evolveum.midpoint.model.impl.tasks.simple.Processing;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleIterativeTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Executes specified deltas on specified set of objects.
 */
@Component
public class ExecuteChangesTaskHandler
        extends SimpleIterativeTaskHandler
        <ObjectType,
                ExecuteChangesTaskHandler.MyExecutionContext,
                ExecuteChangesTaskHandler.MyProcessing> {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/execute/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteChangesTaskHandler.class);

    public ExecuteChangesTaskHandler() {
        super("Execute", OperationConstants.EXECUTE);
        setLogFinishInfo(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected MyExecutionContext createExecutionContext() {
        return new MyExecutionContext();
    }

    @Override
    protected MyProcessing createProcessing(MyExecutionContext ctx) {
        return new MyProcessing(ctx);
    }

    public class MyExecutionContext extends ExecutionContext {

        private ModelExecuteOptions executeOptions;

        @Override
        protected void initialize(OperationResult opResult) throws SchemaException {
            RunningTask localCoordinationTask = getLocalCoordinationTask();

            executeOptions = ModelImplUtils.getModelExecuteOptions(localCoordinationTask);

            // Try to fail fast if there's an obvious problem.
            if (createDeltaFromTask(localCoordinationTask) == null) {
                throw new SchemaException("No delta in the task");
            }
        }
    }

    public class MyProcessing extends Processing<ObjectType, MyExecutionContext> {

        private MyProcessing(MyExecutionContext ctx) {
            super(ctx);
        }

        @Override
        protected Class<? extends ObjectType> determineObjectType(Class<? extends ObjectType> configuredType) {
            return defaultIfNull(configuredType, UserType.class); // TODO consider changing this
        }

        @Override
        protected void handleObject(PrismObject<ObjectType> object, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            LOGGER.trace("Executing change on object {}", object);

            RunningTask localCoordinationTask = ctx.getLocalCoordinationTask();

            ObjectDelta<ObjectType> delta = requireNonNull(createDeltaFromTask(localCoordinationTask));
            delta.setOid(object.getOid());
            if (object.getCompileTimeClass() != null) {
                delta.setObjectTypeClass(object.getCompileTimeClass());
            }
            prismContext.adopt(delta);

            model.executeChanges(Collections.singletonList(delta), ctx.executeOptions, workerTask, result);
            LOGGER.trace("Execute changes {} for object {}: {}", delta, object, result.getStatus());
        }
    }

    private <T extends ObjectType> ObjectDelta<T> createDeltaFromTask(Task task) throws SchemaException {
        ObjectDeltaType objectDeltaBean = task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
        if (objectDeltaBean != null) {
            return DeltaConvertor.createObjectDelta(objectDeltaBean, prismContext);
        } else {
            return null;
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
}
