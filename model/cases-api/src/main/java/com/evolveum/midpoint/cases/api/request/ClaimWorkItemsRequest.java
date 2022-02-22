/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.request;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Request to claim a work item (or multiple work items in given case),
 * i.e. to assign it to the current user.
 *
 * @see ReleaseWorkItemsRequest
 */
public class ClaimWorkItemsRequest extends Request {

    public static class SingleClaim {

        private final long workItemId;

        public SingleClaim(long workItemId) {
            this.workItemId = workItemId;
        }

        public long getWorkItemId() {
            return workItemId;
        }

        @Override
        public String toString() {
            return "SingleClaim{" +
                    "workItemId=" + workItemId +
                    '}';
        }
    }

    @NotNull private final Collection<SingleClaim> claims = new ArrayList<>();

    public ClaimWorkItemsRequest(@NotNull String caseOid) {
        super(caseOid, null);
    }

    @NotNull
    public Collection<SingleClaim> getClaims() {
        return claims;
    }

    @Override
    public String toString() {
        return "ClaimWorkItemsRequest{" +
                "claims=" + claims +
                ", caseOid='" + caseOid + '\'' +
                ", causeInformation=" + causeInformation +
                '}';
    }
}
