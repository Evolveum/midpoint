/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public interface WorkflowEvent extends Event {
    @NotNull CaseType getCase();

    String getProcessInstanceName();

    OperationStatus getOperationStatus();

    ChangeType getChangeType();

    boolean isApprovalCase();

    boolean isManualResourceCase();

    boolean isResultKnown();

    boolean isApproved();

    boolean isRejected();

    @NotNull ApprovalContextType getApprovalContext();

    @NotNull CaseType getWorkflowTask();
}
