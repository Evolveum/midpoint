/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public interface TaskEvent extends Event {

    @NotNull Task getTask();

    @Nullable TaskRunResult getTaskRunResult();

    @NotNull EventOperationType getOperationType();

    boolean isTemporaryError();

    boolean isPermanentError();

    boolean isHaltingError();

    boolean isFinished();

    boolean isInterrupted();

    OperationResultStatus getOperationResultStatus();

    String getMessage();

    long getProgress();
}
