/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import java.util.Collections;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.task.api.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ExecuteChangesTaskHandler extends AbstractSearchIterativeModelTaskHandler<FocusType, AbstractSearchIterativeResultHandler<FocusType>> {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/execute/handler-3";

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService model;

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteChangesTaskHandler.class);

    public ExecuteChangesTaskHandler() {
        super("Execute", OperationConstants.EXECUTE);
        setLogFinishInfo(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    protected Class<? extends ObjectType> getType(Task task) {
        return getTypeFromTask(task, UserType.class);
    }

    @NotNull
    @Override
    protected AbstractSearchIterativeResultHandler<FocusType> createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, final RunningTask coordinatorTask,
            OperationResult opResult) {

        AbstractSearchIterativeResultHandler<FocusType> handler = new AbstractSearchIterativeResultHandler<FocusType>(
                coordinatorTask, ExecuteChangesTaskHandler.class.getName(), "execute", "execute task", taskManager) {
            @Override
            protected boolean handleObject(PrismObject<FocusType> object, RunningTask workerTask, OperationResult result) throws CommonException {
                executeChange(object, coordinatorTask, workerTask, result);
                return true;
            }
        };
        handler.setStopOnError(false);
        return handler;
    }

    private <T extends ObjectType> void executeChange(PrismObject<T> focalObject, Task coordinatorTask, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, PolicyViolationException, SecurityViolationException {
        LOGGER.trace("Executing change on object {}", focalObject);

        ObjectDelta<T> delta = createDeltaFromTask(coordinatorTask);
        if (delta == null) {
            throw new IllegalStateException("No delta in the task");
        }

        delta.setOid(focalObject.getOid());
        if (focalObject.getCompileTimeClass() != null) {
            delta.setObjectTypeClass(focalObject.getCompileTimeClass());
        }
        prismContext.adopt(delta);

        model.executeChanges(Collections.singletonList(delta), getExecuteOptionsFromTask(task), task, result);
        LOGGER.trace("Execute changes {} for object {}: {}", delta, focalObject, result.getStatus());
    }

    private <T extends ObjectType> ObjectDelta<T> createDeltaFromTask(Task task) throws SchemaException {
        PrismProperty<ObjectDeltaType> objectDeltaType = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
        if (objectDeltaType != null && objectDeltaType.getRealValue() != null) {
            return DeltaConvertor.createObjectDelta(objectDeltaType.getRealValue(), prismContext);
        } else {
            return null;
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.EXECUTE_CHANGES;
    }
}
