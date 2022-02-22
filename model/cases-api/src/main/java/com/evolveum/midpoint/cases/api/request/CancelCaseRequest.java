/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
