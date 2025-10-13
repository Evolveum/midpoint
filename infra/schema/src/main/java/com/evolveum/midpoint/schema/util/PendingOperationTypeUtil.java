/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;

import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTION_PENDING;

public class PendingOperationTypeUtil {

    public static boolean isAdd(@NotNull PendingOperationType operation) {
        ObjectDeltaType delta = operation.getDelta();
        return delta != null && delta.getChangeType() == ChangeTypeType.ADD;
    }

    public static boolean isPendingOrExecuting(PendingOperationType operation) {
        PendingOperationExecutionStatusType status = operation.getExecutionStatus();
        return status == EXECUTION_PENDING || status == EXECUTING;
    }
}
