/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
