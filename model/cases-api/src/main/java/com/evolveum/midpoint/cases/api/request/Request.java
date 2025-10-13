/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.api.request;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Abstract request that is going to be processed by the case engine.
 */
public abstract class Request implements Serializable {

    /**
     * Each request is related to a single case. This is its OID.
     */
    @NotNull protected final String caseOid;

    /**
     * What is the cause of the current request (e.g. to complete a work item, or to cancel the whole case)?
     */
    protected final WorkItemEventCauseInformationType causeInformation;

    public Request(@NotNull String caseOid,
            WorkItemEventCauseInformationType causeInformation) {
        this.caseOid = caseOid;
        this.causeInformation = causeInformation;
    }

    @NotNull
    public String getCaseOid() {
        return caseOid;
    }

    public WorkItemEventCauseInformationType getCauseInformation() {
        return causeInformation;
    }
}
