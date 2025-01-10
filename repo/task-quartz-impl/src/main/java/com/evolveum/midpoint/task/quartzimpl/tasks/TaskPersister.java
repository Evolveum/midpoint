/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.TaskListenerRegistry;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TaskPersister {

    private static final Trace LOGGER = TraceManager.getTrace(TaskPersister.class);

    private static final String CLASS_DOT = TaskPersister.class.getName() + ".";

    private static final String OP_ADD_TASK_TO_REPOSITORY_AND_QUARTZ = CLASS_DOT + "addTaskToRepositoryAndQuartz";
    private static final String OP_SWITCH_TO_BACKGROUND = Task.DOT_INTERFACE + "switchToBackground";

    @Autowired private TaskRetriever taskRetriever;
    @Autowired private TaskInstantiator taskInstantiator;
    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private TaskSynchronizer taskSynchronizer;
    @Autowired private Protector protector;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private TaskListenerRegistry taskListenerRegistry;

    public void switchToBackground(TaskQuartzImpl task, OperationResult parentResult) {

        parentResult.setInProgress("Task switched to background");
        OperationResult result = parentResult.createSubresult(OP_SWITCH_TO_BACKGROUND);

        // if the task result was unknown, we change it to 'in-progress'
        // (and roll back this change if storing into repo fails...)
        boolean wasUnknown = false;
        try {
            if (task.getResult().isUnknown()) {
                wasUnknown = true;
                task.getResult().setInProgress();
            }
            persist(task, result);
            result.recordSuccess();
        } catch (RuntimeException ex) {
            if (wasUnknown) {
                task.getResult().setUnknown();
            }
            result.recordFatalError("Unexpected problem: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    private void persist(TaskQuartzImpl task, OperationResult result) {
        if (task.isPersistent()) {
            return;
        }

        if (task instanceof RunningTask) {
            throw new UnsupportedOperationException("Running task cannot be made persistent");
        }

        if (task.getName() == null) {
            task.setName("Task " + task.getTaskIdentifier());
        }

        try {
            CryptoUtil.encryptValues(protector, task.getRawTaskObject());
            addTaskToRepositoryAndQuartz(task, null, result);
        } catch (ObjectAlreadyExistsException ex) {
            // This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
            throw new IllegalStateException("Got ObjectAlreadyExistsException while not expecting it (task:" + task + ")", ex);
        } catch (SchemaException ex) {
            // This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
            throw new IllegalStateException("Got SchemaException while not expecting it (task:" + task + ")", ex);
        } catch (EncryptionException e) {
            // TODO handle this better
            throw new SystemException("Couldn't encrypt plain text values in " + task + ": " + e.getMessage(), e);
        }
    }

    public String addTask(PrismObject<TaskType> taskPrism, RepoAddOptions options, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        if (taskPrism.asObjectable().getOwnerRef() == null) {
            try {
                MidPointPrincipal principal = SecurityUtil.getPrincipal();
                if (principal != null) {
                    ObjectReferenceType newOwnerRef = ObjectTypeUtil.createObjectRef(principal.getFocus());
                    taskPrism.asObjectable().setOwnerRef(newOwnerRef);
                }
            } catch (SecurityViolationException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine logged-in user. Task owner was not set.", e);
            }
        }
        // perhaps redundant, but it's more convenient to work with Task than with Task prism
        TaskQuartzImpl task = taskInstantiator.createTaskInstance(taskPrism, result);
        if (task.getTaskIdentifier() == null) {
            task.setTaskIdentifier(generateTaskIdentifier().toString());
        }
        return addTaskToRepositoryAndQuartz(task, options, result);
    }

    private String addTaskToRepositoryAndQuartz(TaskQuartzImpl task, RepoAddOptions options,
            OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {

        if (task instanceof RunningLightweightTask) {
            throw new IllegalStateException("A task with lightweight task handler cannot be made persistent; task = " + task);
            // otherwise, there would be complications on task restart: the task handler is not stored in the repository,
            // so it is just not possible to restart such a task
        }

        setSchedulingState(task);

        OperationResult result = parentResult.createSubresult(OP_ADD_TASK_TO_REPOSITORY_AND_QUARTZ);
        result.addArbitraryObjectAsParam("task", task);
        try {
            String oid = repositoryService.addObject(task.getUpdatedTaskObject(), options, result);
            task.setOid(oid);
            task.synchronizeWithQuartzWithTriggerRecreation(result);
            return oid;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void setSchedulingState(TaskQuartzImpl task) {
        TaskExecutionStateType executionState = task.getExecutionState();
        if (task.getSchedulingState() == null && executionState != null) {
            task.setSchedulingState(TaskMigrator.determineSchedulingState(executionState));
        }
    }

    public void modifyTask(String oid, Collection<? extends ItemDelta<?, ?>> modifications, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        repositoryService.modifyObject(TaskType.class, oid, modifications, result);
        TaskQuartzImpl task = taskRetriever.getTaskPlain(oid, result);
        task.setRecreateQuartzTrigger(true);
        taskSynchronizer.synchronizeTask(task, result);
        taskListenerRegistry.notifyTaskUpdated(task, result);
    }

    public LightweightIdentifier generateTaskIdentifier() {
        return lightweightIdentifierGenerator.generate();
    }
}
