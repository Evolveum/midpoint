/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

public record AccessCertificationWorkItemId(@NotNull AccessCertificationCaseId caseId, long workItemId) implements Serializable {

    public static AccessCertificationWorkItemId of(@NotNull String campaignOid, long caseId, long workItemId) {
        return new AccessCertificationWorkItemId(
                new AccessCertificationCaseId(campaignOid, caseId),
                workItemId);
    }

    public static AccessCertificationWorkItemId of(@NotNull AccessCertificationWorkItemType workItem) {
        return new AccessCertificationWorkItemId(
                AccessCertificationCaseId.of(
                        CertCampaignTypeUtil.getCaseChecked(workItem)),
                Objects.requireNonNull(workItem.getId(), "No work item ID"));
    }

    public static Set<AccessCertificationWorkItemId> of(@NotNull Collection<AccessCertificationWorkItemType> workItems) {
        return workItems.stream()
                .map(wi -> of(wi))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return caseId + ":" + workItemId;
    }

    public @NotNull String campaignOid() {
        return caseId.campaignOid();
    }

    public @NotNull ItemPath asItemPath() {
        return caseId.asItemPath().append(workItemId);
    }
}
