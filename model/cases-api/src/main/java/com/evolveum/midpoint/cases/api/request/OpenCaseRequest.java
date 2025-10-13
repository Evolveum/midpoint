/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.api.request;

import org.jetbrains.annotations.NotNull;

/**
 * Request to open a (pre-created) case.
 */
public class OpenCaseRequest extends Request {

    public OpenCaseRequest(@NotNull String caseOid) {
        super(caseOid, null);
    }

    @Override
    public String toString() {
        return "OpenCaseRequest{" +
                "caseOid='" + caseOid + '\'' +
                ", causeInformation=" + causeInformation +
                '}';
    }
}
