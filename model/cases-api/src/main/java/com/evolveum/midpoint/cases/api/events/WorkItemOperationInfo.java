/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.api.events;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType;

/**
 * Primarily used to simplify passing parameters to CaseEventCreationListener.
 */
public class WorkItemOperationInfo {

    private final WorkItemOperationKindType operationKind;

    WorkItemOperationInfo(WorkItemOperationKindType operationKind) {
        this.operationKind = operationKind;
    }

    public WorkItemOperationKindType getOperationKind() {
        return operationKind;
    }
}
