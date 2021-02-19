/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWaitingReasonType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface InternalTaskInterface extends Task {
    boolean isRecreateQuartzTrigger();

    void setRecreateQuartzTrigger(boolean recreateQuartzTrigger);

    void checkDependentTasksOnClose(OperationResult result) throws SchemaException, ObjectNotFoundException;

    void checkDependencies(OperationResult result) throws SchemaException, ObjectNotFoundException;

    void setOid(String oid);

    void setExecutionStatusImmediate(TaskExecutionStateType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    void setExecutionStatusImmediate(TaskExecutionStateType value, TaskExecutionStateType previousValue,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException;

    void setWaitingReasonImmediate(TaskWaitingReasonType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    List<PrismObject<TaskType>> listPersistentSubtasksRaw(OperationResult parentResult) throws SchemaException;

    @NotNull
    List<TaskQuartzImpl> listSubtasksInternal(boolean persistentOnly, OperationResult result) throws SchemaException;

    void applyDeltasImmediate(Collection<ItemDelta<?, ?>> itemDeltas, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException;

    // no F_RESULT modifications!
    void applyModificationsTransient(Collection<ItemDelta<?,?>> modifications) throws SchemaException;

    void addSubtask(TaskType subtaskBean);
}
