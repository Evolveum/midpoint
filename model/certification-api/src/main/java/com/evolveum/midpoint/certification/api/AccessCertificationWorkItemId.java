/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.api;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

public record AccessCertificationWorkItemId(@NotNull AccessCertificationCaseId caseId, long workItemId) implements Serializable {

    public static AccessCertificationWorkItemId of(@NotNull String campaignOid, long caseId, long workItemId) {
        return new AccessCertificationWorkItemId(
                new AccessCertificationCaseId(campaignOid, caseId),
                workItemId);
    }

    @Override
    public String toString() {
        return caseId + ":" + workItemId;
    }
}
