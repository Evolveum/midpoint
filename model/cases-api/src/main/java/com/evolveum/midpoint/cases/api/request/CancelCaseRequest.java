/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.api.request;

import org.jetbrains.annotations.NotNull;

/**
 * Request to cancel the specified case.
 */
public class CancelCaseRequest extends Request {

    public CancelCaseRequest(@NotNull String caseOid) {
        super(caseOid, null);
    }

    @Override
    public String toString() {
        return "CancelCaseRequest{" +
                "caseOid='" + caseOid + '\'' +
                ", causeInformation=" + causeInformation +
                '}';
    }
}
