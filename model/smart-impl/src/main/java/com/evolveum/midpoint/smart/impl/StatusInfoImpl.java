/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.io.Serial;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.task.ActivityBasedTaskInformation;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.LocalizableMessageListBuilder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class StatusInfoImpl<T> implements StatusInfo<T> {

    private static final Trace LOGGER = TraceManager.getTrace(StatusInfoImpl.class);

    @Serial private static final long serialVersionUID = 0L;

    /** Information about the task executing the operation. */
    private final TaskInformation taskInformation;

    /** What does the operation deals with? TODO generalize this beyond the resource object set. */
    private final @Nullable BasicResourceObjectSetType request;

    /** Item name of the result in the task work state. */
    private final ItemName resultItemName;

    /** Class of the result item. */
    private final Class<T> resultClass;

    /**
     * @param task Task that is executing the operation. We assume that an operation is being executed in a single task tree,
     * and there are no unrelated activities in the tree.
     * @param resultItemName Item name of the result in the task work state.
     * @param resultClass Class of the result item.
     */
    StatusInfoImpl(TaskType task, ItemName resultItemName, Class<T> resultClass) {
        LOGGER.trace("Task:\n{}", task.debugDump(1));
        argCheck(task.getParent() == null, "Task must be a root task, not a subtask.");
        this.taskInformation = ActivityBasedTaskInformation.createForTask(task, null);
        this.request = task.getAffectedObjects().getActivity().get(0).getResourceObjects(); // FIXME implement more robustly
        this.resultItemName = resultItemName;
        this.resultClass = resultClass;
    }

    @Override
    public String getToken() {
        return taskInformation.getTask().getOid();
    }

    @Override
    public OperationResultStatusType getStatus() {
        if (!wasStarted()) {
            return OperationResultStatusType.UNKNOWN;
        } else if (isExecuting()) {
            return OperationResultStatusType.IN_PROGRESS;
        } else if (isComplete()) {
            return taskInformation.getResultStatus(); // most probably SUCCESS, but could be WARNING, PARTIAL_ERROR, etc.
        } else {
            // task was suspended or failed, but not completed
            var status = taskInformation.getResultStatus();
            if (status == OperationResultStatusType.IN_PROGRESS) {
                // "In progress" is misleading for tasks that are not executing anymore.
                // "Not applicable" is not 100% correct, but it is better than "in progress".
                return OperationResultStatusType.NOT_APPLICABLE;
            } else {
                return status; // most probably some kind of error status
            }
        }
    }

    /** The progress information can be missing if the task did not start yet. */
    @Override
    public @Nullable ActivityProgressInformation getProgressInformation() {
        return taskInformation instanceof ActivityBasedTaskInformation activityBasedTaskInformation ?
                activityBasedTaskInformation.getProgressInformation() : null;
    }

    @Override
    public boolean wasStarted() {
        return getRealizationStartTimestamp() != null;
    }

    @Override
    public boolean isComplete() {
        return taskInformation.isComplete();
    }

    //TODO check problem with suspended tasks
    @Override
    public boolean isExecuting() {
        return taskInformation.isExecuting();
    }

    //Temporary helper
    public boolean isSuspended() {
    	return taskInformation.getTask().getExecutionState() == TaskExecutionStateType.SUSPENDED;
    }

    @Override
    public @Nullable LocalizableMessage getMessage() {
        // We assume that if there are user-friendly messages, they convey all the necessary information,
        // so that the other (not localizable) messages are not needed. The problem is that errors that have a user-friendly
        // form get reported in both forms, and it's hard to decide which not localizable messages correspond to which
        // user-friendly messages. So we just return the user-friendly messages.
        var userFriendlyMessages = taskInformation.getTaskHealthUserFriendlyMessages();
        if (!userFriendlyMessages.isEmpty()) {
            var deduplicated = userFriendlyMessages.stream().distinct().toList();
            return new LocalizableMessageListBuilder()
                    .messages(deduplicated)
                    .separator(LocalizableMessageList.SEMICOLON)
                    .buildOptimized();
        }
        var otherMessages = taskInformation.getTaskHealthMessages();
        if (!otherMessages.isEmpty()) {
            var deduplicated = otherMessages.stream().distinct().toList();
            return new LocalizableMessageBuilder()
                    .fallbackMessage(String.join("; ", deduplicated))
                    .build();
        }
        return null;
    }

    @Override
    public @Nullable String getLocalizedMessage() {
        return SmartIntegrationBeans.get().localizationService.translate(getMessage());
    }

    @Override
    public @Nullable BasicResourceObjectSetType getRequest() {
        return request;
    }

    @Override
    public @Nullable XMLGregorianCalendar getRealizationStartTimestamp() {
        return taskInformation instanceof ActivityBasedTaskInformation activityBasedTaskInformation ?
                activityBasedTaskInformation.getRealizationStartTimestamp() : null;
    }

    @Override
    public @Nullable XMLGregorianCalendar getRealizationEndTimestamp() {
        return taskInformation instanceof ActivityBasedTaskInformation activityBasedTaskInformation ?
                activityBasedTaskInformation.getRealizationEndTimestamp() : null;
    }

    @Override
    public @Nullable XMLGregorianCalendar getRunEndTimestamp() {
        return taskInformation instanceof ActivityBasedTaskInformation activityBasedTaskInformation ?
                activityBasedTaskInformation.getRunEndTimestamp() : null;
    }

    /** Final result of the operation, if available. */
    @Override
    public @Nullable T getResult() {
        var resultItem = taskInformation.getTask().asPrismObject().findItem(
                ItemPath.create(
                        TaskType.F_ACTIVITY_STATE,
                        TaskActivityStateType.F_ACTIVITY,
                        ActivityStateType.F_WORK_STATE,
                        resultItemName));
        if (resultItem == null) {
            return null;
        }
        removePcvIds(resultItem);
        return resultItem.getRealValue(resultClass);
    }

    /**
     * The PCV IDs are irrelevant, and can be even harmful, if the client wants to use the result value e.g. by
     * putting it into a new context.
     */
    private static void removePcvIds(Item<?, ?> item) {
        if (item instanceof PrismContainer<?> container) {
            for (PrismContainerValue<?> pcv : container.getValues()) {
                pcv.setId(null);
                pcv.getItems().forEach(child -> removePcvIds(child));
            }
        }
    }

    @Override
    public String toString() {
        return "StatusInfo[" + taskInformation + "]";
    }

    @Override
    public String debugDump(int indent) {
        return taskInformation.debugDump(indent); // later we can provide more information here, but this is enough for now
    }
}
