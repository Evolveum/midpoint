/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

public record AccessCertificationCaseId(@NotNull String campaignOid, long caseId) implements Serializable {

    public static AccessCertificationCaseId of(@NotNull AccessCertificationCaseType aCase) {
        return new AccessCertificationCaseId(
                CertCampaignTypeUtil.getCampaignChecked(aCase).getOid(),
                Objects.requireNonNull(aCase.getId(), "No case ID"));
    }

    public static Set<AccessCertificationCaseId> of(@NotNull Collection<AccessCertificationCaseType> cases) {
        return cases.stream()
                .map(c -> of(c))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return campaignOid + ":" + caseId;
    }

    public @NotNull ItemPath asItemPath() {
        return ItemPath.create(AccessCertificationCampaignType.F_CASE, caseId);
    }

    public ObjectQuery queryFor() {
        return PrismContext.get().queryFor(AccessCertificationCaseType.class)
                .ownerId(campaignOid)
                .and().id(caseId)
                .build();
    }
}
