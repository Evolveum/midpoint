/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskWaitingReason;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
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

	void setExecutionStatusImmediate(TaskExecutionStatus value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;

	void setExecutionStatusImmediate(TaskExecutionStatus value, TaskExecutionStatusType previousValue,
			OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, PreconditionViolationException;

	void setWaitingReasonImmediate(TaskWaitingReason value, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;

	List<PrismObject<TaskType>> listPersistentSubtasksRaw(OperationResult parentResult) throws SchemaException;

	@NotNull
	List<Task> listSubtasksInternal(boolean persistentOnly, OperationResult result) throws SchemaException;

	void applyDeltasImmediate(Collection<ItemDelta<?, ?>> itemDeltas, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException;

	// no F_RESULT modifications!
	void applyModificationsTransient(Collection<ItemDelta<?,?>> modifications) throws SchemaException;

	void addSubtask(TaskType subtaskBean);
}
