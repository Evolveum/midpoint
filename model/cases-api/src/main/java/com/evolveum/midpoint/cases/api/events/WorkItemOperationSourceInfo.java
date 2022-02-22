/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.events;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;

/**
 * What caused the operation.
 * Primarily used to simplify passing parameters to {@link CaseEventCreationListener}.
 */
public class WorkItemOperationSourceInfo {

    private final ObjectReferenceType initiatorRef;
    private final WorkItemEventCauseInformationType cause;
    private final AbstractWorkItemActionType source;

    public WorkItemOperationSourceInfo(ObjectReferenceType initiatorRef,
            WorkItemEventCauseInformationType cause,
            AbstractWorkItemActionType source) {
        this.initiatorRef = initiatorRef;
        this.cause = cause;
        this.source = source;
    }

    public ObjectReferenceType getInitiatorRef() {
        return initiatorRef;
    }

    public WorkItemEventCauseInformationType getCause() {
        return cause;
    }

    public AbstractWorkItemActionType getSource() {
        return source;
    }
}
