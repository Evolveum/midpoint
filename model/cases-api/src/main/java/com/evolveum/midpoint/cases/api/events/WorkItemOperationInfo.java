/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
