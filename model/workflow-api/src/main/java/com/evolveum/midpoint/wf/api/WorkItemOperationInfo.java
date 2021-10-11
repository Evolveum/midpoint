/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType;

/**
 * Primarily used to simplify passing parameters to WorkflowListener.
 *
 * @author mederly
 */
public class WorkItemOperationInfo {

    private final WorkItemOperationKindType operationKind;

    public WorkItemOperationInfo(WorkItemOperationKindType operationKind) {
        this.operationKind = operationKind;
    }

    public WorkItemOperationKindType getOperationKind() {
        return operationKind;
    }
}
