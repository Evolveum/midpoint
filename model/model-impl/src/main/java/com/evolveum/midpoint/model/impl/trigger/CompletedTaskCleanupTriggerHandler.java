/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class CompletedTaskCleanupTriggerHandler implements SingleTriggerHandler {

    public static final String HANDLER_URI = SchemaConstants.COMPLETED_TASK_CLEANUP_TRIGGER_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(CompletedTaskCleanupTriggerHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private RepositoryService repositoryService;
    @Autowired private TaskManager taskManager;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

    @Override
    public <O extends ObjectType> void handle(@NotNull PrismObject<O> object, @NotNull TriggerType trigger,
            @NotNull RunningTask task, @NotNull OperationResult result) {
        try {
            // reload the task to minimize potential for race conflicts
            // todo use repo preconditions to implement this
            if (!(object.asObjectable() instanceof TaskType)) {
                return;
            }
            TaskType completedTask = repositoryService
                    .getObject(TaskType.class, object.getOid(), readOnly(), result)
                    .asObjectable();
            LOGGER.trace("Checking completed task to be deleted {}", completedTask);
            if (completedTask.getExecutionState() != TaskExecutionStateType.CLOSED) {
                LOGGER.debug("Task {} is not closed, not deleting it.", completedTask);
                return;
            }
            XMLGregorianCalendar completion = completedTask.getCompletionTimestamp();
            if (completion == null) {
                LOGGER.debug("Task {} has no completion timestamp, not deleting it.", completedTask);
                return;
            }
            if (completedTask.getCleanupAfterCompletion() == null) {
                LOGGER.debug("Task {} has no 'cleanup after completion' set, not deleting it.", completedTask);
                return;
            }
            completion.add(completedTask.getCleanupAfterCompletion());
            if (!XmlTypeConverter.isBeforeNow(completion)) {
                LOGGER.debug("Task {} should be deleted no earlier than {}, not deleting it.", completedTask, completion);
                // We assume there is another trigger set to the correct time. This might not be the case if the administrator
                // set 'cleanupAfterCompletion' after the task was completed. Let's jut ignore this situation.
                return;
            }
            LOGGER.debug("Deleting completed task {}", completedTask);
            taskManager.deleteTaskTree(object.getOid(), result);
        } catch (CommonException | RuntimeException | Error e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete completed task {}", e, object);
            // do not retry this trigger execution
        }
    }
}
