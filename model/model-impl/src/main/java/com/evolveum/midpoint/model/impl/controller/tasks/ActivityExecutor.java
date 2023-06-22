/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller.tasks;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType.RUNNABLE;

/**
 * Handles task creation and activity execution requested by
 *
 * - {@link ModelInteractionService#submit(ActivityDefinitionType, ActivitySubmissionOptions, Task, OperationResult)}
 * - {@link ModelInteractionService#createExecutionTask(ActivityDefinitionType, ActivitySubmissionOptions, Task, OperationResult)}
 *
 * Currently limited to their submissions in the form of background tasks.
 */
public class ActivityExecutor {

    @NotNull private final ActivityDefinitionType activityDefinitionBean;
    @NotNull private final ActivitySubmissionOptions options;
    @NotNull private final Task task;
    @NotNull private final ModelBeans b = ModelBeans.get();

    public ActivityExecutor(
            @NotNull ActivityDefinitionType activityDefinitionBean,
            @NotNull ActivitySubmissionOptions options,
            @NotNull Task task) {
        this.activityDefinitionBean = activityDefinitionBean;
        this.options = options;
        this.task = task;
    }

    public String submit(OperationResult result) throws CommonException {
        return b.securityContextManager.runPrivilegedChecked(
                () -> {
                    var newTask = createExecutionTask()
                            .ownerRef(AuthUtil.getPrincipalRefRequired())
                            .executionState(RUNNABLE);
                    var executedDeltas = b.modelService.executeChanges(
                            List.of(newTask.asPrismObject().createAddDelta()),
                            null, task, result);
                    var taskOid = ObjectDeltaOperation.findAddDeltaOidRequired(executedDeltas, TaskType.class);

                    result.setBackgroundTaskOid(taskOid);

                    // Before setting "in progress" status, we may consider checking the task execution status here
                    // (e.g. if it's not suspended). But let's ignore it for the moment.
                    result.setInProgress();

                    return taskOid;
                }
        );
    }

    public TaskType createExecutionTask()
            throws SchemaException, ConfigurationException {

        TaskType newTask;
        TaskType template = options.taskTemplate();
        if (template != null) {
            newTask = template.clone();
        } else {
            newTask = new TaskType();
        }

        newTask.setActivity(activityDefinitionBean.clone());

        getArchetypeRefsToAdd().forEach(
                ref -> newTask.assignment(ObjectTypeUtil.createAssignmentTo(ref)));

        return newTask;
    }

    private List<ObjectReferenceType> getArchetypeRefsToAdd() throws SchemaException, ConfigurationException {

        // Explicitly via the option
        String[] explicitArchetypes = options.archetypes();
        if (explicitArchetypes.length > 0) {
            return Arrays.stream(explicitArchetypes)
                    .map(oid -> ObjectTypeUtil.createObjectRef(oid, ObjectTypes.ARCHETYPE))
                    .toList();
        }

        // Via task template (in options)
        var taskTemplate = options.taskTemplate();
        if (taskTemplate != null && ObjectTypeUtil.hasAssignedArchetype(taskTemplate)) {
            return List.of();
        }

        // From the work definition
        var implicitRef = ObjectTypeUtil.createObjectRefNullSafe(
                b.activityHandlerRegistry.getDefaultArchetypeOid(activityDefinitionBean),
                ObjectTypes.ARCHETYPE);
        return implicitRef != null ? List.of(implicitRef) : List.of();
    }
}
